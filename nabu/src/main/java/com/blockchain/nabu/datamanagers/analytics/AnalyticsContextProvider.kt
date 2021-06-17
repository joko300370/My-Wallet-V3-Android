package com.blockchain.nabu.datamanagers.analytics

import android.content.res.Resources
import com.blockchain.api.analytics.AnalyticsContext
import com.blockchain.api.analytics.DeviceInfo
import com.blockchain.api.analytics.ScreenInfo
import java.util.Locale
import java.util.TimeZone

interface AnalyticsContextProvider {
    fun context(): AnalyticsContext
}

class AnalyticsContextProviderImpl : AnalyticsContextProvider {
    override fun context(): AnalyticsContext {
        return AnalyticsContext(
            device = getDeviceInfo(),
            locale = Locale.getDefault().toString(),
            screen = getScreenInfo(),
            timezone = TimeZone.getDefault().id
        )
    }

    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            android.os.Build.MANUFACTURER,
            android.os.Build.MODEL,
            android.os.Build.DEVICE
        )
    }

    private fun getScreenInfo(): ScreenInfo {
        return ScreenInfo(
            width = Resources.getSystem().displayMetrics.widthPixels,
            height = Resources.getSystem().displayMetrics.heightPixels,
            density = Resources.getSystem().displayMetrics.density
        )
    }
}