package com.guardian.track.data.repository

import com.guardian.track.data.local.db.dao.EmergencyContactDao
import com.guardian.track.data.local.db.entity.EmergencyContactEntity
import com.guardian.track.domain.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: EmergencyContactDao
) {

    fun getAllContacts(): Flow<List<EmergencyContact>> {
        return contactDao.getAllContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addContact(name: String, phoneNumber: String): Long {
        val entity = EmergencyContactEntity(
            name = name,
            phoneNumber = phoneNumber
        )
        return contactDao.insertContact(entity)
    }

    suspend fun deleteContact(id: Long) {
        contactDao.deleteContact(id)
    }

    suspend fun getContactById(id: Long): EmergencyContact? {
        return contactDao.getContactById(id)?.toDomain()
    }

    // Mapper
    private fun EmergencyContactEntity.toDomain(): EmergencyContact {
        return EmergencyContact(
            id = id,
            name = name,
            phoneNumber = phoneNumber
        )
    }
}
