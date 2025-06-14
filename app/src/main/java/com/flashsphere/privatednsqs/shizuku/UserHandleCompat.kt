package com.flashsphere.privatednsqs.shizuku

import android.system.Os

/**
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/os/UserHandle.java
 */
object UserHandleCompat {
    private val MY_USER_ID = getUserId(Os.getuid())
    private const val PER_USER_RANGE = 100000

    fun getUserId(uid: Int): Int = uid / PER_USER_RANGE

    fun myUserId(): Int = MY_USER_ID
}
