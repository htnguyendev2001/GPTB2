package com.htanh.appvk.data.remote.sheets

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.Locale

internal data class AndroidApiKeyIdentity(
    val packageName: String,
    val certificateSha1: String
) {
    companion object {
        fun from(context: Context): AndroidApiKeyIdentity {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            } ?: emptyArray()

            val signingCertificate = signatures.firstOrNull()
                ?: throw IllegalStateException("The app signing certificate is unavailable.")

            return AndroidApiKeyIdentity(
                packageName = packageName,
                certificateSha1 = sha1Hex(signingCertificate.toByteArray())
            )
        }

        internal fun sha1Hex(certificateBytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-1").digest(certificateBytes)
            return digest.joinToString(separator = "") { byte ->
                String.format(Locale.US, "%02X", byte.toInt() and 0xFF)
            }
        }
    }
}
