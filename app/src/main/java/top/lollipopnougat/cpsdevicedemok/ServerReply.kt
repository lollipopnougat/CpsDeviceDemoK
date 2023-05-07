package top.lollipopnougat.cpsdevicedemok

import com.beust.klaxon.Json

data class ServerReply(
    @Json("rid")
    val realCarId: String,

    @Json("lat")
    val latitude: Double,

    @Json("lon")
    val longitude: Double,

    @Json("speed")
    val speed: Double,

    @Json("suggest")
    val suggest: String,

    @Json("ts")
    val ts: Long,

    @Json("time")
    val time: String)
