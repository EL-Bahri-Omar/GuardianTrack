package com.guardian.track.di

import android.content.Context
import androidx.room.Room
import com.guardian.track.data.local.db.GuardianDatabase
import com.guardian.track.data.local.db.dao.EmergencyContactDao
import com.guardian.track.data.local.db.dao.IncidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GuardianDatabase {
        return Room.databaseBuilder(
            context,
            GuardianDatabase::class.java,
            GuardianDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideIncidentDao(database: GuardianDatabase): IncidentDao {
        return database.incidentDao()
    }

    @Provides
    fun provideEmergencyContactDao(database: GuardianDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }
}
