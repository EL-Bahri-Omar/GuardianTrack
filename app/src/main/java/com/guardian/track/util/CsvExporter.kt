package com.guardian.track.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.guardian.track.domain.model.Incident
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor() {

    companion object {
        private const val TAG = "CsvExporter"
    }

    fun exportIncidents(context: Context, incidents: List<Incident>): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "GuardianTrack_Export_${dateFormat.format(Date())}.csv"

            val outputStream = createFileInDocuments(context, fileName)
            if (outputStream == null) {
                Log.e(TAG, "Failed to create output file")
                return false
            }

            outputStream.use { stream ->
                // Write CSV header
                val header = "Date,Time,Type,Latitude,Longitude,Synced\n"
                stream.write(header.toByteArray())

                // Write each incident
                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                for (incident in incidents) {
                    val date = dateFmt.format(Date(incident.timestamp))
                    val time = timeFmt.format(Date(incident.timestamp))
                    val line = "$date,$time,${incident.type.name},${incident.latitude},${incident.longitude},${if (incident.isSynced) "Yes" else "No"}\n"
                    stream.write(line.toByteArray())
                }
            }

            Log.i(TAG, "Exported ${incidents.size} incidents to $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            false
        }
    }

    private fun createFileInDocuments(context: Context, fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for API 29+
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/GuardianTrack")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            )

            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            // Fallback for older APIs
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val guardianDir = java.io.File(dir, "GuardianTrack")
            guardianDir.mkdirs()
            val file = java.io.File(guardianDir, fileName)
            file.outputStream()
        }
    }
}
