package ar.edu.uade.example.digidex

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DapiService {
    @GET("api/v1/digimon")
    suspend fun getDigimonPage(@Query("page") page: Int): DapiDigimonListResponse
    @GET("api/v1/digimon/{id}")
    suspend fun getDigimonById(@Path("id") id: String): DapiDigimonResponse
    @GET("api/v1/digimon/{name}")
    suspend fun getDigimonByName(@Path("name") name: String): DapiDigimonResponse
}

object DapiApi {
    val retrofitService: DapiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://digi-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DapiService::class.java)
    }
}
