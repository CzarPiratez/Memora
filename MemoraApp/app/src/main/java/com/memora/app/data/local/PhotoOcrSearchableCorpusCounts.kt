package com.memora.app.data.local

import androidx.room.ColumnInfo

data class PhotoOcrSearchableCorpusCounts(
    @ColumnInfo(name = "photo_count") val photoCount: Int,
)
