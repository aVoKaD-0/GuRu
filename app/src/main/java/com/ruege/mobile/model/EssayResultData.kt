package com.ruege.mobile.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class EssayResultData(
    @Json(name = "essay_content") val essayContent: String,
    @Json(name = "result") val result: String
) {
    fun toJsonString(): String {
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(EssayResultData::class.java)
        return jsonAdapter.toJson(this)
    }

    companion object {
        fun fromJsonString(jsonString: String): EssayResultData? {
            return try {
                val moshi = Moshi.Builder().build()
                val jsonAdapter = moshi.adapter(EssayResultData::class.java)
                jsonAdapter.fromJson(jsonString)
            } catch (e: Exception) {
                null
            }
        }
    }
} 