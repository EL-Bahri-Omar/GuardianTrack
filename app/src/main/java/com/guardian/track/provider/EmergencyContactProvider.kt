package com.guardian.track.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.room.Room
import com.guardian.track.data.local.db.GuardianDatabase
import com.guardian.track.data.local.db.entity.EmergencyContactEntity
import com.guardian.track.util.Constants

class EmergencyContactProvider : ContentProvider() {

    companion object {
        private const val TAG = "EmergencyContactProvider"
        private const val CONTACTS = 1
        private const val CONTACT_ID = 2

        val CONTENT_URI: Uri = Uri.parse("content://${Constants.PROVIDER_AUTHORITY}/${Constants.PROVIDER_PATH_CONTACTS}")

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(Constants.PROVIDER_AUTHORITY, Constants.PROVIDER_PATH_CONTACTS, CONTACTS)
            addURI(Constants.PROVIDER_AUTHORITY, "${Constants.PROVIDER_PATH_CONTACTS}/#", CONTACT_ID)
        }
    }

    private lateinit var database: GuardianDatabase

    override fun onCreate(): Boolean {
        context?.let { ctx ->
            database = Room.databaseBuilder(
                ctx.applicationContext,
                GuardianDatabase::class.java,
                GuardianDatabase.DATABASE_NAME
            ).build()
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val cursor = when (uriMatcher.match(uri)) {
            CONTACTS -> {
                database.emergencyContactDao().getAllContactsCursor()
            }
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                database.emergencyContactDao().getContactByIdCursor(id)
            }
            else -> {
                Log.w(TAG, "Unknown URI: $uri")
                null
            }
        }

        cursor?.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != CONTACTS) {
            throw IllegalArgumentException("Unknown URI: $uri")
        }

        values ?: return null

        val name = values.getAsString("name") ?: return null
        val phoneNumber = values.getAsString("phone_number") ?: return null

        // Insert synchronously using Room's allowMainThreadQueries is not ideal,
        // but ContentProvider operations are expected to be synchronous.
        // We use a background thread to insert.
        var insertedId: Long = -1
        val thread = Thread {
            val entity = EmergencyContactEntity(name = name, phoneNumber = phoneNumber)
            insertedId = database.emergencyContactDao().let { dao ->
                kotlinx.coroutines.runBlocking {
                    dao.insertContact(entity)
                }
            }
        }
        thread.start()
        thread.join()

        if (insertedId > 0) {
            val newUri = ContentUris.withAppendedId(CONTENT_URI, insertedId)
            context?.contentResolver?.notifyChange(newUri, null)
            return newUri
        }

        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                val thread = Thread {
                    kotlinx.coroutines.runBlocking {
                        database.emergencyContactDao().deleteContact(id)
                    }
                }
                thread.start()
                thread.join()
                context?.contentResolver?.notifyChange(uri, null)
                1
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        // Not required by spec but implementing basic support
        Log.d(TAG, "Update not fully implemented")
        return 0
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            CONTACTS -> "vnd.android.cursor.dir/vnd.${Constants.PROVIDER_AUTHORITY}.${Constants.PROVIDER_PATH_CONTACTS}"
            CONTACT_ID -> "vnd.android.cursor.item/vnd.${Constants.PROVIDER_AUTHORITY}.${Constants.PROVIDER_PATH_CONTACTS}"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}
