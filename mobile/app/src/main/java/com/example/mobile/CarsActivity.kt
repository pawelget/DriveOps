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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

/**
 * Aktywność odpowiedzialna za widok pojazdów użytkownika.
 *
 * Umożliwia:
 * - pobieranie samochodów z backendu,
 * - wyszukiwanie pojazdów,
 * - dodawanie nowych samochodów,
 * - edycję istniejących samochodów,
 * - przejście do historii serwisowej, alertów oraz raportów,
 * - wylogowanie użytkownika.
 */
class CarsActivity : AppCompatActivity() {

    // Pełna lista samochodów pobrana z backendu.
    // Jest zachowywana niezależnie od aktualnego filtrowania wyników.
    private var allCars: List<Car> = emptyList()

    // Adapter odpowiedzialny za wyświetlanie kart pojazdów w liście.
    private var adapter: CarAdapter? = null

    // Główne elementy interfejsu wykorzystywane w kilku metodach klasy.
    private lateinit var lvCars: ListView
    private lateinit var etSearch: EditText

    /**
     * Token JWT aktualnie zalogowanego użytkownika.
     * Token jest przechowywany w MainActivity i dołączany do żądań API.
     */
    private val token: String?
        get() = MainActivity.authToken

    /**
     * Metoda uruchamiana podczas tworzenia widoku pojazdów.
     * Inicjalizuje elementy interfejsu, obsługę przycisków,
     * wyszukiwarkę oraz pobiera listę samochodów.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cars)

        /*
         * Pobranie głównego kontenera widoku.
         * Kontener będzie odsuwany od pasków systemowych telefonu,
         * aby nagłówek i dolne przyciski nie nachodziły na interfejs Androida.
         */
        val carsRoot = findViewById<View>(R.id.carsRoot)

        // Zapamiętanie początkowych odstępów ustawionych w pliku XML.
        val originalLeftPadding = carsRoot.paddingLeft
        val originalTopPadding = carsRoot.paddingTop
        val originalRightPadding = carsRoot.paddingRight
        val originalBottomPadding = carsRoot.paddingBottom

