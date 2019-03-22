package com.jetbridge.testapp.yevhen

import android.app.Application

const val API_BASE_URL = "http://185.236.77.69:15050"

/**
 * Implementation of standard Android Application class.
 *
 * Used here mostly as a way to statically obtain required dependency (repository),
 * while complete DI setup is a redundancy here
 */
class MainApp : Application() {

    companion object {
        val repository = TeamMembersRepository(API_BASE_URL)
    }
}
