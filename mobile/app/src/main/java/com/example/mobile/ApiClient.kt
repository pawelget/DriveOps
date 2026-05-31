package com.example.mobile

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/*
 * Plik zawiera:
 * - modele danych przesyłanych pomiędzy aplikacją mobilną a backendem,
 * - definicje endpointów API wykorzystywanych przez aplikację,
 * - konfigurację klienta Retrofit odpowiedzialnego za komunikację sieciową.
 *
 * Modele danych odpowiadają obiektom JSON zwracanym lub oczekiwanym
 * przez backend systemu DriveOps.
 */

// =========================
// LOGOWANIE UŻYTKOWNIKA
// =========================

/**
 * Dane przesyłane do backendu podczas próby logowania.
 *
 * @param email adres e-mail podany przez użytkownika,
 * @param haslo hasło użytkownika.
 */
data class LoginRequest(
    val email: String,
    val haslo: String
)

/**
 * Odpowiedź zwracana przez backend po próbie logowania.
 *
 * Przy poprawnym logowaniu pole token zawiera token JWT,
 * który jest później wykorzystywany do autoryzacji kolejnych żądań.
 *
 * @param message opcjonalny komunikat backendu,
 * @param token token JWT zwracany po poprawnym logowaniu,
 * @param error komunikat błędu zwracany przy nieudanej operacji.
 */
data class LoginResponse(
    val message: String?,
    val token: String?,
    val error: String?
)

// =========================
// POJAZDY
// =========================

/**
 * Model pojedynczego samochodu pobieranego z backendu.
 *
 * Obiekt zawiera dane pojazdu przypisanego do zalogowanego użytkownika.
 * Pola opcjonalne mogą przyjmować wartość null, jeżeli użytkownik
 * nie podał ich podczas dodawania pojazdu.
 */
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

/**
 * Dane wysyłane do backendu podczas dodawania lub edycji pojazdu.
 *
 * Marka, model oraz numer rejestracyjny są wymagane w formularzu.
 * Pozostałe informacje mogą być uzupełniane opcjonalnie.
 */
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

// =========================
// HISTORIA SERWISOWA
// =========================

/**
 * Skrócona informacja o rodzaju serwisu zwracana razem z wpisem serwisowym.
 *
 * Przykładową wartością może być przegląd okresowy albo naprawa.
 */
data class ServiceType(
    val nazwa: String?
)

/**
 * Skrócone dane samochodu dołączane do wpisu serwisowego.
 *
 * Pozwalają wyświetlić użytkownikowi, którego pojazdu dotyczy
 * dana naprawa lub przegląd.
 */
data class ServiceCar(
    val marka: String?,
    val model: String?,
    val numer_rejestracyjny: String?
)

/**
 * Model wpisu historii serwisowej pobieranego z backendu.
 *
 * Zawiera podstawowe informacje o wykonanej usłudze oraz powiązany
 * pojazd i rodzaj serwisu. Jest wykorzystywany w widoku historii serwisowej.
 */
data class ServiceRecord(
    val id: Int,
    val data_serwisu: String?,
    val nazwa_warsztatu: String?,
    val status: String?,
    val koszt_calkowity: Double?,
    val rodzaj_serwisu: ServiceType?,
    val samochod: ServiceCar?
)

/**
 * Dane pojedynczej czynności wykonywanej podczas serwisu.
 *
 * Każdy wpis serwisowy może zawierać wiele wykonanych czynności,
 * na przykład wymianę oleju, kontrolę hamulców albo wymianę filtrów.
 */
data class ServiceTaskRequest(
    val nazwa_zadania: String,
    val opis: String?,
    val koszt_robocizny: Double
)

/**
 * Dane części wykorzystanej podczas wykonywania serwisu.
 *
 * Cena jednostkowa oraz ilość pozwalają backendowi obliczyć
 * koszt wykorzystanych części.
 */
data class UsedPartRequest(
    val nazwa_czesci: String,
    val producent_czesci: String?,
    val ilosc: Double,
    val cena_jednostkowa: Double
)

