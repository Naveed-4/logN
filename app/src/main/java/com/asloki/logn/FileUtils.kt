package com.asloki.logn

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
object FileUtils {
    private const val FILE_NAME = "notification_log.txt"

    fun logNotification(context: Context, packageName: String, title: String?, text: String?, action: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "$timestamp | $packageName | $title | $text | $action\n"

        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val uriString = sharedPreferences.getString("LOG_FILE_URI", null)

        if (uriString != null) {
            try {
                val uri = Uri.parse(uriString)
                val docFile = DocumentFile.fromTreeUri(context, uri)
                val file = docFile?.findFile(FILE_NAME) ?: docFile?.createFile("text/plain", FILE_NAME)

                file?.let {
                    context.contentResolver.openOutputStream(it.uri, "wa")?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.append(logEntry)
                            writer.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to internal storage if there's an error
                writeToInternalStorage(context, logEntry)
            }
        } else {
            writeToInternalStorage(context, logEntry)
        }
    }

    private fun writeToInternalStorage(context: Context, logEntry: String) {
        context.openFileOutput(FILE_NAME, Context.MODE_APPEND).use { fos ->
            fos.write(logEntry.toByteArray())
            fos.flush()
        }
    }
}