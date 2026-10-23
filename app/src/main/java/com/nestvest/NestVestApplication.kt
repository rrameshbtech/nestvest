package com.nestvest

import android.app.Application
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.data.RoomProfileRepository
import com.nestvest.profile.domain.ProfileRepository

class NestVestApplication : Application() {
    val repository: ProfileRepository by lazy { RoomProfileRepository(ProfileDatabase.open(this)) }
}