/**
 * Komplet danych wysyłanych do backendu podczas dodawania nowego serwisu.
 *
 * Obiekt zawiera podstawowe dane wpisu, listę wykonanych czynności
 * oraz listę części użytych podczas naprawy lub przeglądu.
 */
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

/**
 * Model rodzaju serwisu dostępnego do wyboru w formularzu dodawania wpisu.
 *
 * Identyfikator jest przesyłany do backendu podczas zapisu formularza,
 * natomiast nazwa jest wyświetlana użytkownikowi w rozwijanej liście.
 */
data class ServiceTypeOption(
    val id: Int,
    val nazwa: String?
)

// =========================
// ALERTY
// =========================

/**
 * Model pojedynczego alertu wyświetlanego użytkownikowi.
 *
 * Alert może informować na przykład o zbliżającym się serwisie
 * albo zdarzeniu wymagającym uwagi użytkownika.
 */
data class Alert(
    val id: String,
    val typ: String,
    val priorytet: String,
    val tytul: String,
    val opis: String,
    val data: String,
    val samochod_info: String
)

/**
 * Odpowiedź backendu zawierająca listę alertów.
 *
 * Backend zwraca alerty wewnątrz obiektu, dlatego aplikacja
 * odczytuje właściwą listę z pola alerts.
 */
data class AlertsResponse(
    val alerts: List<Alert>
)

// =========================
// RAPORTY SERWISOWE
// =========================

/**
 * Skrócone dane samochodu prezentowane w raporcie serwisowym.
 */
data class ReportCar(
    val marka: String?,
    val model: String?,
    val numer_rejestracyjny: String?
)

/**
 * Dane wpisu serwisowego dołączane do raportu.
 *
 * Są wykorzystywane do wyświetlenia kosztu naprawy
 * oraz rodzaju serwisu na karcie raportu.
 */
data class ReportService(
    val koszt_calkowity: Double?,
    val rodzaj_serwisu_nazwa: String?
)

/**
 * Model raportu pobieranego z backendu.
 *
 * Zawiera numer dokumentu, datę wygenerowania oraz informacje
 * o pojeździe i wpisie serwisowym, którego raport dotyczy.
 */
data class ReportRecord(
    val id: Int,
    val numer_raportu: String?,
    val wygenerowano_w: String?,
    val samochod: ReportCar?,
    val wpis_serwisowy: ReportService?
)

/**
 * Dane przesyłane podczas żądania wysłania raportu e-mailem.
 *
 * Wartość null oznacza wykorzystanie adresu e-mail
 * przypisanego do konta zalogowanego użytkownika.
 */
data class EmailRequest(
    val email: String? = null
)

/**
 * Odpowiedź backendu po próbie wysłania raportu e-mailem.
 */
data class EmailResponse(
    val message: String?,
    val error: String?
)

// =========================
// DEFINICJE ENDPOINTÓW API
// =========================

/**
 * Interfejs Retrofit definiujący endpointy backendu używane
 * przez aplikację mobilną.
 *
 * Każda metoda odpowiada jednemu żądaniu HTTP.
 * Endpointy chronione wymagają tokenu JWT przekazywanego
 * w nagłówku Authorization w formacie: Bearer <token>.
 */
interface AuthApiService {

    // -------------------------
    // Logowanie
    // -------------------------

    /**
     * Wysyła dane logowania użytkownika do backendu.
     * Endpoint jest publiczny, dlatego nie wymaga tokenu JWT.
     */
    @POST("/auth/login")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    // -------------------------
    // Samochody
    // -------------------------

    /**
     * Pobiera wszystkie pojazdy przypisane do zalogowanego użytkownika.
     */
    @GET("/samochody")
    fun getCars(
        @Header("Authorization") token: String
    ): Call<List<Car>>

    /**
     * Dodaje nowy samochód do konta użytkownika.
     */
    @POST("/samochody")
    fun createCar(
        @Header("Authorization") token: String,
        @Body request: CarRequest
    ): Call<ResponseBody>

    /**
     * Aktualizuje dane istniejącego samochodu.
     *
     * @param carId identyfikator edytowanego pojazdu.
     */
    @PUT("/samochody/{id}")
    fun updateCar(
        @Header("Authorization") token: String,
        @Path("id") carId: Int,
        @Body request: CarRequest
    ): Call<ResponseBody>

