package com.htanh.appvk.data.remote.sheets

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class LearningContentSyncException(
    val category: Category,
    message: String
) : IOException(message) {
    enum class Category { CONFIGURATION, NETWORK, ACCESS, NOT_FOUND, API, INVALID_CONTENT }
}

class GoogleSheetsRemoteDataSource(
    private val spreadsheetId: String,
    private val apiKey: String
) : LearningContentRemoteDataSource {
    override suspend fun fetchCatalog(): com.htanh.appvk.domain.learning.LearningCatalog = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        if (spreadsheetId.isBlank() || apiKey.isBlank()) {
            throw LearningContentSyncException(
                LearningContentSyncException.Category.CONFIGURATION,
                "Configure htavk.sheets.spreadsheetId and htavk.sheets.apiKey in local.properties."
            )
        }

        val ranges = listOf(
            "${LearningCatalogParser.SUBJECTS}!A:Z",
            "${LearningCatalogParser.CHAPTERS}!A:Z",
            "${LearningCatalogParser.TOPICS}!A:Z",
            "${LearningCatalogParser.QUESTIONS}!A:Z",
            "${LearningCatalogParser.QUESTION_OPTIONS}!A:Z"
        )
        val builder = Uri.parse("https://sheets.googleapis.com/v4/spreadsheets")
            .buildUpon()
            .appendPath(spreadsheetId)
            .appendEncodedPath("values:batchGet")
            .appendQueryParameter("majorDimension", "ROWS")
            .appendQueryParameter("valueRenderOption", "UNFORMATTED_VALUE")
            .appendQueryParameter("key", apiKey)
        ranges.forEach { builder.appendQueryParameter("ranges", it) }

        val connection = try {
            URL(builder.build().toString()).openConnection() as HttpURLConnection
        } catch (error: Exception) {
            throw LearningContentSyncException(LearningContentSyncException.Category.NETWORK, "Unable to create Google Sheets request.")
        }

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_RESPONSE_BYTES) {
                throw LearningContentSyncException(
                    LearningContentSyncException.Category.INVALID_CONTENT,
                    "Google Sheets response exceeds the 10 MiB limit."
                )
            }
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.use { it.readUtf8Bounded(MAX_RESPONSE_BYTES) }.orEmpty()
            currentCoroutineContext().ensureActive()

            if (status !in 200..299) {
                val category = when {
                    status == 401 || status == 403 -> LearningContentSyncException.Category.ACCESS
                    status == 404 -> LearningContentSyncException.Category.NOT_FOUND
                    status == 429 || status >= 500 -> LearningContentSyncException.Category.API
                    else -> LearningContentSyncException.Category.API
                }
                throw LearningContentSyncException(category, "Google Sheets API returned HTTP $status.")
            }

            val response = JSONObject(responseBody)
            val valueRanges = response.optJSONArray("valueRanges")
                ?: throw LearningContentSyncException(
                    LearningContentSyncException.Category.INVALID_CONTENT,
                    "Google Sheets response does not contain valueRanges."
                )
            if (valueRanges.length() != ranges.size) {
                throw LearningContentSyncException(
                    LearningContentSyncException.Category.INVALID_CONTENT,
                    "Google Sheets returned an unexpected number of tabs."
                )
            }

            val tables = ranges.mapIndexed { index, range ->
                val tabName = range.substringBefore('!')
                val values = valueRanges.optJSONObject(index)?.optJSONArray("values") ?: JSONArray()
                val rows = values.toStringRows()
                tabName to SheetTable.fromValues(tabName, rows)
            }.toMap()

            try {
                LearningCatalogParser.parse(tables)
            } catch (error: CatalogValidationException) {
                throw LearningContentSyncException(
                    LearningContentSyncException.Category.INVALID_CONTENT,
                    error.message ?: "Learning content validation failed."
                )
            }
        } catch (error: LearningContentSyncException) {
            throw error
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw LearningContentSyncException(LearningContentSyncException.Category.NETWORK, "Unable to reach Google Sheets.")
        } catch (error: Exception) {
            throw LearningContentSyncException(LearningContentSyncException.Category.INVALID_CONTENT, "Unable to parse Google Sheets response.")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val MAX_RESPONSE_BYTES = 10L * 1024L * 1024L
    }

    private fun InputStream.readUtf8Bounded(maxBytes: Long): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                throw LearningContentSyncException(
                    LearningContentSyncException.Category.INVALID_CONTENT,
                    "Google Sheets response exceeds the 10 MiB limit."
                )
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun JSONArray.toStringRows(): List<List<String>> = buildList {
        for (rowIndex in 0 until length()) {
            val row = optJSONArray(rowIndex) ?: JSONArray()
            add(buildList {
                for (columnIndex in 0 until row.length()) {
                    val value = row.opt(columnIndex)
                    add(if (value == null || value == JSONObject.NULL) "" else value.toString())
                }
            })
        }
    }
}
