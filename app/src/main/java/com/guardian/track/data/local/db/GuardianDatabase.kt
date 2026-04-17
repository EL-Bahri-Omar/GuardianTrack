package com.guardian.track.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.guardian.track.data.local.db.dao.EmergencyContactDao
import com.guardian.track.data.local.db.dao.IncidentDao
import com.guardian.track.data.local.db.entity.EmergencyContactEntity
import com.guardian.track.data.local.db.entity.IncidentEntity

@Database(
    entities = [IncidentEntity::class, EmergencyContactEntity::class],
    version = 1,
    exportSchema = true
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        const val DATABASE_NAME = "guardian_track_db"
    }
}