    // -------------------------
    // Historia serwisowa
    // -------------------------

    /**
     * Pobiera historię napraw i przeglądów użytkownika.
     */
    @GET("/wpisy-serwisowe")
    fun getServices(
        @Header("Authorization") token: String
    ): Call<List<ServiceRecord>>

    /**
     * Pobiera dostępne rodzaje serwisu wykorzystywane
     * w formularzu dodawania nowego wpisu.
     */
    @GET("/rodzaje-serwisu")
    fun getServiceTypes(
        @Header("Authorization") token: String
    ): Call<List<ServiceTypeOption>>

    /**
     * Dodaje nowy wpis serwisowy wraz z czynnościami oraz częściami.
     */
    @POST("/wpisy-serwisowe")
    fun createService(
        @Header("Authorization") token: String,
        @Body request: ServiceRequest
    ): Call<ResponseBody>

    // -------------------------
    // Alerty
    // -------------------------

    /**
     * Pobiera alerty przypisane do pojazdów zalogowanego użytkownika.
     */
    @GET("/alerts")
    fun getAlerts(
        @Header("Authorization") token: String
    ): Call<AlertsResponse>

    // -------------------------
    // Raporty
    // -------------------------

    /**
     * Pobiera listę raportów serwisowych użytkownika.
     */
    @GET("/raporty")
    fun getReports(
        @Header("Authorization") token: String
    ): Call<List<ReportRecord>>

    /**
     * Usuwa wybrany raport serwisowy.
     *
     * @param reportId identyfikator raportu przeznaczonego do usunięcia.
     */
    @DELETE("/raporty/{id}")
    fun deleteReport(
        @Header("Authorization") token: String,
        @Path("id") reportId: Int
    ): Call<Void>

    /**
     * Pobiera wygenerowany raport w postaci dokumentu PDF.
     *
     * Odpowiedź ma typ ResponseBody, ponieważ zawiera dane binarne pliku,
     * a nie standardowy obiekt JSON.
     */
    @GET("/raporty/{id}/pdf")
    fun downloadReportPdf(
        @Header("Authorization") token: String,
        @Path("id") reportId: Int
    ): Call<ResponseBody>

    /**
     * Zleca wysłanie raportu na adres e-mail użytkownika.
     *
     * @param reportId identyfikator raportu przeznaczonego do wysłania,
     * @param request opcjonalny adres e-mail; przy wartości null backend
     *                może wykorzystać adres przypisany do konta.
     */
    @POST("/raporty/{id}/wyslij-email")
    fun sendReportEmail(
        @Header("Authorization") token: String,
        @Path("id") reportId: Int,
        @Body request: EmailRequest
    ): Call<EmailResponse>
}

// =========================
// KONFIGURACJA RETROFIT
// =========================

/**
 * Obiekt konfigurujący głównego klienta komunikacji z backendem.
 *
 * Zastosowanie obiektu singleton powoduje, że jedna instancja klienta API
 * jest współdzielona przez wszystkie aktywności aplikacji.
 */
object ApiClient {

    /*
     * Adres bazowy backendu.
     *
     * Przy uruchamianiu aplikacji na fizycznym telefonie należy podać
     * adres IP komputera, na którym działa backend. Telefon i komputer
     * muszą znajdować się w tej samej sieci lokalnej.
     *
     * Adres http://10.0.2.2:5000 pozwala symulatorowi android połączyć się z backendem
     * znajdującym się na tym samym komputerze.
     */
    private const val BASE_URL = "http://10.0.2.2"

    /**
     * Utworzenie klienta Retrofit.
     *
     * Klient:
     * - korzysta z ustawionego adresu backendu,
     * - zamienia odpowiedzi JSON na obiekty danych języka Kotlin
     *   przy użyciu GsonConverterFactory,
     * - udostępnia metody zdefiniowane w interfejsie AuthApiService.
     *
     * Słowo lazy oznacza, że obiekt zostanie utworzony dopiero
     * przy pierwszej próbie użycia komunikacji z API.
     */
    val authService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}