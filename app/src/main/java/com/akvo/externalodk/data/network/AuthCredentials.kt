package com.akvo.externalodk.data.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthCredentials @Inject constructor() {
    @Volatile
    var username: String = ""
        private set

    @Volatile
    var password: String = ""
        private set

    val isSet: Boolean
        get() = username.isNotEmpty() && password.isNotEmpty()

    fun set(username: String, password: String) {
        this.username = username
        this.password = password
    }

    fun clear() {
        this.username = ""
        this.password = ""
    }
}
