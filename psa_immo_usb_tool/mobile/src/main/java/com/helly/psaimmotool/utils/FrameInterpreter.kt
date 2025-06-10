package com.helly.psaimmotool.mobile

import android.annotation.SuppressLint
import android.content.Context
import com.helly.psaimmotool.R

object FrameInterpreter {

    @SuppressLint("StringFormatInvalid")
    fun interpretAndFormat(context: Context, frame: String): String {
        val parts = frame.trim().split(" ")
        if (parts.size < 2) {
            return context.getString(R.string.can_frame_invalid, "CAN")
        }

        return when (parts[0]) {
            "22" -> decodePinFrame(context, parts)
            "09" -> decodeVinFrame(context, parts)
            "01" -> decodeEngineData(context, parts)
            "FD" -> decodePsarCommand(context, parts)
            else -> context.getString(R.string.can_frame_unknown, parts[0])
        }
    }

    private fun decodePinFrame(context: Context, parts: List<String>): String {
        return if (parts.size >= 5) {
            val pin = parts.subList(2, 6).joinToString("") { it }
            context.getString(R.string.pin_detected, pin)
        } else {
            context.getString(R.string.invalid_frame, "PIN")
        }
    }

    private fun decodeVinFrame(context: Context, parts: List<String>): String {
        return if (parts.size >= 20) {
            val hexString = parts.subList(3, 20).joinToString("")
            val vin = hexString.chunked(2)
                .mapNotNull {
                    try {
                        it.toInt(16).toChar()
                    } catch (_: Exception) {
                        null
                    }
                }
                .joinToString("")
            context.getString(R.string.vin_detected, vin)
        } else {
            context.getString(R.string.invalid_frame, "VIN")
        }
    }

    private fun decodeEngineData(context: Context, parts: List<String>): String {
        return try {
            when (val pid = parts[2]) {
                "0C" -> {
                    val rpm = ((parts[3].toInt(16) * 256 + parts[4].toInt(16)) / 4)
                    context.getString(R.string.engine_status, rpm, 0)
                }
                "05" -> {
                    val temp = parts[3].toInt(16) - 40
                    context.getString(R.string.engine_status, 0, temp)
                }
                else -> context.getString(R.string.can_frame_unknown, pid)
            }
        } catch (e: Exception) {
            context.getString(R.string.can_listen_error, e.message ?: "error")
        }
    }

    private fun decodePsarCommand(context: Context, parts: List<String>): String {
        if (parts.size < 4) {
            return context.getString(R.string.invalid_frame, "PSAR")
        }

        val type = parts[2]
        val data = parts[3]

        return when (type) {
            "02" -> decodeSteeringButton(context, data)
            "03" -> decodeClimateCommand(context, data)
            else -> context.getString(R.string.can_frame_unknown, type)
        }
    }

    private fun decodeSteeringButton(context: Context, data: String): String {
        return when (val code = data.substring(0, 2).toInt(16)) {
            0x01 -> context.getString(R.string.btn_source)
            0x02 -> context.getString(R.string.btn_volume_up)
            0x03 -> context.getString(R.string.btn_volume_down)
            0x04 -> context.getString(R.string.btn_next)
            0x05 -> context.getString(R.string.btn_prev)
            else -> context.getString(R.string.unknown_steering_code, code)
        }
    }

    private fun decodeClimateCommand(context: Context, data: String): String {
        return try {
            val temp = data.toInt(16) / 2.0
            context.getString(R.string.climate_temp, temp)
        } catch (_: Exception) {
            context.getString(R.string.invalid_frame, "CLIMATE")
        }
    }
}
