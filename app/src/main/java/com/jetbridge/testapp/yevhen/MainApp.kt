package com.jetbridge.testapp.yevhen

import android.app.Application

const val API_BASE_URL = "http://192.168.1.127:15050"

class MainApp : Application() {

    companion object {
        val repository = TeamMembersRepository(API_BASE_URL)
    }
}
