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

class MainActivity : AppCompatActivity() {

    // Gdzieś musimy przechować token na czas działania aplikacji
    companion object {
        var authToken: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Podpinamy nasz plik XML z wyglądem
        setContentView(R.layout.activity_main)

        // Odnajdujemy nasze pola tekstowe i przycisk na ekranie
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Ustawiamy, co ma się stać po kliknięciu przycisku
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val haslo = etPassword.text.toString().trim()

            // Zabezpieczenie przed pustymi polami
            if (email.isEmpty() || haslo.isEmpty()) {
                tvStatus.text = "Wypełnij oba pola!"
                tvStatus.setTextColor(android.graphics.Color.RED)
                return@setOnClickListener
            }

            tvStatus.text = "Logowanie..."
            tvStatus.setTextColor(android.graphics.Color.GRAY)

            // Tworzymy paczkę danych zgodnie z wymaganiami backendu
            val request = LoginRequest(email, haslo)

            // Strzelamy do API
            ApiClient.authService.loginUser(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        // Udało się! (Kod 200)
                        val body = response.body()
                        authToken = body?.token // Zapisujemy token na później

                        tvStatus.text = "Zalogowano pomyślnie!"
                        tvStatus.setTextColor(android.graphics.Color.GREEN)

                        Toast.makeText(this@MainActivity, "Mamy token: $authToken", Toast.LENGTH_SHORT).show()

                        val intent = android.content.Intent(this@MainActivity, CarsActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Błąd logowania (np. kod 401 - złe hasło)
                        tvStatus.text = "Błąd: Nieprawidłowy email lub hasło"
                        tvStatus.setTextColor(android.graphics.Color.RED)
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // Całkowity błąd połączenia (np. wyłączony Docker)
                    tvStatus.text = "Błąd sieci: ${t.message}"
                    tvStatus.setTextColor(android.graphics.Color.RED)
                }
            })
        }
    }
}