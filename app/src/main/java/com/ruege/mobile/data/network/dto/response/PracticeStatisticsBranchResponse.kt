package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PracticeStatisticsBranchResponse(
    @field:Json(name = "status")
    val status: String,
    @field:Json(name = "new_server_sync_timestamp")
    val newServerSyncTimestamp: Long
) 