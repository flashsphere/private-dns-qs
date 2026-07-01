package com.flashsphere.privatednsqs.hilt

import jakarta.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ComputeDispatcher
