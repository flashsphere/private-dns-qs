package com.flashsphere.privatednsqs.util

import kotlinx.coroutines.CancellationException

suspend inline fun <reified T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        Result.failure(t)
    }
