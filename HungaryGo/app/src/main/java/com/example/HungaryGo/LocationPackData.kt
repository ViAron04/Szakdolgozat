package com.example.HungaryGo

data class LocationPackData(
    var name: String = "",
    var locations: MutableMap<String, LocationDescription?> = mutableMapOf(),
    var description: String = "",
    var rating: Int? = null
)

data class LocationDescription (
    var isQuestion: Boolean = false,
    var description: String? = null,
    var question: String? = null,
    var solution: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null
)
