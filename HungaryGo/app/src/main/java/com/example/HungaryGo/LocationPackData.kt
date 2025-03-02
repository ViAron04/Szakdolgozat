package com.example.HungaryGo

import com.google.android.gms.maps.model.MarkerOptions
import java.io.Serializable

data class LocationPackData(
    var name: String = "",
    var locations: MutableMap<String, LocationDescription?> = mutableMapOf(),
    var description: String = "",
    var rating: Double? = null,
    var completionNumber: Int = 0,
    var area: String = "",
    var origin: String = ""
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

data class FiltersData(
    var teljesitesTeljesitett: Boolean = true,
    var teljesitesFuggoben: Boolean = true,
    var teljesitesHatralevo: Boolean = true,
    var helyszinszamMin: Int = 1,
    var helyszinszamMax: Int = 10,
    var tipusBeepitett: Boolean = true,
    var tipusKozossegi: Boolean = true,
    var teruletFalusi: Boolean = true,
    var teruletVarosi: Boolean = true,
    var teruletOrszagos: Boolean = true,
    var teruletNemzetkozi: Boolean = true
)

data class MakerLocationPackData(
    var name: String = "",
    var locations: MutableList<MakerLocationDescription?> = mutableListOf(),
    var description: String = "",
    var area: String = "",
    var origin: String = "community"
)

data class MakerLocationDescription (
    @Transient
    var name: String? = null,
    var markerOptions: MarkerOptions? = null,
    var description: String? = null,
    var question: String? = null,
    var answer: String? = null,
) : Serializable
