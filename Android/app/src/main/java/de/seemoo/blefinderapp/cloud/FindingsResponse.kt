package de.seemoo.blefinderapp.cloud

import android.location.Location
import android.util.Base64
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.Exception
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class FindingsResponse {
    @Expose
    @SerializedName("findings")
    lateinit var findings : List<FindReport>

    companion object {
        const val IV_LENGTH = 16 // AES IV
        const val HMAC_LENGTH = 32 // sha256
    }

    class FindReport {
        @Expose @SerializedName("mac-address")
        lateinit var macAddress : String
        @Expose @SerializedName("counter")
        var counter : Int = 0
        @Expose @SerializedName("e2e-message")
        lateinit var e2eMessage : String
        @Expose @SerializedName("server-timestamp")
        lateinit var serverTimestamp : Date

        fun getE2eMessageBinary() : ByteArray {
            return Base64.decode(e2eMessage, Base64.DEFAULT);
        }

        fun decryptAndParseGeoloc(e2eKey:ByteArray):Location {
            return parseGeoloc(decrypt(e2eKey))
        }
        fun parseGeoloc(plainMessage:ByteArray) :Location{
            val buf = ByteBuffer.wrap(plainMessage)
            val loc = Location("privatefind_remote")
            val reserved= buf.getInt(0)
            loc.latitude = buf.getFloat(4).toDouble()
            loc.longitude = buf.getFloat(8).toDouble()
            loc.accuracy = buf.getFloat(12)
            val reserved2 = buf.getInt(16)
            return loc
        }
        fun decrypt(e2eKey : ByteArray): ByteArray {
            val bin = getE2eMessageBinary()

            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(e2eKey, "HmacSHA256"))
            mac.update(bin, 0, bin.size - HMAC_LENGTH)
            val hmacCorrect = mac.doFinal()

            // TODO may be off by one
            val hmacReceived = bin.sliceArray(bin.size - HMAC_LENGTH .. bin.size)

            if (!MessageDigest.isEqual(hmacCorrect, hmacReceived)) {
                throw Exception("HMAC mismatch")
            }

            val cipher = Cipher.getInstance("AES/CTR/NOPADDING")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(e2eKey, "AES"),
                    IvParameterSpec(bin, 0, IV_LENGTH))
            val deciphered = cipher.doFinal(bin, IV_LENGTH, bin.size - IV_LENGTH - HMAC_LENGTH)
            return deciphered
        }

    }
}
