package com.example.mobile

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServicesActivity : AppCompatActivity() {

    private var allServices: List<ServiceRecord> = emptyList()

    private lateinit var lvServices: ListView

    private val token: String?
        get() = MainActivity.authToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        lvServices = findViewById(R.id.lvServices)

        // Znajdujemy przycisk i ustawiamy powrót.
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Zamyka ekran serwisów, automatycznie wracając do ekranu pojazdów.
            finish()
        }

        // Przycisk otwierający formularz dodawania nowego wpisu serwisowego.
        val btnAddService = findViewById<Button>(R.id.btnAddService)
        btnAddService.setOnClickListener {
            loadFormDataAndShowDialog()
        }

        if (token == null) {
            Toast.makeText(this, "Brak tokenu!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Przy otwarciu widoku pobieramy istniejącą historię serwisową.
        loadServices()
    }

    // Pobiera wszystkie wpisy serwisowe użytkownika i wyświetla je na liście.
    private fun loadServices() {
        val currentToken = token ?: return

        ApiClient.authService.getServices("Bearer $currentToken")
            .enqueue(object : Callback<List<ServiceRecord>> {

                override fun onResponse(
                    call: Call<List<ServiceRecord>>,
                    response: Response<List<ServiceRecord>>
                ) {
                    if (response.isSuccessful) {
                        allServices = response.body() ?: emptyList()
                        lvServices.adapter = ServiceAdapter(this@ServicesActivity, allServices)
                    } else {
                        Toast.makeText(
                            this@ServicesActivity,
                            "Błąd pobierania danych. Kod: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<ServiceRecord>>, t: Throwable) {
                    Toast.makeText(
                        this@ServicesActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /*
     * Przed otwarciem formularza pobieramy:
     * - pojazdy użytkownika, aby można było przypisać serwis do samochodu,
     * - rodzaje serwisu, aby można było wybrać właściwą kategorię wpisu.
     */
    private fun loadFormDataAndShowDialog() {
        val currentToken = token ?: return

        ApiClient.authService.getCars("Bearer $currentToken")
            .enqueue(object : Callback<List<Car>> {

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

                override fun onFailure(call: Call<List<Car>>, t: Throwable) {
                    Toast.makeText(
                        this@ServicesActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // Pobiera rodzaje serwisu i po ich otrzymaniu otwiera formularz dodawania.
    private fun loadServiceTypesAndShowDialog(cars: List<Car>) {
        val currentToken = token ?: return

        ApiClient.authService.getServiceTypes("Bearer $currentToken")
            .enqueue(object : Callback<List<ServiceTypeOption>> {

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

    // Otwiera formularz dodawania nowego wpisu serwisowego.
    private fun showAddServiceDialog(
        cars: List<Car>,
        serviceTypes: List<ServiceTypeOption>
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_service, null)

        val spServiceCar = dialogView.findViewById<Spinner>(R.id.spServiceCar)
        val spServiceType = dialogView.findViewById<Spinner>(R.id.spServiceType)
        val spServiceStatus = dialogView.findViewById<Spinner>(R.id.spServiceStatus)

        val etServiceDate = dialogView.findViewById<EditText>(R.id.etServiceDate)
        val etServiceWorkshop = dialogView.findViewById<EditText>(R.id.etServiceWorkshop)
        val etServiceAddress = dialogView.findViewById<EditText>(R.id.etServiceAddress)
        val etServiceMileage = dialogView.findViewById<EditText>(R.id.etServiceMileage)
        val etServiceDescription = dialogView.findViewById<EditText>(R.id.etServiceDescription)

        val taskContainer = dialogView.findViewById<LinearLayout>(R.id.taskContainer)
        val partContainer = dialogView.findViewById<LinearLayout>(R.id.partContainer)

        val btnAddTask = dialogView.findViewById<Button>(R.id.btnAddTask)
        val btnAddPart = dialogView.findViewById<Button>(R.id.btnAddPart)

        /*
         * Listy przechowują pola utworzonych dynamicznie czynności i części.
         * Dzięki temu użytkownik może dodać więcej niż jedną czynność
         * lub więcej niż jedną wymienioną część.
         */
        val taskRows = mutableListOf<TaskRowViews>()
        val partRows = mutableListOf<PartRowViews>()

        // Ustawienie listy pojazdów użytkownika.
        val carLabels = cars.map { car ->
            "${car.marka ?: ""} ${car.model ?: ""} (${car.numer_rejestracyjny ?: "-"})"
        }

        spServiceCar.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            carLabels
        )

        // Ustawienie listy rodzajów serwisu. Pierwsza opcja oznacza brak wyboru.
        val serviceTypeLabels = mutableListOf("-- wybierz rodzaj serwisu --")
        serviceTypeLabels.addAll(serviceTypes.map { it.nazwa ?: "-" })

        spServiceType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            serviceTypeLabels
        )

        // Statusy zgodne z wartościami wykorzystywanymi w aplikacji webowej.
        val statusLabels = listOf("Zakończony", "W toku", "Anulowany")
        val statusValues = listOf("zakonczony", "w_toku", "anulowany")

        spServiceStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusLabels
        )

        // Domyślna data serwisu to bieżący dzień.
        etServiceDate.setText(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )

        /*
         * Tak samo jak w aplikacji webowej, formularz na początku posiada
         * jedną pustą czynność. Części są opcjonalne i dodawane przyciskiem.
         */
        addTaskRow(taskContainer, taskRows)

        btnAddTask.setOnClickListener {
            addTaskRow(taskContainer, taskRows)
        }

        btnAddPart.setOnClickListener {
            addPartRow(partContainer, partRows)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Dodaj wpis serwisowy")
            .setView(dialogView)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Zapisz serwis", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val selectedCar = cars[spServiceCar.selectedItemPosition]

                val selectedTypeId = if (spServiceType.selectedItemPosition == 0) {
                    null
                } else {
                    serviceTypes[spServiceType.selectedItemPosition - 1].id
                }

                val date = etServiceDate.text.toString().trim()
                val workshop = etServiceWorkshop.text.toString().trim()
                val address = etServiceAddress.text.toString().trim()
                val mileageText = etServiceMileage.text.toString().trim()
                val description = etServiceDescription.text.toString().trim()

                if (date.isBlank()) {
                    etServiceDate.error = "Podaj datę serwisu"
                    return@setOnClickListener
                }

                if (!isValidDate(date)) {
                    etServiceDate.error = "Data musi mieć format RRRR-MM-DD"
                    return@setOnClickListener
                }

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

                // Zbieramy wprowadzone czynności. Puste wiersze są pomijane.
                val tasks = mutableListOf<ServiceTaskRequest>()

                for (row in taskRows) {
                    val taskName = row.name.text.toString().trim()
                    val taskDescription = row.description.text.toString().trim()
                    val taskCostText = row.cost.text.toString().trim()

                    if (taskName.isBlank() &&
                        taskDescription.isBlank() &&
                        taskCostText.isBlank()
                    ) {
                        continue
                    }

                    if (taskName.isBlank()) {
                        row.name.error = "Podaj nazwę czynności"
                        return@setOnClickListener
                    }

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

                    tasks.add(
                        ServiceTaskRequest(
                            nazwa_zadania = taskName,
                            opis = taskDescription.ifBlank { null },
                            koszt_robocizny = taskCost ?: 0.0
                        )
                    )
                }

                // Zbieramy użyte części. Puste wiersze są pomijane.
                val parts = mutableListOf<UsedPartRequest>()

                for (row in partRows) {
                    val partName = row.name.text.toString().trim()
                    val manufacturer = row.manufacturer.text.toString().trim()
                    val quantityText = row.quantity.text.toString().trim()
                    val priceText = row.price.text.toString().trim()

                    if (partName.isBlank() &&
                        manufacturer.isBlank() &&
                        quantityText.isBlank() &&
                        priceText.isBlank()
                    ) {
                        continue
                    }

                    if (partName.isBlank()) {
                        row.name.error = "Podaj nazwę części"
                        return@setOnClickListener
                    }

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

                    parts.add(
                        UsedPartRequest(
                            nazwa_czesci = partName,
                            producent_czesci = manufacturer.ifBlank { null },
                            ilosc = quantity ?: 1.0,
                            cena_jednostkowa = price ?: 0.0
                        )
                    )
                }

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

                createService(request, dialog)
            }
        }

        dialog.show()
    }

    /*
     * Dodaje pojedynczy wiersz czynności serwisowej.
     * Każda czynność posiada nazwę, opis oraz koszt robocizny.
     */
    private fun addTaskRow(
        container: LinearLayout,
        rows: MutableList<TaskRowViews>
    ) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(12))
        }

        val tvHeader = TextView(this).apply {
            text = "Czynność ${rows.size + 1}"
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val etName = EditText(this).apply {
            hint = "Nazwa czynności, np. wymiana oleju"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val etDescription = EditText(this).apply {
            hint = "Opis czynności"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val etCost = EditText(this).apply {
            hint = "Koszt robocizny [zł]"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val btnRemove = Button(this).apply {
            text = "Usuń czynność"
            isAllCaps = false
        }

        val rowViews = TaskRowViews(
            layout = rowLayout,
            name = etName,
            description = etDescription,
            cost = etCost
        )

        btnRemove.setOnClickListener {
            /*
             * Pozostawiamy przynajmniej jeden wiersz czynności,
             * tak samo jak formularz w aplikacji webowej.
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

        rowLayout.addView(tvHeader)
        rowLayout.addView(etName)
        rowLayout.addView(etDescription)
        rowLayout.addView(etCost)
        rowLayout.addView(btnRemove)

        container.addView(rowLayout)
        rows.add(rowViews)
    }

    // Aktualizuje numerację czynności po usunięciu jednego z wierszy.
    private fun refreshTaskHeaders(rows: List<TaskRowViews>) {
        rows.forEachIndexed { index, row ->
            val header = row.layout.getChildAt(0) as TextView
            header.text = "Czynność ${index + 1}"
        }
    }

    /*
     * Dodaje pojedynczy wiersz użytej części.
     * Części są opcjonalne i mogą zostać całkowicie usunięte z formularza.
     */
    private fun addPartRow(
        container: LinearLayout,
        rows: MutableList<PartRowViews>
    ) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(12))
        }

        val tvHeader = TextView(this).apply {
            text = "Część ${rows.size + 1}"
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val etName = EditText(this).apply {
            hint = "Nazwa części, np. olej 5W30"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val etManufacturer = EditText(this).apply {
            hint = "Producent"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val etQuantity = EditText(this).apply {
            hint = "Ilość"
            setText("1")
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val etPrice = EditText(this).apply {
            hint = "Cena jednostkowa [zł]"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val btnRemove = Button(this).apply {
            text = "Usuń część"
            isAllCaps = false
        }

        val rowViews = PartRowViews(
            layout = rowLayout,
            name = etName,
            manufacturer = etManufacturer,
            quantity = etQuantity,
            price = etPrice
        )

        btnRemove.setOnClickListener {
            container.removeView(rowLayout)
            rows.remove(rowViews)
            refreshPartHeaders(rows)
        }

        rowLayout.addView(tvHeader)
        rowLayout.addView(etName)
        rowLayout.addView(etManufacturer)
        rowLayout.addView(etQuantity)
        rowLayout.addView(etPrice)
        rowLayout.addView(btnRemove)

        container.addView(rowLayout)
        rows.add(rowViews)
    }

    // Aktualizuje numerację części po usunięciu jednego z wierszy.
    private fun refreshPartHeaders(rows: List<PartRowViews>) {
        rows.forEachIndexed { index, row ->
            val header = row.layout.getChildAt(0) as TextView
            header.text = "Część ${index + 1}"
        }
    }

    // Wysyła nowy wpis serwisowy do backendu.
    private fun createService(
        request: ServiceRequest,
        dialog: AlertDialog
    ) {
        val currentToken = token ?: return

        ApiClient.authService.createService(
            "Bearer $currentToken",
            request
        ).enqueue(object : Callback<ResponseBody> {

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

                    // Po zapisaniu pobieramy listę ponownie, aby nowy wpis był od razu widoczny.
                    loadServices()
                } else {
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

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@ServicesActivity,
                    "Błąd sieci: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Sprawdza, czy data ma format wymagany przez backend: RRRR-MM-DD.
    private fun isValidDate(value: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.isLenient = false
            formatter.parse(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Pomocnicza funkcja przeliczająca dp na piksele.
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // Przechowuje odwołania do pól jednego wiersza czynności.
    private data class TaskRowViews(
        val layout: LinearLayout,
        val name: EditText,
        val description: EditText,
        val cost: EditText
    )

    // Przechowuje odwołania do pól jednego wiersza części.
    private data class PartRowViews(
        val layout: LinearLayout,
        val name: EditText,
        val manufacturer: EditText,
        val quantity: EditText,
        val price: EditText
    )

    // Adapter odpowiedzialny za wyświetlanie kart istniejących wpisów serwisowych.
    inner class ServiceAdapter(
        context: AppCompatActivity,
        private val services: List<ServiceRecord>
    ) : ArrayAdapter<ServiceRecord>(context, 0, services) {

        override fun getCount(): Int = services.size

        override fun getItem(position: Int): ServiceRecord = services[position]

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_service, parent, false)

            val service = getItem(position)

            val tvServiceDate = view.findViewById<TextView>(R.id.tvServiceDate)
            val tvServiceStatus = view.findViewById<TextView>(R.id.tvServiceStatus)
            val tvServiceCar = view.findViewById<TextView>(R.id.tvServiceCar)
            val tvServiceDetails = view.findViewById<TextView>(R.id.tvServiceDetails)

            // Wyciągamy bezpiecznie datę, tylko część YYYY-MM-DD, jeśli tekst jest dłuższy.
            val fullDate = service.data_serwisu ?: "Brak daty"
            tvServiceDate.text =
                if (fullDate.length > 10) fullDate.substring(0, 10) else fullDate

            tvServiceStatus.text = when (service.status) {
                "zakonczony" -> "Zakończony"
                "w_toku" -> "W toku"
                "anulowany" -> "Anulowany"
                else -> service.status ?: "Nieznany"
            }

            val car = service.samochod
            tvServiceCar.text =
                "${car?.marka ?: ""} ${car?.model ?: ""} (${car?.numer_rejestracyjny ?: ""})"

            tvServiceDetails.text = """
                Rodzaj: ${service.rodzaj_serwisu?.nazwa ?: "-"}
                Warsztat: ${service.nazwa_warsztatu ?: "-"}
                Koszt: ${String.format(Locale.getDefault(), "%.2f", service.koszt_calkowity ?: 0.0)} PLN
            """.trimIndent()

            return view
        }
    }
}