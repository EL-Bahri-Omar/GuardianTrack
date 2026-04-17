package com.guardian.track.data.local.db.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardian.track.data.local.db.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Query("DELETE FROM emergency_contacts WHERE _id = :id")
    suspend fun deleteContact(id: Long)

    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts WHERE _id = :id")
    suspend fun getContactById(id: Long): EmergencyContactEntity?

    // For ContentProvider - returns Cursor directly
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContactsCursor(): Cursor

    @Query("SELECT * FROM emergency_contacts WHERE _id = :id")
    fun getContactByIdCursor(id: Long): Cursor
}
