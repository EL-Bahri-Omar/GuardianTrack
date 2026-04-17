package com.guardian.track.data.remote.api

import com.guardian.track.data.remote.api.dto.IncidentDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GuardianApiService {

    @POST("incidents")
    suspend fun sendIncident(@Body incident: IncidentDto): Response<IncidentDto>

    @GET("incidents")
    suspend fun getIncidents(): Response<List<IncidentDto>>
}
