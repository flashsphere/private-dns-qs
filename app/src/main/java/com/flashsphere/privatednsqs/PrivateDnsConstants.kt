package com.flashsphere.privatednsqs

import android.net.Uri
import androidx.core.net.toUri

object PrivateDnsConstants {
    const val PRIVATE_DNS_SPECIFIER = "private_dns_specifier"
    const val PRIVATE_DNS_MODE = "private_dns_mode"
    val HELP_URL: Uri by lazy { "https://private-dns-qs.web.app/help".toUri() }
}