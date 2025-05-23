package com.flashsphere.privatednsqs.shizuku

import android.Manifest
import android.content.Context
import android.content.pm.IPackageManager
import android.os.Build
import android.os.UserHandle
import android.permission.IPermissionManager
import androidx.annotation.RequiresApi
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

object ShizukuUtils {
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun grantWriteSecureSettingsPermission(context: Context): Boolean {
        return grantPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)
    }

    private fun grantPermission(context: Context, permissionName: String): Boolean {
        val packageName = context.packageName

        val userId = runCatching {
            UserHandle::class.java.getMethod("myUserId").invoke(null) as? Int ?: 0
        }.onFailure {
            Timber.e(it, "Failed to get user id")
        }.getOrDefault(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/permission")
            val permissionManager = getPermissionManager()

            runCatching {
                permissionManager.grantRuntimePermission(packageName, permissionName, userId)
                return true
            }.onFailure {
                Timber.e(it, "Grant permission using Android 11 API failed")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    // Android 14 r29
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permissionName,
                        context.deviceId,
                        userId
                    )
                    return true
                }.onFailure {
                    Timber.e(it, "Grant permission using Android 14 r29 API failed")
                }.recoverCatching {
                    // Android 14 r50
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permissionName,
                        "default:" + Context.DEVICE_ID_DEFAULT,
                        userId
                    )
                    return true
                }.onFailure {
                    Timber.e(it, "Grant permission using Android 14 r50 API failed")
                }
            }
        } else {
            runCatching {
                getPackageManager().grantRuntimePermission(packageName, permissionName, userId)
                return true
            }
        }
        return false
    }

    private fun getPackageManager(): IPackageManager {
        return IPackageManager.Stub.asInterface(ShizukuBinderWrapper(
            SystemServiceHelper.getSystemService("package")))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getPermissionManager(): IPermissionManager {
        return IPermissionManager.Stub.asInterface(ShizukuBinderWrapper(
            SystemServiceHelper.getSystemService("permissionmgr")))
    }
}