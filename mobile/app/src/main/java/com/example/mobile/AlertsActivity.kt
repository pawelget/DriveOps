package com.example.mobile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlertsActivity : AppCompatActivity() {

    private var alertsList: List<Alert> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        val lvAlerts = findViewById<ListView>(R.id.lvAlerts)
        val btnBackFromAlerts = findViewById<Button>(R.id.btnBackFromAlerts)

        btnBackFromAlerts.setOnClickListener { finish() }

        val token = MainActivity.authToken
        if (token == null) {
            Toast.makeText(this, "Brak tokenu!", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.authService.getAlerts("Bearer $token").enqueue(object : Callback<AlertsResponse> {
            override fun onResponse(call: Call<AlertsResponse>, response: Response<AlertsResponse>) {
                if (response.isSuccessful) {
                    // Wyciągamy listę z obiektu AlertsResponse
                    alertsList = response.body()?.alerts ?: emptyList()
                    lvAlerts.adapter = AlertAdapter(this@AlertsActivity, alertsList)
                } else {
                    Toast.makeText(this@AlertsActivity, "Błąd pobierania danych", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AlertsResponse>, t: Throwable) {
                Toast.makeText(this@AlertsActivity, "Błąd sieci: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class AlertAdapter(context: AppCompatActivity, private val alerts: List<Alert>) :
        ArrayAdapter<Alert>(context, 0, alerts) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_alert, parent, false)
            val alert = getItem(position)

            val tvAlertTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
            val tvAlertDesc = view.findViewById<TextView>(R.id.tvAlertDesc)
            val tvAlertDate = view.findViewById<TextView>(R.id.tvAlertDate)
            val tvAlertPriority = view.findViewById<TextView>(R.id.tvAlertPriority)

            tvAlertTitle.text = alert?.tytul ?: "Brak tytułu"
            tvAlertDesc.text = alert?.opis ?: ""

            val fullDate = alert?.data ?: ""
            tvAlertDate.text = if(fullDate.length > 10) fullDate.substring(0, 10) else fullDate

            // Logika kolorowania priorytetów
            when (alert?.priorytet) {
                "critical" -> {
                    tvAlertPriority.text = "KRYTYCZNE"
                    tvAlertPriority.setTextColor(Color.parseColor("#FF4C4C")) // Czerwony
                }
                "warning" -> {
                    tvAlertPriority.text = "OSTRZEŻENIE"
                    tvAlertPriority.setTextColor(Color.parseColor("#FFA500")) // Pomarańczowy
                }
                "info" -> {
                    tvAlertPriority.text = "INFO"
                    tvAlertPriority.setTextColor(Color.parseColor("#4C9AFF")) // Niebieski
                }
                else -> {
                    tvAlertPriority.text = alert?.priorytet?.uppercase()
                    tvAlertPriority.setTextColor(Color.GRAY)
                }
            }

            return view
        }
    }
}