        /*
         * Obsługa obszaru zajmowanego przez pasek statusu oraz pasek nawigacyjny.
         * Do podstawowych odstępów widoku dodawane są rzeczywiste rozmiary
         * pasków systemowych aktualnego telefonu.
         */
        ViewCompat.setOnApplyWindowInsetsListener(carsRoot) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                originalLeftPadding + systemBars.left,
                originalTopPadding + systemBars.top,
                originalRightPadding + systemBars.right,
                originalBottomPadding + systemBars.bottom
            )

            windowInsets
        }

        // Wymuszenie zastosowania marginesów systemowych po utworzeniu widoku.
        ViewCompat.requestApplyInsets(carsRoot)

        // Pobranie listy pojazdów oraz pola wyszukiwania z layoutu.
        lvCars = findViewById(R.id.lvCars)
        etSearch = findViewById(R.id.etSearch)

        // Pobranie przycisków dostępnych w widoku pojazdów.
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnToServices = findViewById<Button>(R.id.btnToServices)
        val btnToAlerts = findViewById<Button>(R.id.btnToAlerts)
        val btnAddCar = findViewById<Button>(R.id.btnAddCar)
        val btnToReports = findViewById<Button>(R.id.btnToReports)

        /*
         * Widok pojazdów wymaga zalogowanego użytkownika.
         * Jeżeli token nie istnieje, nie można pobrać prywatnych danych.
         */
        if (token == null) {
            Toast.makeText(
                this,
                "Brak tokenu. Zaloguj się ponownie.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // Przejście do widoku historii napraw i przeglądów.
        btnToServices.setOnClickListener {
            val intent = Intent(this@CarsActivity, ServicesActivity::class.java)
            startActivity(intent)
        }

        // Przejście do widoku alertów przypisanych do pojazdów użytkownika.
        btnToAlerts.setOnClickListener {
            val intent = Intent(this@CarsActivity, AlertsActivity::class.java)
            startActivity(intent)
        }

        // Przejście do widoku raportów serwisowych.
        btnToReports.setOnClickListener {
            val intent = Intent(this@CarsActivity, ReportsActivity::class.java)
            startActivity(intent)
        }

        /*
         * Obsługa wylogowania.
         * Token użytkownika jest usuwany, a aplikacja wraca do ekranu logowania.
         */
        btnLogout.setOnClickListener {
            MainActivity.authToken = null

            val intent = Intent(this@CarsActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        /*
         * Otwarcie pustego formularza dodawania pojazdu.
         * Przekazanie wartości null oznacza, że formularz nie dotyczy
         * istniejącego samochodu, lecz tworzenia nowego wpisu.
         */
        btnAddCar.setOnClickListener {
            showCarDialog(null)
        }

        /*
         * Obsługa wyszukiwarki.
         * Lista pojazdów jest filtrowana po każdej zmianie tekstu,
         * bez wysyłania dodatkowego zapytania do backendu.
         */
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
                // Metoda wymagana przez interfejs TextWatcher.
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                // Filtrowanie wykonywane jest w afterTextChanged().
            }
        })

        // Pobranie pojazdów użytkownika bezpośrednio po otwarciu widoku.
        loadCars()
    }

    /**
     * Pobiera listę samochodów zalogowanego użytkownika z backendu.
     * Do żądania dołączany jest token JWT w nagłówku Authorization.
     */
    private fun loadCars() {
        val currentToken = token ?: return

        ApiClient.authService.getCars("Bearer $currentToken")
            .enqueue(object : Callback<List<Car>> {

                /**
                 * Obsługa odpowiedzi serwera.
                 * Po poprawnym pobraniu dane są przekazywane do adaptera listy.
                 */
                override fun onResponse(
                    call: Call<List<Car>>,
                    response: Response<List<Car>>
                ) {
                    if (response.isSuccessful) {
                        allCars = response.body() ?: emptyList()

                        /*
                         * Przy pierwszym pobraniu tworzymy adapter.
                         * Przy kolejnych pobraniach, np. po dodaniu lub edycji,
                         * odświeżamy widoczną listę z zachowaniem filtra.
                         */
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

                /**
                 * Obsługa błędu połączenia, np. gdy backend nie jest uruchomiony
                 * albo telefon nie ma dostępu do komputera w sieci lokalnej.
                 */
                override fun onFailure(call: Call<List<Car>>, t: Throwable) {
                    Toast.makeText(
                        this@CarsActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Filtruje lokalnie pobraną listę samochodów.
     * Wyszukiwanie obejmuje markę, model, numer rejestracyjny oraz VIN.
     */
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

    /**
     * Wyświetla formularz dodawania lub edycji pojazdu.
     *
     * @param car obiekt edytowanego samochodu albo null,
     *            jeżeli użytkownik dodaje nowy pojazd.
     */
    private fun showCarDialog(car: Car?) {
        // Załadowanie layoutu wspólnego dla dodawania i edycji samochodu.
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_car, null)

        // Pobranie wszystkich pól formularza.
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

        // Jeżeli przekazano obiekt samochodu, formularz działa w trybie edycji.
        val isEditing = car != null

        /*
         * W przypadku edycji formularz zostaje automatycznie wypełniony
         * aktualnymi danymi wybranego pojazdu.
         */
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

        /*
         * Utworzenie okna dialogowego.
         * Tekst tytułu i przycisku zapisu zależy od tego,
         * czy użytkownik dodaje nowy pojazd, czy edytuje istniejący.
         */
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isEditing) "Edytuj pojazd" else "Dodaj pojazd")
            .setView(dialogView)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton(if (isEditing) "Zapisz zmiany" else "Dodaj", null)
            .create()

        /*
         * Własna obsługa przycisku pozytywnego pozwala zatrzymać dialog
         * na ekranie, jeżeli dane formularza są niepoprawne.
         */
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                // Pobranie i przygotowanie wartości tekstowych z formularza.
                val brand = etBrand.text.toString().trim()
                val model = etModel.text.toString().trim()
                val plate = etPlate.text.toString().trim().uppercase()
                val vin = etVin.text.toString().trim().uppercase()
                val fuel = etFuel.text.toString().trim().lowercase()
                val color = etColor.text.toString().trim()

                // Walidacja wymaganych pól tekstowych.
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

                // VIN jest opcjonalny, ale po podaniu musi mieć 17 znaków.
                if (vin.isNotBlank() && vin.length != 17) {
                    etVin.error = "Numer VIN musi mieć dokładnie 17 znaków"
                    return@setOnClickListener
                }

                // Pobranie pól liczbowych jako tekstu przed próbą konwersji.
                val yearText = etYear.text.toString().trim()
                val capacityText = etCapacity.text.toString().trim()
                val powerText = etPower.text.toString().trim()
                val mileageText = etMileage.text.toString().trim()

                /*
                 * Puste pola liczbowe są traktowane jako wartości opcjonalne.
                 * Jeżeli użytkownik poda wartość, musi ona zostać poprawnie
                 * przekonwertowana do liczby całkowitej.
                 */
                val year = yearText.takeIf { it.isNotBlank() }?.toIntOrNull()
                val capacity = capacityText.takeIf { it.isNotBlank() }?.toIntOrNull()
                val power = powerText.takeIf { it.isNotBlank() }?.toIntOrNull()
                val mileage = mileageText.takeIf { it.isNotBlank() }?.toIntOrNull()

                // Walidacja poprawności pól liczbowych.
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

                /*
                 * Maksymalny dopuszczony rok produkcji ustawiono jako
                 * rok następny, aby możliwe było dodanie nowego modelu auta.
                 */
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

                // Lista wartości rodzaju paliwa akceptowanych przez bazę danych.
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

                /*
                 * Utworzenie obiektu przesyłanego do backendu.
                 * Pola opcjonalne są przekazywane jako null, jeżeli
                 * użytkownik pozostawił je puste.
                 */
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

                /*
                 * W zależności od trybu formularza wywoływana jest metoda
                 * dodająca nowy samochód albo aktualizująca istniejący wpis.
                 */
                if (isEditing) {
                    updateCar(car!!.id, request, dialog)
                } else {
                    createCar(request, dialog)
                }
            }
        }

        // Wyświetlenie dialogu użytkownikowi.
        dialog.show()

        /*
         * Ustawienie tła oraz kolorów przycisków dialogu tak,
         * aby formularz był czytelny w ciemnym motywie aplikacji.
         */
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                android.graphics.Color.parseColor("#1E1E24")
            )
        )

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(android.graphics.Color.parseColor("#B69CFF"))

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(android.graphics.Color.parseColor("#B69CFF"))
    }

    /**
     * Wysyła do backendu żądanie utworzenia nowego samochodu.
     * Po poprawnym zapisie dialog jest zamykany, a lista pojazdów odświeżana.
     */
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

                    // Ponowne pobranie danych pozwala od razu wyświetlić nowy pojazd.
                    loadCars()
                } else {
                    /*
                     * Komunikat jest dobierany do rodzaju błędu
                     * zwróconego przez backend.
                     */
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

    /**
     * Wysyła do backendu żądanie aktualizacji wskazanego samochodu.
     *
     * @param carId identyfikator edytowanego pojazdu,
     * @param request nowe dane samochodu,
     * @param dialog formularz, który ma zostać zamknięty po poprawnym zapisie.
     */
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

                    // Ponowne pobranie danych prezentuje użytkownikowi zapisane zmiany.
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

    /**
     * Adapter odpowiadający za utworzenie oraz aktualizację kart pojazdów
     * wyświetlanych w elemencie ListView.
     */
    inner class CarAdapter(
        context: AppCompatActivity,
        private var cars: List<Car>
    ) : ArrayAdapter<Car>(context, 0, cars) {

        /**
         * Aktualizuje dane widoczne w liście, np. po zastosowaniu wyszukiwania.
         */
        fun updateData(newCars: List<Car>) {
            cars = newCars
            notifyDataSetChanged()
        }

        // Zwraca liczbę pojazdów aktualnie widocznych na liście.
        override fun getCount(): Int = cars.size

        // Zwraca pojazd znajdujący się na podanej pozycji listy.
        override fun getItem(position: Int): Car = cars[position]

        /**
         * Przygotowuje wygląd pojedynczej karty samochodu.
         * Jeżeli istnieje już nieużywany widok, zostaje on wykorzystany ponownie.
         */
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_car, parent, false)

            val car = getItem(position)

            // Pobranie elementów widoku pojedynczej karty pojazdu.
            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            val tvPlate = view.findViewById<TextView>(R.id.tvPlate)
            val tvDetails = view.findViewById<TextView>(R.id.tvDetails)
            val btnEditCar = view.findViewById<Button>(R.id.btnEditCar)

            // Wyświetlenie nazwy pojazdu oraz numeru rejestracyjnego.
            tvTitle.text = "${car.marka.orEmpty()} ${car.model.orEmpty()}"
            tvPlate.text = car.numer_rejestracyjny.orEmpty()

            // Wyświetlenie szczegółowych danych pojazdu.
            tvDetails.text = """
                VIN: ${car.vin ?: "-"}
                Rok produkcji: ${car.rok_produkcji ?: "-"}
                Pojemność: ${car.pojemnosc_cm3 ?: "-"} cm³
                Paliwo: ${car.paliwo ?: "-"}
                Przebieg: ${car.przebieg ?: "-"} km
                Moc: ${car.moc_km ?: "-"} KM
                Kolor: ${car.kolor ?: "-"}
            """.trimIndent()

            /*
             * Po kliknięciu przycisku formularz otrzymuje aktualny obiekt auta,
             * dlatego uruchamia się w trybie edycji z uzupełnionymi polami.
             */
            btnEditCar.setOnClickListener {
                showCarDialog(car)
            }

            return view
        }
    }
}