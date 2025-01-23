package com.example.HungaryGo

data class LocationPackData(
    var name: String = "",
    var locations: MutableMap<String, LocationDescription?> = mutableMapOf(),
    var description: String = "",
    var rating: Int? = null
)

data class LocationDescription (
    var isQuestion: Boolean = false,
    var description: String = "",
    var question: String? = null,
    var solution: String? = null
)
