package com.flashsphere.privatednsqs.util

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_MODE
import com.flashsphere.privatednsqs.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PrivateDnsTest {
    lateinit var context: Context
    lateinit var contentResolver: ContentResolver
    lateinit var privateDns: PrivateDns

    @Before
    fun setup() {
        mockkStatic(Settings.Global::class)
        mockkStatic(ContextCompat::class)

        contentResolver = mockk()
        context = mockk<Context>().also {
            every { it.contentResolver } returns contentResolver
        }
        privateDns = PrivateDns(context)
    }

    @Test
    fun hasPermission_granted() {
        every { ContextCompat.checkSelfPermission(any(), any()) } returns
                PackageManager.PERMISSION_GRANTED
        assertThat(privateDns.hasPermission()).isTrue()
    }

    @Test
    fun hasPermission_denied() {
        every { ContextCompat.checkSelfPermission(any(), any()) } returns
                PackageManager.PERMISSION_DENIED
        assertThat(privateDns.hasPermission()).isFalse()
    }

    @Test
    fun getCurrentDnsConfig_on() {
        val configs = createDnsConfigs()

        // current dns in configs
        every { Settings.Global.getString(any(), PRIVATE_DNS_MODE) } returns DnsMode.On.value
        every { Settings.Global.getString(any(), PRIVATE_DNS_SPECIFIER) } returns "dns.google"

        privateDns.getCurrentDnsConfig(configs).also { dnsConfig ->
            assertThat(dnsConfig).isEqualTo(configs[3])
        }

        // current dns not in configs
        every { Settings.Global.getString(any(), PRIVATE_DNS_MODE) } returns DnsMode.On.value
        every { Settings.Global.getString(any(), PRIVATE_DNS_SPECIFIER) } returns "test"

        privateDns.getCurrentDnsConfig(configs).also { dnsConfig ->
            assertThat(configs.contains(dnsConfig)).isFalse()
            assertThat(dnsConfig).isNotNull().isInstanceOf<DnsConfiguration.On>()
            dnsConfig as DnsConfiguration.On
            assertThat(dnsConfig.hostname).isEqualTo("test")
            assertThat(dnsConfig.icon).isNull()
        }
    }

    @Test
    fun getCurrentDnsConfig_off() {
        every { Settings.Global.getString(any(), PRIVATE_DNS_MODE) } returns DnsMode.Off.value

        val configs = createDnsConfigs()

        val dnsConfig = privateDns.getCurrentDnsConfig(configs)
        assertThat(dnsConfig).isEqualTo(configs[0])
    }

    @Test
    fun getCurrentDnsConfig_auto() {
        every { Settings.Global.getString(any(), PRIVATE_DNS_MODE) } returns DnsMode.Auto.value

        val configs = createDnsConfigs()

        val dnsConfig = privateDns.getCurrentDnsConfig(configs)
        assertThat(dnsConfig).isEqualTo(configs[1])
    }

    @Test
    fun getNextDnsConfig() {
        val configs = createDnsConfigs()

        // from last to first
        every { Settings.Global.getString(any(), PRIVATE_DNS_MODE) } returns DnsMode.On.value
        every { Settings.Global.getString(any(), PRIVATE_DNS_SPECIFIER) } returns "dns.google"

        privateDns.getNextDnsConfig(configs).also { dnsConfig ->
            assertThat(dnsConfig).isEqualTo(configs[0])
        }

        // from auto to on
        every { Settings.Global.getString(any(), PRIVATE_DNS_MODE) } returns DnsMode.Auto.value

        privateDns.getNextDnsConfig(configs).also { dnsConfig ->
            assertThat(dnsConfig).isEqualTo(configs[2])
        }

        // current dns not in configs
        every { Settings.Global.getString(any(), PRIVATE_DNS_MODE) } returns DnsMode.On.value
        every { Settings.Global.getString(any(), PRIVATE_DNS_SPECIFIER) } returns "test"

        privateDns.getNextDnsConfig(configs).also { dnsConfig ->
            assertThat(dnsConfig).isEqualTo(configs[0])
        }
    }

    @Test
    fun setDnsConfig_on_with_hostname() {
        every { Settings.Global.putString(any(), any(), any()) } returns true

        val dnsConfiguration = DnsConfiguration.On("one.one.one.one", null)
        privateDns.setDnsConfig(dnsConfiguration)

        verify {
            Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, DnsMode.On.value)
            Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER, dnsConfiguration.hostname)
        }
    }

    @Test
    fun setDnsConfig_off() {
        every { Settings.Global.putString(any(), any(), any()) } returns true

        val dnsConfiguration = DnsConfiguration.Off
        privateDns.setDnsConfig(dnsConfiguration)

        verify {
            Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, DnsMode.Off.value)
        }
    }

    @Test
    fun setDnsConfig_auto() {
        every { Settings.Global.putString(any(), any(), any()) } returns true

        val dnsConfiguration = DnsConfiguration.Auto
        privateDns.setDnsConfig(dnsConfiguration)

        verify {
            Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, DnsMode.Auto.value)
        }
    }

    private fun createDnsConfigs(): List<DnsConfiguration> {
        return listOf(
            DnsConfiguration.Off,
            DnsConfiguration.Auto,
            DnsConfiguration.On("one.one.one.one", "1.png"),
            DnsConfiguration.On("dns.google", "2.png"),
        )
    }
}