package com.example.mobile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Aktywność odpowiedzialna za historię serwisową pojazdów użytkownika.
 *
 * Widok umożliwia:
 * - pobieranie i wyświetlanie dotychczasowych wpisów serwisowych,
 * - przejście z powrotem do ekranu pojazdów,
 * - otwarcie formularza dodawania nowego serwisu,
 * - przypisanie serwisu do wybranego samochodu,
 * - dodanie wykonanych czynności i wykorzystanych części,
 * - przesłanie nowego wpisu do backendu.
 */
class ServicesActivity : AppCompatActivity() {

    /**
     * Lista wpisów serwisowych pobranych z backendu.
     * Jest przekazywana do adaptera odpowiedzialnego za wyświetlanie kart serwisów.
     */
    private var allServices: List<ServiceRecord> = emptyList()

    // Lista widoczna na ekranie, zawierająca historię napraw i przeglądów.
    private lateinit var lvServices: ListView

    /**
     * Token JWT zalogowanego użytkownika.
     * Token został zapisany podczas logowania i jest wymagany
     * przez backend przy pobieraniu oraz dodawaniu danych.
     */
    private val token: String?
        get() = MainActivity.authToken

    /**
     * Metoda wywoływana podczas tworzenia ekranu historii serwisowej.
     * Inicjalizuje layout, przyciski, obsługę pasków systemowych
     * oraz pobiera istniejące wpisy serwisowe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Podpięcie pliku XML odpowiadającego za wygląd widoku serwisów.
        setContentView(R.layout.activity_services)

        /*
         * Pobranie głównego kontenera widoku.
         * Kontener będzie odsunięty od pasków systemowych telefonu,
         * aby elementy interfejsu nie nachodziły na górny pasek statusu
         * ani dolny pasek nawigacyjny.
         */
        val servicesRoot = findViewById<View>(R.id.servicesRoot)

        // Zapamiętanie początkowych odstępów ustawionych w pliku XML.
        val originalLeftPadding = servicesRoot.paddingLeft
        val originalTopPadding = servicesRoot.paddingTop
        val originalRightPadding = servicesRoot.paddingRight
        val originalBottomPadding = servicesRoot.paddingBottom

