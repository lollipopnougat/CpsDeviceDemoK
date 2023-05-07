package top.lollipopnougat.cpsdevicedemok

import com.beust.klaxon.Json

data class CarState(
    @Json("rid")
    val realCarId: String,

    @Json("lat")
    val latitude: Double,

    @Json("lon")
    val longitude: Double,

    @Json("speed")
    val speed: Double,

    @Json("timeStamp")
    val ts: Long)
