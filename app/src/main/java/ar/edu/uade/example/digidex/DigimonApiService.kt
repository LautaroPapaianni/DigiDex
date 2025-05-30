import ar.edu.uade.example.digidex.Digimon
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface DigimonApiService {
    @GET("api/digimon")
    suspend fun getAllDigimons(): List<Digimon>
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

