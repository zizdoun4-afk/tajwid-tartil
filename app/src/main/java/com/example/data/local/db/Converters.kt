package com.example.data.local.db

import androidx.room.TypeConverter
import com.example.audio.SessionMarker
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SessionMarkerTypeConverter {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, SessionMarker::class.java)
    private val adapter = moshi.adapter<List<SessionMarker>>(listType)

    @TypeConverter
    fun fromMarkersList(markers: List<SessionMarker>?): String? {
        return markers?.let { adapter.toJson(it) }
    }

    @TypeConverter
    fun toMarkersList(json: String?): List<SessionMarker>? {
        return if (!json.isNullOrBlank()) {
            try {
                adapter.fromJson(json)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
