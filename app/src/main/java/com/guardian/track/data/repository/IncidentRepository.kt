package com.guardian.track.data.repository

import android.util.Log
import com.guardian.track.data.local.db.dao.IncidentDao
import com.guardian.track.data.local.db.entity.IncidentEntity
import com.guardian.track.data.remote.NetworkResult
import com.guardian.track.data.remote.api.GuardianApiService
import com.guardian.track.data.remote.api.dto.IncidentDto
import com.guardian.track.domain.model.Incident
import com.guardian.track.domain.model.IncidentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val incidentDao: IncidentDao,
    private val apiService: GuardianApiService
) {
    companion object {
        private const val TAG = "IncidentRepository"
    }

    fun getAllIncidents(): Flow<List<Incident>> {
        return incidentDao.getAllIncidents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun recordIncident(type: IncidentType, latitude: Double, longitude: Double): Long {
        val entity = IncidentEntity(
            timestamp = System.currentTimeMillis(),
            type = type.name,
            latitude = latitude,
            longitude = longitude,
            isSynced = false
        )
        val id = incidentDao.insertIncident(entity)
        Log.d(TAG, "Recorded incident: type=$type, id=$id")

        // Try immediate sync
        try {
            val dto = entity.copy(id = id).toDto()
            val response = apiService.sendIncident(dto)
            if (response.isSuccessful) {
                incidentDao.markAsSynced(id)
                Log.d(TAG, "Incident $id synced immediately")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Immediate sync failed for incident $id, will retry later", e)
        }

        return id
    }

    suspend fun syncPendingIncidents(): NetworkResult<Int> {
        return try {
            val unsynced = incidentDao.getUnsyncedIncidents()
            if (unsynced.isEmpty()) {
                return NetworkResult.Success(0)
            }

            var syncedCount = 0
            for (entity in unsynced) {
                try {
                    val response = apiService.sendIncident(entity.toDto())
                    if (response.isSuccessful) {
                        incidentDao.markAsSynced(entity.id)
                        syncedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync incident ${entity.id}", e)
                }
            }
            Log.d(TAG, "Synced $syncedCount/${unsynced.size} incidents")
            NetworkResult.Success(syncedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            NetworkResult.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun deleteIncident(id: Long) {
        incidentDao.deleteIncident(id)
    }

    suspend fun getAllIncidentsList(): List<Incident> {
        return incidentDao.getAllIncidentsList().map { it.toDomain() }
    }

    // Mapper extensions
    private fun IncidentEntity.toDomain(): Incident {
        return Incident(
            id = id,
            timestamp = timestamp,
            type = IncidentType.fromString(type),
            latitude = latitude,
            longitude = longitude,
            isSynced = isSynced
        )
    }

    private fun IncidentEntity.toDto(): IncidentDto {
        return IncidentDto(
            timestamp = timestamp,
            type = type,
            latitude = latitude,
            longitude = longitude
        )
    }
}
