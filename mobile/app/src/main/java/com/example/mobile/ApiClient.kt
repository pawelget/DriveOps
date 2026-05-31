package com.example.mobile

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Dane wysyłane podczas logowania.
data class LoginRequest(
    val email: String,
    val haslo: String
)

// Odpowiedź backendu po próbie logowania.
data class LoginResponse(
    val message: String?,
    val token: String?,
    val error: String?
)

// Dane pojedynczego samochodu pobierane z backendu.
data class Car(
    val id: Int,
    val vin: String?,
    val numer_rejestracyjny: String?,
    val marka: String?,
    val model: String?,
    val rok_produkcji: Int?,
    val pojemnosc_cm3: Int?,
    val moc_km: Int?,
    val paliwo: String?,
    val przebieg: Int?,
    val kolor: String?
)

data class CarRequest(
    val vin: String?,
    val numer_rejestracyjny: String,
    val marka: String,
    val model: String,
    val rok_produkcji: Int?,
    val pojemnosc_cm3: Int?,
    val moc_km: Int?,
    val paliwo: String?,
    val przebieg: Int?,
    val kolor: String?
)

// Modele pomocnicze historii serwisów.
data class ServiceType(
    val nazwa: String?
)

data class ServiceCar(
    val marka: String?,
    val model: String?,
    val numer_rejestracyjny: String?
)

data class ServiceRecord(
    val id: Int,
    val data_serwisu: String?,
    val nazwa_warsztatu: String?,
    val status: String?,
    val koszt_calkowity: Double?,
    val rodzaj_serwisu: ServiceType?,
    val samochod: ServiceCar?
)

data class ServiceTaskRequest(
    val nazwa_zadania: String,
    val opis: String?,
    val koszt_robocizny: Double
)

data class UsedPartRequest(
    val nazwa_czesci: String,
    val producent_czesci: String?,
    val ilosc: Double,
    val cena_jednostkowa: Double
)

data class ServiceRequest(
    val samochod_id: Int,
    val rodzaj_serwisu_id: Int?,
    val data_serwisu: String,
    val nazwa_warsztatu: String?,
    val adres_warsztatu: String?,
    val przebieg_przy_serwisie: Int?,
    val opis: String?,
    val status: String,
    val zadania: List<ServiceTaskRequest>,
    val uzyte_czesci: List<UsedPartRequest>
)

data class ServiceTypeOption(
    val id: Int,
    val nazwa: String?
)

// Modele alertów.
data class Alert(
    val id: String,
    val typ: String,
    val priorytet: String,
    val tytul: String,
    val opis: String,
    val data: String,
    val samochod_info: String
)

data class AlertsResponse(
    val alerts: List<Alert>
)

// Endpointy wykorzystywane przez aplikację mobilną.
interface AuthApiService {

    @POST("/auth/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @GET("/samochody")
    fun getCars(
        @Header("Authorization") token: String
    ): Call<List<Car>>

    @POST("/samochody")
    fun createCar(
        @Header("Authorization") token: String,
        @Body request: CarRequest
    ): Call<ResponseBody>

    @PUT("/samochody/{id}")
    fun updateCar(
        @Header("Authorization") token: String,
        @Path("id") carId: Int,
        @Body request: CarRequest
    ): Call<ResponseBody>

    @GET("/wpisy-serwisowe")
    fun getServices(
        @Header("Authorization") token: String
    ): Call<List<ServiceRecord>>

    @GET("/rodzaje-serwisu")
    fun getServiceTypes(
        @Header("Authorization") token: String
    ): Call<List<ServiceTypeOption>>

    @POST("/wpisy-serwisowe")
    fun createService(
        @Header("Authorization") token: String,
        @Body request: ServiceRequest
    ): Call<ResponseBody>

    @GET("/alerts")
    fun getAlerts(
        @Header("Authorization") token: String
    ): Call<AlertsResponse>
}

object ApiClient {

    /*
     * Adres 10.0.2.2 pozwala emulatorowi Androida połączyć się
     * z backendem uruchomionym na komputerze.
     * Dla fizycznego telefonu należy ustawić adres IP komputera,
     * np. http://192.168.1.97:5000
     */
    private const val BASE_URL = "http://10.0.2.2:5000"

    val authService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}