import ar.edu.uade.example.digidex.data.model.Digimon
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path // IMPORTA ESTE

interface DigimonApiService {
    @GET("api/digimon")
    suspend fun getAllDigimons(): List<Digimon>

    @GET("api/digimon/level/{level}")
    suspend fun getDigimonsByLevel(@Path("level") level: String): List<Digimon>
}

object DigimonApi {
    val retrofitService: DigimonApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://digimon-api.vercel.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DigimonApiService::class.java)
    }
}