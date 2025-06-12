package com.ruege.mobile.data.mapper

import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.network.dto.response.PracticeStatisticSyncResponseDto

fun PracticeStatisticSyncResponseDto.toEntity(): PracticeStatisticsEntity {
    return PracticeStatisticsEntity(
        this.egeNumber,
        this.totalAttempts,
        this.correctAttempts,
        this.lastAttemptDate
    )
} 