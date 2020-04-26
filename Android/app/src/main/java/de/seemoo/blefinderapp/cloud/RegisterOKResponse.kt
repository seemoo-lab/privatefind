package de.seemoo.blefinderapp.cloud

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class RegisterOKResponse {
    @Expose
    @SerializedName("access-token")
    val token : String = ""

}