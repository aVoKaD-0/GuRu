package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TasksWithTextsResponseDto(
    @Json(name = "tasks") val tasks: List<TaskDto>,
    @Json(name = "texts") val texts: List<TextDataDto>
) 