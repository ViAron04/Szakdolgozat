package com.example.HungaryGo

import com.google.android.gms.maps.model.MarkerOptions
import java.io.Serializable

data class LocationPackData(
    var name: String = "",
    var locations: MutableMap<String, LocationDescription?> = mutableMapOf(),
    var description: String = "",
    var rating: Double? = null,
    var completionNumber: Int = 0,
    var area: String = "none"
) : Serializable
// a serializable az intentként törénő küldéshez kell

data class LocationDescription (
    @Transient
    var markerOptions: MarkerOptions? = null,
    var description: String? = null,
    var question: String? = null,
    var answer: String? = null,
) : Serializable

data class CurrentUserLocationPackData (
    var completionCount: Int = 0,
    var usersRating: Double = 0.0
)