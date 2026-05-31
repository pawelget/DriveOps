package com.example.mobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Button
class CarsActivity : AppCompatActivity() {

    private var allCars: List<Car> = listOf() // Tu trzymamy wszystkie auta
    private var adapter: CarAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cars)

        val lvCars = findViewById<ListView>(R.id.lvCars)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        // Znajdujemy nasz nowy przycisk
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val token = MainActivity.authToken
        if (token == null) {
            Toast.makeText(this, "Brak tokenu!", Toast.LENGTH_SHORT).show()
            return
        }
        val btnToServices = findViewById<Button>(R.id.btnToServices)
        btnToServices.setOnClickListener {
            val intent = android.content.Intent(this@CarsActivity, ServicesActivity::class.java)
            startActivity(intent)
        }
        val btnToAlerts = findViewById<Button>(R.id.btnToAlerts)
        btnToAlerts.setOnClickListener {
            val intent = android.content.Intent(this@CarsActivity, AlertsActivity::class.java)
            startActivity(intent)
        }
        val btnToReports = findViewById<Button>(R.id.btnToReports)
        btnToReports.setOnClickListener {
            val intent = android.content.Intent(this@CarsActivity, ReportsActivity::class.java)
            startActivity(intent)
        }

        // --- Logika wylogowania ---
        btnLogout.setOnClickListener {
            // 1. Kasujemy token z pamięci
            MainActivity.authToken = null

            // 2. Tworzymy bilet powrotny do ekranu logowania
            val intent = android.content.Intent(this@CarsActivity, MainActivity::class.java)
            startActivity(intent)

            // 3. Zamykamy obecny ekran z listą pojazdów
            finish()
        }

        // --- Logika wyszukiwarki ---
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = allCars.filter {
                    (it.marka?.lowercase()?.contains(query) == true) ||
                            (it.model?.lowercase()?.contains(query) == true) ||
                            (it.numer_rejestracyjny?.lowercase()?.contains(query) == true)
                }
                adapter?.updateData(filtered)
            }
        })

        // --- Pobieranie danych z API ---
        ApiClient.authService.getCars("Bearer $token").enqueue(object : Callback<List<Car>> {
            override fun onResponse(call: Call<List<Car>>, response: Response<List<Car>>) {
                if (response.isSuccessful) {
                    allCars = response.body() ?: emptyList()
                    adapter = CarAdapter(this@CarsActivity, allCars)
                    lvCars.adapter = adapter
                } else {
                    Toast.makeText(this@CarsActivity, "Błąd pobierania danych", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Car>>, t: Throwable) {
                Toast.makeText(this@CarsActivity, "Błąd sieci: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- Custom Adapter do rysowania ładnych kart ---
    inner class CarAdapter(context: AppCompatActivity, private var cars: List<Car>) :
        ArrayAdapter<Car>(context, 0, cars) {

        fun updateData(newCars: List<Car>) {
            cars = newCars
            notifyDataSetChanged()
        }

        override fun getCount(): Int = cars.size
        override fun getItem(position: Int): Car = cars[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_car, parent, false)
            val car = getItem(position)

            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            val tvPlate = view.findViewById<TextView>(R.id.tvPlate)
            val tvDetails = view.findViewById<TextView>(R.id.tvDetails)

            tvTitle.text = "${car.marka ?: ""} ${car.model ?: ""}"
            tvPlate.text = car.numer_rejestracyjny ?: ""

            // Formatujemy szczegóły
            tvDetails.text = """
                Rok: ${car.rok_produkcji ?: "-"}
                Paliwo: ${car.paliwo ?: "-"}
                Przebieg: ${car.przebieg ?: "-"} km
                Moc: ${car.moc_km ?: "-"} KM
                Kolor: ${car.kolor ?: "-"}
            """.trimIndent()

            return view
        }
    }
}