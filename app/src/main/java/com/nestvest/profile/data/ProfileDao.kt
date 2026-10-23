package com.nestvest.profile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProfileDao {
    @Query("SELECT * FROM primary_profile WHERE primarySlot = 1")
    suspend fun read(): ProfileEntity?

    @Insert
    suspend fun insert(profile: ProfileEntity)

    @Query("SELECT COUNT(*) FROM primary_profile")
    suspend fun count(): Int
}
