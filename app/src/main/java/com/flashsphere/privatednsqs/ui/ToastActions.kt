package com.flashsphere.privatednsqs.ui

interface ToastActions {
    fun showToast(message: String)
    fun cancelToast()
}

object NoOpToastActions : ToastActions {
    override fun showToast(message: String) = Unit
    override fun cancelToast() = Unit
}
