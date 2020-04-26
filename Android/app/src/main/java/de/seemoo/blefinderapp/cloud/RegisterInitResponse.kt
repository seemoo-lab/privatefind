package de.seemoo.blefinderapp.cloud

import android.util.Base64
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class RegisterInitResponse {
    @Expose
    @SerializedName("setup-challenge")
    var setupChallenge : String = ""

    @Expose
    @SerializedName("hmac")
    var hmac : String = ""

    fun getBinarySetupChallenge(): ByteArray {
        return Base64.decode(setupChallenge, Base64.DEFAULT)
    }


}