        /*
         * Pobranie rzeczywistej wielkości pasków systemowych urządzenia
         * i dodanie jej do odstępów głównego kontenera.
         */
        ViewCompat.setOnApplyWindowInsetsListener(servicesRoot) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                originalLeftPadding + systemBars.left,
                originalTopPadding + systemBars.top,
                originalRightPadding + systemBars.right,
                originalBottomPadding + systemBars.bottom
            )

            windowInsets
        }

        // Wymuszenie obliczenia bezpiecznych odstępów po uruchomieniu widoku.
        ViewCompat.requestApplyInsets(servicesRoot)

        // Pobranie listy, na której będą wyświetlane wpisy serwisowe.
        lvServices = findViewById(R.id.lvServices)

        // Znajdujemy przycisk powrotu i ustawiamy jego działanie.
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            /*
             * Zamknięcie obecnej aktywności automatycznie powoduje
             * powrót do poprzedniego ekranu, czyli widoku pojazdów.
             */
            finish()
        }

        // Przycisk otwierający formularz dodawania nowego wpisu serwisowego.
        val btnAddService = findViewById<Button>(R.id.btnAddService)
        btnAddService.setOnClickListener {
            /*
             * Przed pokazaniem formularza należy pobrać pojazdy użytkownika
             * oraz dostępne rodzaje serwisu, ponieważ są wybierane w formularzu.
             */
            loadFormDataAndShowDialog()
        }

        /*
         * Dostęp do historii serwisowej wymaga zalogowanego użytkownika.
         * Jeżeli token nie istnieje, aplikacja nie może pobrać prywatnych danych.
         */
        if (token == null) {
            Toast.makeText(
                this,
                "Brak tokenu!",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // Przy otwarciu widoku pobieramy istniejącą historię serwisową.
        loadServices()
    }

    /**
     * Pobiera wszystkie wpisy serwisowe przypisane do zalogowanego użytkownika.
     * Do żądania dołączany jest token JWT w nagłówku Authorization.
     */
    private fun loadServices() {
        val currentToken = token ?: return

        ApiClient.authService.getServices("Bearer $currentToken")
            .enqueue(object : Callback<List<ServiceRecord>> {

                /**
                 * Metoda wykonywana po otrzymaniu odpowiedzi backendu.
                 * Jeżeli odpowiedź jest poprawna, wpisy zostają wyświetlone na liście.
                 */
                override fun onResponse(
                    call: Call<List<ServiceRecord>>,
                    response: Response<List<ServiceRecord>>
                ) {
                    if (response.isSuccessful) {
                        allServices = response.body() ?: emptyList()

                        /*
                         * Utworzenie adaptera, który zamienia obiekty ServiceRecord
                         * na czytelne karty historii serwisowej w interfejsie.
                         */
                        lvServices.adapter =
                            ServiceAdapter(this@ServicesActivity, allServices)
                    } else {
                        Toast.makeText(
                            this@ServicesActivity,
                            "Błąd pobierania danych. Kod: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                /**
                 * Obsługa sytuacji, gdy nie udało się połączyć z backendem,
                 * na przykład z powodu wyłączonego serwera lub problemu z siecią.
                 */
                override fun onFailure(
                    call: Call<List<ServiceRecord>>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@ServicesActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Rozpoczyna przygotowanie formularza nowego wpisu serwisowego.
     *
     * Najpierw pobierana jest lista samochodów użytkownika,
     * ponieważ każdy wpis serwisowy musi być przypisany do pojazdu.
     */
    private fun loadFormDataAndShowDialog() {
        val currentToken = token ?: return

        ApiClient.authService.getCars("Bearer $currentToken")
            .enqueue(object : Callback<List<Car>> {

                /**
                 * Po poprawnym pobraniu pojazdów następuje pobranie
                 * dostępnych rodzajów serwisu.
                 */
                override fun onResponse(
                    call: Call<List<Car>>,
                    response: Response<List<Car>>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@ServicesActivity,
                            "Nie udało się pobrać pojazdów.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val cars = response.body() ?: emptyList()

                    /*
                     * Nie można utworzyć wpisu serwisowego bez samochodu.
                     * W takim przypadku użytkownik powinien najpierw dodać pojazd.
                     */
                    if (cars.isEmpty()) {
                        Toast.makeText(
                            this@ServicesActivity,
                            "Najpierw dodaj przynajmniej jeden pojazd.",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    loadServiceTypesAndShowDialog(cars)
                }

                // Obsługa błędu połączenia podczas pobierania samochodów.
                override fun onFailure(
                    call: Call<List<Car>>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@ServicesActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Pobiera rodzaje serwisu dostępne w systemie.
     *
     * @param cars lista samochodów pobrana wcześniej z backendu.
     */
    private fun loadServiceTypesAndShowDialog(cars: List<Car>) {
        val currentToken = token ?: return

        ApiClient.authService.getServiceTypes("Bearer $currentToken")
            .enqueue(object : Callback<List<ServiceTypeOption>> {

                /**
                 * Po pobraniu typów serwisu otwierany jest właściwy formularz.
                 */
                override fun onResponse(
                    call: Call<List<ServiceTypeOption>>,
                    response: Response<List<ServiceTypeOption>>
                ) {
                    if (response.isSuccessful) {
                        val serviceTypes = response.body() ?: emptyList()
                        showAddServiceDialog(cars, serviceTypes)
                    } else {
                        Toast.makeText(
                            this@ServicesActivity,
                            "Nie udało się pobrać rodzajów serwisu.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Obsługa błędu połączenia podczas pobierania rodzajów serwisu.
                override fun onFailure(
                    call: Call<List<ServiceTypeOption>>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@ServicesActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Wyświetla formularz dodawania nowego wpisu serwisowego.
     *
     * Formularz pozwala wskazać pojazd i rodzaj serwisu,
     * wprowadzić podstawowe informacje o usłudze oraz dynamicznie
     * dodać wykonane czynności i wykorzystane części.
     *
     * @param cars pojazdy użytkownika możliwe do wybrania w formularzu,
     * @param serviceTypes dostępne kategorie serwisów.
     */
    private fun showAddServiceDialog(
        cars: List<Car>,
        serviceTypes: List<ServiceTypeOption>
    ) {
        // Załadowanie layoutu formularza dodawania nowego serwisu.
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_service, null)

        // Pola wyboru samochodu, rodzaju serwisu oraz statusu.
        val spServiceCar = dialogView.findViewById<Spinner>(R.id.spServiceCar)
        val spServiceType = dialogView.findViewById<Spinner>(R.id.spServiceType)
        val spServiceStatus = dialogView.findViewById<Spinner>(R.id.spServiceStatus)

        // Podstawowe pola tekstowe formularza.
        val etServiceDate = dialogView.findViewById<EditText>(R.id.etServiceDate)
        val etServiceWorkshop = dialogView.findViewById<EditText>(R.id.etServiceWorkshop)
        val etServiceAddress = dialogView.findViewById<EditText>(R.id.etServiceAddress)
        val etServiceMileage = dialogView.findViewById<EditText>(R.id.etServiceMileage)
        val etServiceDescription = dialogView.findViewById<EditText>(R.id.etServiceDescription)

        /*
         * Kontenery, do których dynamicznie będą dodawane pola
         * opisujące czynności serwisowe oraz użyte części.
         */
        val taskContainer = dialogView.findViewById<LinearLayout>(R.id.taskContainer)
        val partContainer = dialogView.findViewById<LinearLayout>(R.id.partContainer)

        // Przyciski pozwalające dodać kolejne czynności lub części.
        val btnAddTask = dialogView.findViewById<Button>(R.id.btnAddTask)
        val btnAddPart = dialogView.findViewById<Button>(R.id.btnAddPart)

        /*
         * Listy przechowują odwołania do pól dodanych dynamicznie.
         * Na ich podstawie podczas zapisu zbierane są wszystkie
         * wprowadzone przez użytkownika czynności oraz części.
         */
        val taskRows = mutableListOf<TaskRowViews>()
        val partRows = mutableListOf<PartRowViews>()

        /*
         * Przygotowanie opisów samochodów widocznych w rozwijanej liście.
         * Użytkownik wybiera samochód na podstawie marki, modelu
         * oraz numeru rejestracyjnego.
         */
        val carLabels = cars.map { car ->
            "${car.marka ?: ""} ${car.model ?: ""} (${car.numer_rejestracyjny ?: "-"})"
        }

        spServiceCar.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            carLabels
        )

        /*
         * Przygotowanie listy rodzajów serwisu.
         * Pierwszy element oznacza, że użytkownik nie wybrał
         * konkretnej kategorii serwisu.
         */
        val serviceTypeLabels = mutableListOf("-- wybierz rodzaj serwisu --")
        serviceTypeLabels.addAll(serviceTypes.map { it.nazwa ?: "-" })

        spServiceType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            serviceTypeLabels
        )

        /*
         * Statusy widoczne dla użytkownika oraz odpowiadające im wartości,
         * które zostaną przesłane do backendu.
         */
        val statusLabels = listOf("Zakończony", "W toku", "Anulowany")
        val statusValues = listOf("zakonczony", "w_toku", "anulowany")

        spServiceStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusLabels
        )

        // Domyślne ustawienie bieżącej daty w polu daty serwisu.
        etServiceDate.setText(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )

        /*
         * Formularz rozpoczyna się od jednego pustego wiersza czynności.
         * Wiersze części są opcjonalne i pojawiają się dopiero
         * po kliknięciu przycisku dodawania części.
         */
        addTaskRow(taskContainer, taskRows)

        // Dodawanie kolejnego pola opisującego wykonaną czynność.
        btnAddTask.setOnClickListener {
            addTaskRow(taskContainer, taskRows)
        }

        // Dodawanie pola opisującego część wykorzystaną podczas serwisu.
        btnAddPart.setOnClickListener {
            addPartRow(partContainer, partRows)
        }

        /*
         * Utworzenie okna dialogowego zawierającego formularz.
         * Obsługa pozytywnego przycisku jest ustawiana ręcznie,
         * aby przy błędnych danych formularz nie został zamknięty.
         */
        val dialog = AlertDialog.Builder(this)
            .setTitle("Dodaj wpis serwisowy")
            .setView(dialogView)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Zapisz serwis", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                // Pobranie samochodu wybranego w formularzu.
                val selectedCar = cars[spServiceCar.selectedItemPosition]

                /*
                 * Pobranie identyfikatora wybranego rodzaju serwisu.
                 * Jeżeli użytkownik pozostawił pierwszą opcję,
                 * typ serwisu zostanie przesłany jako wartość null.
                 */
                val selectedTypeId = if (spServiceType.selectedItemPosition == 0) {
                    null
                } else {
                    serviceTypes[spServiceType.selectedItemPosition - 1].id
                }

                // Pobranie podstawowych danych wpisu serwisowego.
                val date = etServiceDate.text.toString().trim()
                val workshop = etServiceWorkshop.text.toString().trim()
                val address = etServiceAddress.text.toString().trim()
                val mileageText = etServiceMileage.text.toString().trim()
                val description = etServiceDescription.text.toString().trim()

                // Data serwisu jest polem wymaganym.
                if (date.isBlank()) {
                    etServiceDate.error = "Podaj datę serwisu"
                    return@setOnClickListener
                }

                // Sprawdzenie poprawności formatu i wartości daty.
                if (!isValidDate(date)) {
                    etServiceDate.error = "Data musi mieć format RRRR-MM-DD"
                    return@setOnClickListener
                }

                /*
                 * Przebieg jest polem opcjonalnym.
                 * Jeżeli został podany, musi być poprawną liczbą całkowitą.
                 */
                val mileage = mileageText
                    .takeIf { it.isNotBlank() }
                    ?.toIntOrNull()

                if (mileageText.isNotBlank() && mileage == null) {
                    etServiceMileage.error = "Przebieg musi być liczbą"
                    return@setOnClickListener
                }

                if (mileage != null && mileage < 0) {
                    etServiceMileage.error = "Przebieg nie może być ujemny"
                    return@setOnClickListener
                }

                /*
                 * Zebranie wykonanych czynności z dynamicznie utworzonych pól.
                 * Puste wiersze nie są przesyłane do backendu.
                 */
                val tasks = mutableListOf<ServiceTaskRequest>()

                for (row in taskRows) {
                    val taskName = row.name.text.toString().trim()
                    val taskDescription = row.description.text.toString().trim()
                    val taskCostText = row.cost.text.toString().trim()

                    // Całkowicie pusty wiersz czynności zostaje pominięty.
                    if (taskName.isBlank() &&
                        taskDescription.isBlank() &&
                        taskCostText.isBlank()
                    ) {
                        continue
                    }

                    // Jeżeli użytkownik rozpoczął uzupełnianie czynności, nazwa jest wymagana.
                    if (taskName.isBlank()) {
                        row.name.error = "Podaj nazwę czynności"
                        return@setOnClickListener
                    }

                    /*
                     * Koszt może zostać wpisany z przecinkiem lub kropką.
                     * Przed konwersją przecinek jest zamieniany na kropkę.
                     */
                    val taskCost = taskCostText
                        .takeIf { it.isNotBlank() }
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()

                    if (taskCostText.isNotBlank() && taskCost == null) {
                        row.cost.error = "Koszt musi być liczbą"
                        return@setOnClickListener
                    }

                    if (taskCost != null && taskCost < 0) {
                        row.cost.error = "Koszt nie może być ujemny"
                        return@setOnClickListener
                    }

                    // Utworzenie obiektu pojedynczej czynności wysyłanej do backendu.
                    tasks.add(
                        ServiceTaskRequest(
                            nazwa_zadania = taskName,
                            opis = taskDescription.ifBlank { null },
                            koszt_robocizny = taskCost ?: 0.0
                        )
                    )
                }

                /*
                 * Zebranie użytych części z dynamicznych pól formularza.
                 * Dodawanie części jest opcjonalne.
                 */
                val parts = mutableListOf<UsedPartRequest>()

                for (row in partRows) {
                    val partName = row.name.text.toString().trim()
                    val manufacturer = row.manufacturer.text.toString().trim()
                    val quantityText = row.quantity.text.toString().trim()
                    val priceText = row.price.text.toString().trim()

                    // Całkowicie pusty wiersz części zostaje pominięty.
                    if (partName.isBlank() &&
                        manufacturer.isBlank() &&
                        quantityText.isBlank() &&
                        priceText.isBlank()
                    ) {
                        continue
                    }

                    // Dla rozpoczętego wpisu części jej nazwa jest wymagana.
                    if (partName.isBlank()) {
                        row.name.error = "Podaj nazwę części"
                        return@setOnClickListener
                    }

                    // Konwersja ilości i ceny, z obsługą przecinka dziesiętnego.
                    val quantity = quantityText
                        .takeIf { it.isNotBlank() }
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()

                    val price = priceText
                        .takeIf { it.isNotBlank() }
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()

                    if (quantityText.isNotBlank() && quantity == null) {
                        row.quantity.error = "Ilość musi być liczbą"
                        return@setOnClickListener
                    }

                    if (priceText.isNotBlank() && price == null) {
                        row.price.error = "Cena musi być liczbą"
                        return@setOnClickListener
                    }

                    if (quantity != null && quantity <= 0) {
                        row.quantity.error = "Ilość musi być większa od zera"
                        return@setOnClickListener
                    }

                    if (price != null && price < 0) {
                        row.price.error = "Cena nie może być ujemna"
                        return@setOnClickListener
                    }

                    // Utworzenie obiektu opisującego część wysyłaną do backendu.
                    parts.add(
                        UsedPartRequest(
                            nazwa_czesci = partName,
                            producent_czesci = manufacturer.ifBlank { null },
                            ilosc = quantity ?: 1.0,
                            cena_jednostkowa = price ?: 0.0
                        )
                    )
                }

                /*
                 * Przygotowanie kompletnego obiektu nowego wpisu serwisowego.
                 * Obiekt zawiera podstawowe dane, czynności oraz użyte części.
                 */
                val request = ServiceRequest(
                    samochod_id = selectedCar.id,
                    rodzaj_serwisu_id = selectedTypeId,
                    data_serwisu = date,
                    nazwa_warsztatu = workshop.ifBlank { null },
                    adres_warsztatu = address.ifBlank { null },
                    przebieg_przy_serwisie = mileage,
                    opis = description.ifBlank { null },
                    status = statusValues[spServiceStatus.selectedItemPosition],
                    zadania = tasks,
                    uzyte_czesci = parts
                )

                // Przesłanie przygotowanego wpisu serwisowego do backendu.
                createService(request, dialog)
            }
        }

        // Wyświetlenie formularza na ekranie.
        dialog.show()

        /*
         * Ustawienie jednolitego ciemnego tła dialogu.
         * Dotyczy to również jego nagłówka oraz obszaru przycisków,
         * dzięki czemu nie są one przezroczyste.
         */
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                android.graphics.Color.parseColor("#1E1E24")
            )
        )

        // Dostosowanie kolorów przycisków do ciemnej stylistyki aplikacji.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(android.graphics.Color.parseColor("#B69CFF"))

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(android.graphics.Color.parseColor("#B69CFF"))
    }

    /**
     * Dodaje do formularza pojedynczy wiersz czynności serwisowej.
     *
     * Każda czynność posiada:
     * - nazwę,
     * - opcjonalny opis,
     * - koszt robocizny.
     *
     * @param container kontener XML, do którego zostanie dodany nowy wiersz,
     * @param rows lista odwołań do dynamicznie utworzonych pól czynności.
     */
    private fun addTaskRow(
        container: LinearLayout,
        rows: MutableList<TaskRowViews>
    ) {
        // Utworzenie pionowego kontenera dla jednej czynności.
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(12))
        }

        // Nagłówek określający numer kolejnej czynności.
        val tvHeader = TextView(this).apply {
            text = "Czynność ${rows.size + 1}"
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        // Pole nazwy wykonanej czynności.
        val etName = EditText(this).apply {
            hint = "Nazwa czynności, np. wymiana oleju"
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#8D8D94"))
        }

        // Pole dodatkowego opisu czynności.
        val etDescription = EditText(this).apply {
            hint = "Opis czynności"
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#8D8D94"))
        }

        // Pole kosztu robocizny związanego z daną czynnością.
        val etCost = EditText(this).apply {
            hint = "Koszt robocizny [zł]"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#8D8D94"))
        }

        // Przycisk umożliwiający usunięcie wybranej czynności.
        val btnRemove = Button(this).apply {
            text = "Usuń czynność"
            isAllCaps = false
        }

        /*
         * Zapisanie odwołań do pól w obiekcie pomocniczym.
         * Dzięki temu dane będzie można później odczytać przy zapisie formularza.
         */
        val rowViews = TaskRowViews(
            layout = rowLayout,
            name = etName,
            description = etDescription,
            cost = etCost
        )

        btnRemove.setOnClickListener {
            /*
             * Formularz zachowuje zawsze przynajmniej jeden wiersz czynności.
             * Jeżeli jest to jedyny wiersz, jego pola są tylko czyszczone.
             */
            if (rows.size > 1) {
                container.removeView(rowLayout)
                rows.remove(rowViews)
                refreshTaskHeaders(rows)
            } else {
                etName.text.clear()
                etDescription.text.clear()
                etCost.text.clear()
            }
        }

        // Dodanie elementów pojedynczej czynności do jej kontenera.
        rowLayout.addView(tvHeader)
        rowLayout.addView(etName)
        rowLayout.addView(etDescription)
        rowLayout.addView(etCost)
        rowLayout.addView(btnRemove)

        // Dodanie całego wiersza czynności do formularza.
        container.addView(rowLayout)

        // Zapisanie utworzonych pól w liście wykorzystywanej podczas zapisu.
        rows.add(rowViews)
    }

    /**
     * Aktualizuje numery czynności po usunięciu któregoś wiersza.
     *
     * @param rows aktualna lista widocznych czynności.
     */
    private fun refreshTaskHeaders(rows: List<TaskRowViews>) {
        rows.forEachIndexed { index, row ->
            val header = row.layout.getChildAt(0) as TextView
            header.text = "Czynność ${index + 1}"
        }
    }

    /**
     * Dodaje do formularza pojedynczy wiersz użytej części.
     *
     * Każda część posiada:
     * - nazwę,
     * - opcjonalnego producenta,
     * - ilość,
     * - cenę jednostkową.
     *
     * @param container kontener, w którym zostanie umieszczony nowy wiersz,
     * @param rows lista odwołań do utworzonych pól części.
     */
    private fun addPartRow(
        container: LinearLayout,
        rows: MutableList<PartRowViews>
    ) {
        // Utworzenie pionowego kontenera dla jednej części.
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(12))
        }

        // Nagłówek określający numer kolejnej części.
        val tvHeader = TextView(this).apply {
            text = "Część ${rows.size + 1}"
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        // Pole nazwy użytej części.
        val etName = EditText(this).apply {
            hint = "Nazwa części, np. olej 5W30"
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#8D8D94"))
        }

        // Pole producenta części.
        val etManufacturer = EditText(this).apply {
            hint = "Producent"
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#8D8D94"))
        }

        // Pole określające liczbę wykorzystanych sztuk lub jednostek części.
        val etQuantity = EditText(this).apply {
            hint = "Ilość"
            setText("1")
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#8D8D94"))
        }

        // Pole ceny pojedynczej sztuki lub jednostki części.
        val etPrice = EditText(this).apply {
            hint = "Cena jednostkowa [zł]"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#8D8D94"))
        }

        // Przycisk usuwający wybraną część z formularza.
        val btnRemove = Button(this).apply {
            text = "Usuń część"
            isAllCaps = false
        }

        // Zapisanie odwołań do pól jednej części w obiekcie pomocniczym.
        val rowViews = PartRowViews(
            layout = rowLayout,
            name = etName,
            manufacturer = etManufacturer,
            quantity = etQuantity,
            price = etPrice
        )

        btnRemove.setOnClickListener {
            /*
             * Części są opcjonalne, dlatego każdy wiersz części
             * można całkowicie usunąć z formularza.
             */
            container.removeView(rowLayout)
            rows.remove(rowViews)
            refreshPartHeaders(rows)
        }

        // Dodanie elementów jednej części do jej kontenera.
        rowLayout.addView(tvHeader)
        rowLayout.addView(etName)
        rowLayout.addView(etManufacturer)
        rowLayout.addView(etQuantity)
        rowLayout.addView(etPrice)
        rowLayout.addView(btnRemove)

        // Dodanie przygotowanego wiersza do sekcji części w formularzu.
        container.addView(rowLayout)

        // Zachowanie pól w liście, aby można było odczytać ich dane przy zapisie.
        rows.add(rowViews)
    }

    /**
     * Aktualizuje numerację części po usunięciu wybranego wiersza.
     *
     * @param rows aktualna lista części znajdujących się w formularzu.
     */
    private fun refreshPartHeaders(rows: List<PartRowViews>) {
        rows.forEachIndexed { index, row ->
            val header = row.layout.getChildAt(0) as TextView
            header.text = "Część ${index + 1}"
        }
    }

    /**
     * Wysyła nowy wpis serwisowy do backendu.
     *
     * Po poprawnym zapisaniu wpisu okno formularza zostaje zamknięte,
     * a historia serwisowa jest pobierana ponownie, aby natychmiast
     * wyświetlić nowo dodany serwis.
     *
     * @param request komplet danych nowego wpisu serwisowego,
     * @param dialog formularz, który zostanie zamknięty po poprawnym zapisie.
     */
    private fun createService(
        request: ServiceRequest,
        dialog: AlertDialog
    ) {
        val currentToken = token ?: return

        ApiClient.authService.createService(
            "Bearer $currentToken",
            request
        ).enqueue(object : Callback<ResponseBody> {

            /**
             * Obsługa odpowiedzi backendu po próbie zapisania serwisu.
             */
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ServicesActivity,
                        "Wpis serwisowy został dodany",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    /*
                     * Po zapisaniu pobieramy listę ponownie,
                     * aby nowy wpis był od razu widoczny na ekranie.
                     */
                    loadServices()
                } else {
                    /*
                     * Przygotowanie czytelnego komunikatu zależnego
                     * od kodu błędu zwróconego przez backend.
                     */
                    val message = when (response.code()) {
                        400 -> "Uzupełnij wymagane pola lub sprawdź poprawność danych."
                        401 -> "Sesja wygasła. Zaloguj się ponownie."
                        403 -> "Wybrany pojazd nie należy do użytkownika."
                        else -> "Nie udało się dodać wpisu. Kod: ${response.code()}"
                    }

                    Toast.makeText(
                        this@ServicesActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Obsługa problemu z połączeniem podczas zapisu wpisu.
            override fun onFailure(
                call: Call<ResponseBody>,
                t: Throwable
            ) {
                Toast.makeText(
                    this@ServicesActivity,
                    "Błąd sieci: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /**
     * Sprawdza, czy podany tekst przedstawia poprawną datę
     * w formacie wymaganym przez backend: RRRR-MM-DD.
     *
     * @param value data wpisana przez użytkownika,
     * @return true, jeżeli data jest poprawna; false w przeciwnym przypadku.
     */
    private fun isValidDate(value: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            /*
             * Wyłączenie trybu tolerancyjnego powoduje, że daty takie jak
             * 2026-02-31 zostaną potraktowane jako niepoprawne.
             */
            formatter.isLenient = false
            formatter.parse(value)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Przelicza wartość odstępu podaną w dp na piksele urządzenia.
     * Jest wykorzystywana przy dynamicznym tworzeniu elementów formularza.
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * Klasa pomocnicza przechowująca pola pojedynczej czynności.
     * Umożliwia późniejsze odczytanie wartości wprowadzonych
     * do dynamicznie utworzonych elementów formularza.
     */
    private data class TaskRowViews(
        val layout: LinearLayout,
        val name: EditText,
        val description: EditText,
        val cost: EditText
    )

    /**
     * Klasa pomocnicza przechowująca pola pojedynczej części.
     * Umożliwia zebranie danych części podczas zapisu formularza.
     */
    private data class PartRowViews(
        val layout: LinearLayout,
        val name: EditText,
        val manufacturer: EditText,
        val quantity: EditText,
        val price: EditText
    )

    /**
     * Adapter odpowiedzialny za wyświetlanie kart istniejących wpisów serwisowych.
     */
    inner class ServiceAdapter(
        context: AppCompatActivity,
        private val services: List<ServiceRecord>
    ) : ArrayAdapter<ServiceRecord>(context, 0, services) {

        // Zwraca liczbę wpisów serwisowych znajdujących się na liście.
        override fun getCount(): Int = services.size

        // Zwraca wpis serwisowy znajdujący się pod wskazanym indeksem.
        override fun getItem(position: Int): ServiceRecord = services[position]

        /**
         * Przygotowuje wygląd pojedynczej karty historii serwisowej.
         *
         * @param position pozycja wpisu na liście,
         * @param convertView widok możliwy do ponownego wykorzystania,
         * @param parent nadrzędny kontener listy.
         */
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            /*
             * Jeśli istnieje już niewykorzystywany widok karty,
             * Android wykorzystuje go ponownie zamiast tworzyć nowy.
             */
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_service, parent, false)

            val service = getItem(position)

            // Pobranie elementów tekstowych pojedynczej karty serwisowej.
            val tvServiceDate = view.findViewById<TextView>(R.id.tvServiceDate)
            val tvServiceStatus = view.findViewById<TextView>(R.id.tvServiceStatus)
            val tvServiceCar = view.findViewById<TextView>(R.id.tvServiceCar)
            val tvServiceDetails = view.findViewById<TextView>(R.id.tvServiceDetails)

            /*
             * Pobranie daty serwisu.
             * Jeżeli backend zwrócił także godzinę, wyświetlana jest tylko
             * pierwsza część tekstu odpowiadająca formatowi RRRR-MM-DD.
             */
            val fullDate = service.data_serwisu ?: "Brak daty"
            tvServiceDate.text =
                if (fullDate.length > 10) fullDate.substring(0, 10) else fullDate

            /*
             * Zamiana technicznych wartości statusu zwracanych przez backend
             * na czytelne polskie opisy widoczne dla użytkownika.
             */
            tvServiceStatus.text = when (service.status) {
                "zakonczony" -> "Zakończony"
                "w_toku" -> "W toku"
                "anulowany" -> "Anulowany"
                else -> service.status ?: "Nieznany"
            }

            // Wyświetlenie pojazdu, którego dotyczy wybrany wpis serwisowy.
            val car = service.samochod
            tvServiceCar.text =
                "${car?.marka ?: ""} ${car?.model ?: ""} (${car?.numer_rejestracyjny ?: ""})"

            /*
             * Wyświetlenie dodatkowych danych serwisu:
             * rodzaju usługi, warsztatu oraz całkowitego kosztu.
             */
            tvServiceDetails.text = """
                Rodzaj: ${service.rodzaj_serwisu?.nazwa ?: "-"}
                Warsztat: ${service.nazwa_warsztatu ?: "-"}
                Koszt: ${String.format(Locale.getDefault(), "%.2f", service.koszt_calkowity ?: 0.0)} PLN
            """.trimIndent()

            return view
        }
    }
}