package com.flashsphere.privatednsqs.json

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object SingletonJsonLoader {
    @OptIn(ExperimentalSerializationApi::class)
    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            exceptionsWithDebugInfo = true
        }
    }

    fun get(): Json {
        return json
    }
}

inline val Context.json: Json
    get() = SingletonJsonLoader.get()
