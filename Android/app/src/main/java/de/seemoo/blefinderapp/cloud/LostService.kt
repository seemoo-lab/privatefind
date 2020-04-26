package de.seemoo.blefinderapp.cloud

import retrofit2.Call
import retrofit2.http.*

interface LostService {
    @GET("register")
    fun RegisterInit() : Call<RegisterInitResponse>

    @FormUrlEncoded
    @POST("register")
    fun RegisterResponse(@Field("hmac") hmac:String,
                         @Field("mac-address") macAddress:String,
                         @Field("setup-response") setupResponse:String) : Call<RegisterOKResponse>

    @FormUrlEncoded
    @POST("found")
    fun Found(@Field("mac-address") macAddress: String,
              @Field("counter") counter: Int,
              @Field("signature") signature: String,
              @Field("e2e-message") e2eMessage: String) : Call<Unit>

    @GET("findings")
    fun GetFindings(@Query("access-token") accessToken:String) : Call<FindingsResponse>

    @DELETE("findings")
    fun DeleteFindings(@Field("access-token") accessToken:String) : Call<Unit>


}