package com.bassengine.control.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.bassengine.core.DEVICE_BLUETOOTH
import com.bassengine.core.DEVICE_HEADPHONE
import com.bassengine.core.DEVICE_SPEAKER

/** What the phone is currently sending media audio to. */
data class OutputRoute(
    val key: String = DEVICE_SPEAKER,
    val label: String = "扬声器",
    val name: String = "内置扬声器"
)

object OutputRoutes {
    /** Bluetooth wins over wired, wired over the built-in speaker. */
    fun detect(context: Context): OutputRoute {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            var bluetooth: AudioDeviceInfo? = null
            var wired: AudioDeviceInfo? = null
            var speaker: AudioDeviceInfo? = null
            for (d in am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                when (d.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER,
                    AudioDeviceInfo.TYPE_BLE_BROADCAST -> if (bluetooth == null) bluetooth = d

                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> if (wired == null) wired = d

                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> speaker = d
                }
            }
            when {
                bluetooth != null -> OutputRoute(DEVICE_BLUETOOTH, "蓝牙", nameOf(bluetooth, "蓝牙设备"))
                wired != null -> OutputRoute(DEVICE_HEADPHONE, "有线耳机", nameOf(wired, "有线耳机"))
                else -> OutputRoute(DEVICE_SPEAKER, "扬声器", "内置扬声器")
            }
        } catch (_: Throwable) {
            OutputRoute()
        }
    }

    private fun nameOf(device: AudioDeviceInfo?, fallback: String): String {
        val name = device?.productName?.toString()?.trim().orEmpty()
        return if (name.isEmpty() || name.equals("unknown", ignoreCase = true)) fallback else name
    }
}
