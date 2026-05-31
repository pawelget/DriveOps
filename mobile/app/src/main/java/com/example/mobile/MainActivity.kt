package com.example.mobile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Główna aktywność aplikacji odpowiedzialna za ekran logowania.
 *
 * Użytkownik wprowadza adres e-mail oraz hasło, a aplikacja
 * przesyła te dane do backendu. Po poprawnym uwierzytelnieniu
 * otrzymany token JWT jest zapisywany i użytkownik przechodzi
 * do widoku swoich pojazdów.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Obiekt współdzielony przechowujący token JWT na czas działania aplikacji.
     *
     * Token jest zwracany przez backend po poprawnym logowaniu
     * i wykorzystywany przez kolejne widoki do wykonywania
     * autoryzowanych żądań, na przykład pobierania samochodów,
     * serwisów, alertów lub raportów.
     *
     * Po wylogowaniu wartość tokenu jest ustawiana na null.
     */
    companion object {
        var authToken: String? = null
    }

    /**
     * Metoda wywoływana podczas tworzenia ekranu logowania.
     * Odpowiada za połączenie kodu z layoutem XML oraz
     * ustawienie obsługi przycisku logowania.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Podpięcie pliku XML określającego wygląd ekranu logowania.
        setContentView(R.layout.activity_main)

        /*
         * Pobranie elementów interfejsu z layoutu:
         * - pola adresu e-mail,
         * - pola hasła,
         * - przycisku logowania,
         * - pola przeznaczonego na komunikaty dla użytkownika.
         */
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        /**
         * Obsługa kliknięcia przycisku logowania.
         * Po kliknięciu dane są pobierane z formularza,
         * sprawdzane, a następnie wysyłane do backendu.
         */
        btnLogin.setOnClickListener {

            // Pobranie danych wpisanych przez użytkownika.
            // Funkcja trim() usuwa przypadkowe spacje na początku i końcu tekstu.
            val email = etEmail.text.toString().trim()
            val haslo = etPassword.text.toString().trim()

            /*
             * Podstawowa walidacja formularza.
             * Żądanie do backendu nie jest wysyłane,
             * jeżeli użytkownik pozostawił któreś pole puste.
             */
            if (email.isEmpty() || haslo.isEmpty()) {
                tvStatus.text = "Wypełnij oba pola!"
                tvStatus.setTextColor(android.graphics.Color.RED)
                return@setOnClickListener
            }

            /*
             * Wyświetlenie informacji o trwającym logowaniu.
             * Komunikat jest widoczny do czasu otrzymania
             * odpowiedzi z backendu albo błędu sieciowego.
             */
            tvStatus.text = "Logowanie..."
            tvStatus.setTextColor(android.graphics.Color.GRAY)

            /*
             * Utworzenie obiektu zawierającego dane logowania.
             * Nazwy pól klasy LoginRequest odpowiadają danym
             * oczekiwanym przez endpoint logowania backendu.
             */
            val request = LoginRequest(email, haslo)

            /*
             * Wysłanie asynchronicznego żądania logowania do API.
             * Dzięki metodzie enqueue() interfejs aplikacji nie zostaje
             * zablokowany podczas oczekiwania na odpowiedź serwera.
             */
            ApiClient.authService.loginUser(request)
                .enqueue(object : Callback<LoginResponse> {

                    /**
                     * Metoda wykonywana, gdy backend zwróci odpowiedź HTTP.
                     * Odpowiedź może oznaczać poprawne logowanie
                     * albo błąd, na przykład nieprawidłowe hasło.
                     */
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        if (response.isSuccessful) {
                            /*
                             * Poprawne logowanie użytkownika.
                             * Backend zwraca token JWT, który zapisujemy
                             * w aplikacji do użycia w kolejnych żądaniach.
                             */
                            val body = response.body()
                            authToken = body?.token

                            /*
                             * Dodatkowe zabezpieczenie:
                             * nawet przy poprawnym kodzie odpowiedzi sprawdzamy,
                             * czy token rzeczywiście został zwrócony.
                             */
                            if (authToken == null) {
                                tvStatus.text = "Błąd: backend nie zwrócił tokenu"
                                tvStatus.setTextColor(android.graphics.Color.RED)
                                return
                            }

                            // Poinformowanie użytkownika o poprawnym zalogowaniu.
                            tvStatus.text = "Zalogowano pomyślnie!"
                            tvStatus.setTextColor(android.graphics.Color.GREEN)

                            /*
                             * Przejście do widoku pojazdów.
                             * Od tej chwili kolejne aktywności mogą korzystać
                             * z zapisanego tokenu JWT, aby pobierać dane konta.
                             */
                            val intent = android.content.Intent(
                                this@MainActivity,
                                CarsActivity::class.java
                            )
                            startActivity(intent)

                        } else {
                            /*
                             * Backend odrzucił próbę logowania,
                             * na przykład z powodu błędnego e-maila lub hasła.
                             */
                            tvStatus.text = "Błąd: Nieprawidłowy email lub hasło"
                            tvStatus.setTextColor(android.graphics.Color.RED)
                        }
                    }

                    /**
                     * Metoda wykonywana w przypadku całkowitego błędu połączenia.
                     *
                     * Taka sytuacja może wystąpić, gdy:
                     * - backend nie jest uruchomiony,
                     * - kontenery Docker są wyłączone,
                     * - telefon nie ma dostępu do komputera w sieci lokalnej,
                     * - w ApiClient ustawiono niepoprawny adres backendu.
                     */
                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        tvStatus.text = "Błąd sieci: ${t.message}"
                        tvStatus.setTextColor(android.graphics.Color.RED)
                    }
                })
        }
    }
}