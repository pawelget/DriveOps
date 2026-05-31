package com.example.mobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Button
class ServicesActivity : AppCompatActivity() {

    private var allServices: List<ServiceRecord> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        val lvServices = findViewById<ListView>(R.id.lvServices)
        // Znajdujemy przycisk i ustawiamy powrót ---
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Zamyka ekran serwisów, automatycznie wracając do ekranu pojazdów
            finish()
        }

        val token = MainActivity.authToken
        if (token == null) {
            Toast.makeText(this, "Brak tokenu!", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.authService.getServices("Bearer $token").enqueue(object : Callback<List<ServiceRecord>> {
            override fun onResponse(call: Call<List<ServiceRecord>>, response: Response<List<ServiceRecord>>) {
                if (response.isSuccessful) {
                    allServices = response.body() ?: emptyList()
                    lvServices.adapter = ServiceAdapter(this@ServicesActivity, allServices)
                } else {
                    Toast.makeText(this@ServicesActivity, "Błąd pobierania danych", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ServiceRecord>>, t: Throwable) {
                Toast.makeText(this@ServicesActivity, "Błąd sieci: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class ServiceAdapter(context: AppCompatActivity, private val services: List<ServiceRecord>) :
        ArrayAdapter<ServiceRecord>(context, 0, services) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_service, parent, false)
            val service = getItem(position)

            val tvServiceDate = view.findViewById<TextView>(R.id.tvServiceDate)
            val tvServiceStatus = view.findViewById<TextView>(R.id.tvServiceStatus)
            val tvServiceCar = view.findViewById<TextView>(R.id.tvServiceCar)
            val tvServiceDetails = view.findViewById<TextView>(R.id.tvServiceDetails)

            // Wyciągamy bezpiecznie datę (tylko część YYYY-MM-DD, jeśli długa)
            val fullDate = service?.data_serwisu ?: "Brak daty"
            tvServiceDate.text = if(fullDate.length > 10) fullDate.substring(0, 10) else fullDate

            tvServiceStatus.text = service?.status ?: "Nieznany"

            val car = service?.samochod
            tvServiceCar.text = "${car?.marka ?: ""} ${car?.model ?: ""} (${car?.numer_rejestracyjny ?: ""})"

            tvServiceDetails.text = """
                Rodzaj: ${service?.rodzaj_serwisu?.nazwa ?: "-"}
                Warsztat: ${service?.nazwa_warsztatu ?: "-"}
                Koszt: ${service?.koszt_calkowity ?: "0.0"} PLN
            """.trimIndent()

            return view
        }
    }
}