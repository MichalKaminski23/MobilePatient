package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val first_name: String,
    val last_name: String,
    val pesel: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val role: String? = null,
    val email: String? = null,
)

@Serializable
data class ShortProfileDto(
    val id: String,
    val first_name: String,
    val last_name: String,
    val role: String
)