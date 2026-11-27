package com.htanh.appvk.data.remote.sheets

class CatalogValidationException(message: String) : Exception(message)

data class SheetTable(
    val headers: List<String>,
    val rows: List<List<String>>
) {
    private val headerIndexes: Map<String, Int> = headers
        .mapIndexed { index, header -> header.trim().lowercase() to index }
        .toMap()

    fun requireHeaders(tabName: String, required: Set<String>) {
        val duplicates = headers.map { it.trim().lowercase() }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw CatalogValidationException("$tabName contains duplicate headers: ${duplicates.joinToString()}")
        }
        val missing = required.filterNot { headerIndexes.containsKey(it.lowercase()) }
        if (missing.isNotEmpty()) {
            throw CatalogValidationException("$tabName is missing required headers: ${missing.joinToString()}")
        }
    }

    fun value(row: List<String>, header: String): String {
        val index = headerIndexes[header.lowercase()] ?: return ""
        return row.getOrNull(index)?.trim().orEmpty()
    }

    companion object {
        fun fromValues(tabName: String, values: List<List<String>>): SheetTable {
            val headerRow = values.firstOrNull()
                ?: throw CatalogValidationException("$tabName is empty or has no header row")
            val headers = headerRow.map(String::trim)
            if (headers.isEmpty() || headers.all(String::isBlank)) {
                throw CatalogValidationException("$tabName has an empty header row")
            }
            return SheetTable(headers, values.drop(1))
        }
    }
}
