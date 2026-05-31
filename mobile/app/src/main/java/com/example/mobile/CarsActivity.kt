package com.example.mobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class CarsActivity : AppCompatActivity() {

    private var allCars: List<Car> = emptyList()
    private var adapter: CarAdapter? = null

    private lateinit var lvCars: ListView
    private lateinit var etSearch: EditText

    private val token: String?
        get() = MainActivity.authToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cars)

        lvCars = findViewById(R.id.lvCars)
        etSearch = findViewById(R.id.etSearch)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnToServices = findViewById<Button>(R.id.btnToServices)
        val btnToAlerts = findViewById<Button>(R.id.btnToAlerts)
        val btnAddCar = findViewById<Button>(R.id.btnAddCar)

        if (token == null) {
            Toast.makeText(
                this,
                "Brak tokenu. Zaloguj się ponownie.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        btnToServices.setOnClickListener {
            val intent = Intent(this@CarsActivity, ServicesActivity::class.java)
            startActivity(intent)
        }

        btnToAlerts.setOnClickListener {
            val intent = Intent(this@CarsActivity, AlertsActivity::class.java)
            startActivity(intent)
        }
        val btnToReports = findViewById<Button>(R.id.btnToReports)
        btnToReports.setOnClickListener {
            val intent = android.content.Intent(this@CarsActivity, ReportsActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            MainActivity.authToken = null

            val intent = Intent(this@CarsActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnAddCar.setOnClickListener {
            showCarDialog(null)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCars()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })

        loadCars()
    }

    private fun loadCars() {
        val currentToken = token ?: return

        ApiClient.authService.getCars("Bearer $currentToken")
            .enqueue(object : Callback<List<Car>> {

                override fun onResponse(
                    call: Call<List<Car>>,
                    response: Response<List<Car>>
                ) {
                    if (response.isSuccessful) {
                        allCars = response.body() ?: emptyList()

                        if (adapter == null) {
                            adapter = CarAdapter(this@CarsActivity, allCars)
                            lvCars.adapter = adapter
                        } else {
                            filterCars()
                        }
                    } else {
                        Toast.makeText(
                            this@CarsActivity,
                            "Błąd pobierania pojazdów. Kod: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<Car>>, t: Throwable) {
                    Toast.makeText(
                        this@CarsActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun filterCars() {
        val query = etSearch.text.toString().trim().lowercase()

        val filteredCars = if (query.isBlank()) {
            allCars
        } else {
            allCars.filter { car ->
                car.marka?.lowercase()?.contains(query) == true ||
                        car.model?.lowercase()?.contains(query) == true ||
                        car.numer_rejestracyjny?.lowercase()?.contains(query) == true ||
                        car.vin?.lowercase()?.contains(query) == true
            }
        }

        adapter?.updateData(filteredCars)
    }

    private fun showCarDialog(car: Car?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_car, null)

        val etBrand = dialogView.findViewById<EditText>(R.id.etEditBrand)
        val etModel = dialogView.findViewById<EditText>(R.id.etEditModel)
        val etPlate = dialogView.findViewById<EditText>(R.id.etEditPlate)
        val etVin = dialogView.findViewById<EditText>(R.id.etEditVin)
        val etYear = dialogView.findViewById<EditText>(R.id.etEditYear)
        val etCapacity = dialogView.findViewById<EditText>(R.id.etEditCapacity)
        val etPower = dialogView.findViewById<EditText>(R.id.etEditPower)
        val etFuel = dialogView.findViewById<EditText>(R.id.etEditFuel)
        val etMileage = dialogView.findViewById<EditText>(R.id.etEditMileage)
        val etColor = dialogView.findViewById<EditText>(R.id.etEditColor)

        val isEditing = car != null

        if (isEditing) {
            etBrand.setText(car?.marka.orEmpty())
            etModel.setText(car?.model.orEmpty())
            etPlate.setText(car?.numer_rejestracyjny.orEmpty())
            etVin.setText(car?.vin.orEmpty())
            etYear.setText(car?.rok_produkcji?.toString().orEmpty())
            etCapacity.setText(car?.pojemnosc_cm3?.toString().orEmpty())
            etPower.setText(car?.moc_km?.toString().orEmpty())
            etFuel.setText(car?.paliwo.orEmpty())
            etMileage.setText(car?.przebieg?.toString().orEmpty())
            etColor.setText(car?.kolor.orEmpty())
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isEditing) "Edytuj pojazd" else "Dodaj pojazd")
            .setView(dialogView)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton(if (isEditing) "Zapisz zmiany" else "Dodaj", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val brand = etBrand.text.toString().trim()
                val model = etModel.text.toString().trim()
                val plate = etPlate.text.toString().trim().uppercase()
                val vin = etVin.text.toString().trim().uppercase()
                val fuel = etFuel.text.toString().trim().lowercase()
                val color = etColor.text.toString().trim()

                if (brand.isBlank()) {
                    etBrand.error = "Podaj markę"
                    return@setOnClickListener
                }

                if (model.isBlank()) {
                    etModel.error = "Podaj model"
                    return@setOnClickListener
                }

                if (plate.isBlank()) {
                    etPlate.error = "Podaj numer rejestracyjny"
                    return@setOnClickListener
                }

                if (vin.isNotBlank() && vin.length != 17) {
                    etVin.error = "Numer VIN musi mieć dokładnie 17 znaków"
                    return@setOnClickListener
                }

                val yearText = etYear.text.toString().trim()
                val capacityText = etCapacity.text.toString().trim()
                val powerText = etPower.text.toString().trim()
                val mileageText = etMileage.text.toString().trim()

                val year = yearText.takeIf { it.isNotBlank() }?.toIntOrNull()
                val capacity = capacityText.takeIf { it.isNotBlank() }?.toIntOrNull()
                val power = powerText.takeIf { it.isNotBlank() }?.toIntOrNull()
                val mileage = mileageText.takeIf { it.isNotBlank() }?.toIntOrNull()

                if (yearText.isNotBlank() && year == null) {
                    etYear.error = "Rok produkcji musi być liczbą"
                    return@setOnClickListener
                }

                if (capacityText.isNotBlank() && capacity == null) {
                    etCapacity.error = "Pojemność musi być liczbą"
                    return@setOnClickListener
                }

                if (powerText.isNotBlank() && power == null) {
                    etPower.error = "Moc musi być liczbą"
                    return@setOnClickListener
                }

                if (mileageText.isNotBlank() && mileage == null) {
                    etMileage.error = "Przebieg musi być liczbą"
                    return@setOnClickListener
                }

                val nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1

                if (year != null && (year < 1886 || year > nextYear)) {
                    etYear.error = "Podaj poprawny rok produkcji"
                    return@setOnClickListener
                }

                if (capacity != null && capacity <= 0) {
                    etCapacity.error = "Pojemność musi być większa od zera"
                    return@setOnClickListener
                }

                if (power != null && power <= 0) {
                    etPower.error = "Moc musi być większa od zera"
                    return@setOnClickListener
                }

                if (mileage != null && mileage < 0) {
                    etMileage.error = "Przebieg nie może być ujemny"
                    return@setOnClickListener
                }

                val allowedFuelTypes = listOf(
                    "benzyna",
                    "diesel",
                    "elektryczny",
                    "hybryda",
                    "benzyna_gaz"
                )

                if (fuel.isNotBlank() && fuel !in allowedFuelTypes) {
                    etFuel.error =
                        "Wpisz: benzyna, diesel, elektryczny, hybryda lub benzyna_gaz"
                    return@setOnClickListener
                }

                val request = CarRequest(
                    vin = vin.ifBlank { null },
                    numer_rejestracyjny = plate,
                    marka = brand,
                    model = model,
                    rok_produkcji = year,
                    pojemnosc_cm3 = capacity,
                    moc_km = power,
                    paliwo = fuel.ifBlank { null },
                    przebieg = mileage,
                    kolor = color.ifBlank { null }
                )

                if (isEditing) {
                    updateCar(car!!.id, request, dialog)
                } else {
                    createCar(request, dialog)
                }
            }
        }

        dialog.show()
    }

    private fun createCar(
        request: CarRequest,
        dialog: AlertDialog
    ) {
        val currentToken = token ?: return

        ApiClient.authService.createCar(
            "Bearer $currentToken",
            request
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CarsActivity,
                        "Pojazd został dodany",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                    loadCars()
                } else {
                    val message = when (response.code()) {
                        400 -> "Sprawdź poprawność wprowadzonych danych."
                        401 -> "Sesja wygasła. Zaloguj się ponownie."
                        409 -> "Pojazd z takim numerem rejestracyjnym lub VIN już istnieje."
                        else -> "Nie udało się dodać pojazdu. Kod: ${response.code()}"
                    }

                    Toast.makeText(
                        this@CarsActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@CarsActivity,
                    "Błąd sieci: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateCar(
        carId: Int,
        request: CarRequest,
        dialog: AlertDialog
    ) {
        val currentToken = token ?: return

        ApiClient.authService.updateCar(
            "Bearer $currentToken",
            carId,
            request
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CarsActivity,
                        "Dane pojazdu zostały zapisane",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                    loadCars()
                } else {
                    val message = when (response.code()) {
                        400 -> "Sprawdź poprawność wprowadzonych danych."
                        401 -> "Sesja wygasła. Zaloguj się ponownie."
                        404 -> "Nie znaleziono wskazanego pojazdu."
                        409 -> "Pojazd z takim numerem rejestracyjnym lub VIN już istnieje."
                        else -> "Nie udało się zapisać zmian. Kod: ${response.code()}"
                    }

                    Toast.makeText(
                        this@CarsActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@CarsActivity,
                    "Błąd sieci: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    inner class CarAdapter(
        context: AppCompatActivity,
        private var cars: List<Car>
    ) : ArrayAdapter<Car>(context, 0, cars) {

        fun updateData(newCars: List<Car>) {
            cars = newCars
            notifyDataSetChanged()
        }

        override fun getCount(): Int = cars.size

        override fun getItem(position: Int): Car = cars[position]

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_car, parent, false)

            val car = getItem(position)

            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            val tvPlate = view.findViewById<TextView>(R.id.tvPlate)
            val tvDetails = view.findViewById<TextView>(R.id.tvDetails)
            val btnEditCar = view.findViewById<Button>(R.id.btnEditCar)

            tvTitle.text = "${car.marka.orEmpty()} ${car.model.orEmpty()}"
            tvPlate.text = car.numer_rejestracyjny.orEmpty()

            tvDetails.text = """
                VIN: ${car.vin ?: "-"}
                Rok produkcji: ${car.rok_produkcji ?: "-"}
                Pojemność: ${car.pojemnosc_cm3 ?: "-"} cm³
                Paliwo: ${car.paliwo ?: "-"}
                Przebieg: ${car.przebieg ?: "-"} km
                Moc: ${car.moc_km ?: "-"} KM
                Kolor: ${car.kolor ?: "-"}
            """.trimIndent()

            btnEditCar.setOnClickListener {
                showCarDialog(car)
            }

            return view
        }
    }
}