package com.jetbridge.testapp.yevhen

import android.app.Application

const val API_BASE_URL = "http://185.236.77.69:15050"

class MainApp : Application() {

    companion object {
        val repository = TeamMembersRepository(API_BASE_URL)
    }
}
