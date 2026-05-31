package com.example.mobile

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

// 1. dane wysyłane ( 'email', haslo)
data class LoginRequest(val email: String, val haslo: String)

// 2. Wzbogacona odpowiedź, żeby złapać token z backendu
data class LoginResponse(
    val message: String?,
    val token: String?,
    val error: String?
)
// Definicja pojedynczego samochodu (pola takie same jak w Pythonie)
data class Car(
    val id: Int,
    val marka: String?,
    val model: String?,
    val numer_rejestracyjny: String?,
    val rok_produkcji: Int?,
    val paliwo: String?,
    val przebieg: Int?,
    val moc_km: Int?,
    val kolor: String?
)
// 3. Nasz Endpoint (odpowiednik AuthApi.jsx)
interface AuthApiService {
    @POST("/auth/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    // Pobieranie aut. Wymaga podania tokenu!
    @GET("/samochody")
    fun getCars(@Header("Authorization") token: String): Call<List<Car>>
}

// 4. Konfiguracja głównego klienta
object ApiClient {
    // Magiczny adres 10.0.2.2 wskazuje na Dockera na Twoim Windowsie!
    private const val BASE_URL = "http://10.0.2.2:5000"

    val authService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}