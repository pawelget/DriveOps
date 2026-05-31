package com.example.mobile

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

/**
 * Aktywność odpowiedzialna za wyświetlanie i obsługę raportów serwisowych.
 *
 * Użytkownik może:
 * - pobrać listę wygenerowanych raportów,
 * - pobrać raport w formacie PDF,
 * - wysłać raport na swój adres e-mail,
 * - usunąć wybrany raport.
 */
class ReportsActivity : AppCompatActivity() {

    /**
     * Lista raportów wyświetlanych aktualnie na ekranie.
     *
     * Zastosowano MutableList, ponieważ po usunięciu raportu
     * można od razu usunąć jego kartę z listy bez ponownego
     * pobierania wszystkich danych z backendu.
     */
    private var reportsList: MutableList<ReportRecord> = mutableListOf()

    // Adapter odpowiedzialny za wyświetlanie kart raportów w elemencie ListView.
    private var adapter: ReportAdapter? = null

    /**
     * Token JWT aktualnie zalogowanego użytkownika.
     * Token jest wymagany przy pobieraniu oraz modyfikowaniu raportów.
     */
    private var token: String? = null

    /**
     * Metoda uruchamiana podczas tworzenia widoku raportów.
     * Inicjalizuje interfejs, obsługuje marginesy systemowe telefonu
     * oraz pobiera raporty użytkownika z backendu.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Podpięcie pliku XML definiującego wygląd widoku raportów.
        setContentView(R.layout.activity_reports)

        /*
         * Pobranie głównego kontenera ekranu.
         * Na jego podstawie ustawione zostaną bezpieczne odstępy
         * od górnego i dolnego interfejsu systemowego telefonu.
         */
        val reportsRoot = findViewById<View>(R.id.reportsRoot)

        // Zapamiętanie oryginalnych odstępów zdefiniowanych w pliku XML.
        val originalLeftPadding = reportsRoot.paddingLeft
        val originalTopPadding = reportsRoot.paddingTop
        val originalRightPadding = reportsRoot.paddingRight
        val originalBottomPadding = reportsRoot.paddingBottom

        /*
         * Obsługa obszaru zajmowanego przez pasek statusu oraz pasek nawigacyjny.
         * Dzięki temu nagłówek i przyciski nie nachodzą na elementy systemu Android.
         */
        ViewCompat.setOnApplyWindowInsetsListener(reportsRoot) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                originalLeftPadding + systemBars.left,
                originalTopPadding + systemBars.top,
                originalRightPadding + systemBars.right,
                originalBottomPadding + systemBars.bottom
            )

            windowInsets
        }

        // Wymuszenie zastosowania poprawnych odstępów po załadowaniu widoku.
        ViewCompat.requestApplyInsets(reportsRoot)

        // Pobranie listy raportów oraz przycisku powrotu z layoutu.
        val lvReports = findViewById<ListView>(R.id.lvReports)
        val btnBackFromReports = findViewById<Button>(R.id.btnBackFromReports)

        /*
         * Obsługa przycisku powrotu.
         * Zamknięcie aktywności powoduje powrót do poprzedniego ekranu.
         */
        btnBackFromReports.setOnClickListener {
            finish()
        }

        /*
         * Pobranie tokenu użytkownika zapisanego po poprawnym logowaniu.
         * Bez tokenu backend nie pozwoli na dostęp do prywatnych raportów.
         */
        token = MainActivity.authToken

        if (token == null) {
            Toast.makeText(
                this,
                "Brak tokenu!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Pobranie raportów bezpośrednio po otwarciu ekranu.
        fetchReports(lvReports)
    }

    /**
     * Pobiera raporty zalogowanego użytkownika z backendu.
     *
     * @param listView lista, w której mają zostać wyświetlone pobrane raporty.
     */
    private fun fetchReports(listView: ListView) {

        /*
         * Wysłanie żądania GET do backendu.
         * Token JWT jest przesyłany w nagłówku Authorization
         * w formacie wymaganym przez backend: Bearer <token>.
         */
        ApiClient.authService.getReports("Bearer $token")
            .enqueue(object : Callback<List<ReportRecord>> {

                /**
                 * Obsługa odpowiedzi otrzymanej z backendu.
                 * Przy poprawnej odpowiedzi lista raportów zostaje
                 * przekazana do adaptera i wyświetlona użytkownikowi.
                 */
                override fun onResponse(
                    call: Call<List<ReportRecord>>,
                    response: Response<List<ReportRecord>>
                ) {
                    if (response.isSuccessful) {
                        reportsList = response.body()?.toMutableList() ?: mutableListOf()

                        adapter = ReportAdapter(
                            this@ReportsActivity,
                            reportsList
                        )

                        listView.adapter = adapter
                    } else {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Błąd pobierania raportów",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                /**
                 * Obsługa błędu połączenia z backendem.
                 * Taka sytuacja może wystąpić na przykład przy wyłączonym
                 * backendzie albo braku połączenia telefonu z komputerem.
                 */
                override fun onFailure(
                    call: Call<List<ReportRecord>>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Adapter odpowiedzialny za utworzenie kart raportów
     * oraz przypisanie akcji do przycisków znajdujących się na każdej karcie.
     */
    inner class ReportAdapter(
        context: AppCompatActivity,
        private val reports: MutableList<ReportRecord>
    ) : ArrayAdapter<ReportRecord>(context, 0, reports) {

        /**
         * Przygotowuje wygląd pojedynczego raportu na liście.
         *
         * @param position pozycja raportu na liście,
         * @param convertView wcześniej utworzony widok możliwy do ponownego użycia,
         * @param parent kontener, w którym wyświetlana jest karta.
         */
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            /*
             * Jeżeli Android przekazał istniejący widok, wykorzystujemy go ponownie.
             * W przeciwnym przypadku tworzymy nową kartę na podstawie item_report.xml.
             */
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_report, parent, false)

            // Pobranie raportu odpowiadającego aktualnej pozycji listy.
            val report = getItem(position) ?: return view

            // Pobranie pól tekstowych oraz przycisków z layoutu pojedynczej karty.
            val tvReportNumber = view.findViewById<TextView>(R.id.tvReportNumber)
            val tvReportCar = view.findViewById<TextView>(R.id.tvReportCar)
            val tvReportDetails = view.findViewById<TextView>(R.id.tvReportDetails)

            val btnDownloadPdf = view.findViewById<Button>(R.id.btnDownloadPdf)
            val btnDeleteReport = view.findViewById<Button>(R.id.btnDeleteReport)
            val btnSendEmail = view.findViewById<Button>(R.id.btnSendEmail)

            // Wyświetlenie numeru raportu lub komunikatu zastępczego.
            tvReportNumber.text = report.numer_raportu ?: "Nieznany raport"

            /*
             * Wyświetlenie danych pojazdu powiązanego z raportem:
             * marki, modelu oraz numeru rejestracyjnego.
             */
            val car = report.samochod
            tvReportCar.text =
                "${car?.marka ?: ""} ${car?.model ?: ""} (${car?.numer_rejestracyjny ?: ""})"

            /*
             * Pobranie kosztu z wpisu serwisowego powiązanego z raportem
             * oraz skrócenie daty wygenerowania do formatu RRRR-MM-DD.
             */
            val koszt = report.wpis_serwisowy?.koszt_calkowity ?: 0.0
            val dataWyg = report.wygenerowano_w?.take(10) ?: "-"

            // Wyświetlenie podstawowych szczegółów raportu.
            tvReportDetails.text =
                "Wygenerowano: $dataWyg\nKoszt naprawy: $koszt PLN"

            /*
             * Akcja wysłania raportu na e-mail użytkownika.
             * Po kliknięciu przycisku do backendu wysyłane jest żądanie
             * dotyczące konkretnego raportu.
             */
            btnSendEmail.setOnClickListener {
                Toast.makeText(
                    this@ReportsActivity,
                    "Zlecono wysyłkę...",
                    Toast.LENGTH_SHORT
                ).show()

                ApiClient.authService.sendReportEmail(
                    "Bearer $token",
                    report.id,
                    EmailRequest()
                ).enqueue(object : Callback<EmailResponse> {

                    /**
                     * Obsługa odpowiedzi backendu po próbie wysłania wiadomości.
                     */
                    override fun onResponse(
                        call: Call<EmailResponse>,
                        response: Response<EmailResponse>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@ReportsActivity,
                                "Wysłano pomyślnie na Twój e-mail!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ReportsActivity,
                                "Błąd: Serwer odrzucił wysyłkę",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    /**
                     * Obsługa sytuacji, gdy nie udało się połączyć z backendem.
                     */
                    override fun onFailure(
                        call: Call<EmailResponse>,
                        t: Throwable
                    ) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Błąd sieci: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            /*
             * Akcja usunięcia raportu.
             * Po poprawnej odpowiedzi backendu raport jest usuwany
             * również z lokalnej listy, dzięki czemu znika z ekranu od razu.
             */
            btnDeleteReport.setOnClickListener {
                ApiClient.authService.deleteReport(
                    "Bearer $token",
                    report.id
                ).enqueue(object : Callback<Void> {

                    override fun onResponse(
                        call: Call<Void>,
                        response: Response<Void>
                    ) {
                        if (response.isSuccessful) {
                            // Usunięcie raportu z listy przechowywanej w pamięci.
                            reports.remove(report)

                            // Odświeżenie wyświetlanej listy kart.
                            notifyDataSetChanged()

                            Toast.makeText(
                                this@ReportsActivity,
                                "Usunięto raport",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ReportsActivity,
                                "Nie udało się usunąć raportu",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Błąd sieci: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            /*
             * Akcja pobrania pliku PDF.
             * Backend zwraca zawartość dokumentu jako ResponseBody,
             * a aplikacja zapisuje go następnie w folderze Pobrane telefonu.
             */
            btnDownloadPdf.setOnClickListener {
                Toast.makeText(
                    this@ReportsActivity,
                    "Rozpoczynam pobieranie...",
                    Toast.LENGTH_SHORT
                ).show()

                ApiClient.authService.downloadReportPdf(
                    "Bearer $token",
                    report.id
                ).enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful && response.body() != null) {

                            /*
                             * Ukośniki z numeru raportu są zamieniane na podkreślenia,
                             * ponieważ nie mogą występować w nazwie zapisywanego pliku.
                             */
                            val safeReportNumber =
                                report.numer_raportu?.replace("/", "_") ?: report.id.toString()

                            savePdfToDisk(
                                response.body()!!,
                                "raport_$safeReportNumber.pdf"
                            )
                        } else {
                            Toast.makeText(
                                this@ReportsActivity,
                                "Błąd pliku",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseBody>,
                        t: Throwable
                    ) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Błąd sieci: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            return view
        }

        /**
         * Zapisuje pobrany raport PDF w publicznym folderze Pobrane urządzenia.
         *
         * Dla Androida 10 i nowszych wykorzystywany jest MediaStore,
         * ponieważ współczesne wersje systemu ograniczają bezpośredni
         * dostęp aplikacji do wspólnej pamięci urządzenia.
         *
         * Dla starszych wersji Androida plik jest zapisywany
         * bezpośrednio w katalogu Download.
         *
         * @param body zawartość pliku PDF otrzymana z backendu,
         * @param fileName nazwa pliku zapisywanego na urządzeniu.
         */
        private fun savePdfToDisk(body: ResponseBody, fileName: String) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    /*
                     * Android 10 lub nowszy:
                     * przygotowanie metadanych dokumentu zapisywanego
                     * w publicznym folderze Pobrane przez MediaStore.
                     */
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                        )

                        /*
                         * Flaga IS_PENDING informuje system, że zapis pliku
                         * jeszcze trwa. Plik nie jest widoczny dla użytkownika,
                         * dopóki aplikacja nie zakończy zapisu.
                         */
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val resolver = contentResolver

                    /*
                     * Utworzenie nowego dokumentu PDF
                     * w systemowej kolekcji plików pobranych.
                     */
                    val uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    if (uri == null) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Nie udało się utworzyć pliku PDF",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    /*
                     * Zapis danych otrzymanych z backendu
                     * do utworzonego pliku w pamięci telefonu.
                     */
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        body.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    /*
                     * Po zakończeniu zapisu usuwamy flagę IS_PENDING.
                     * Od tej chwili plik będzie widoczny w aplikacji Pliki
                     * oraz w folderze Pobrane urządzenia.
                     */
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    Toast.makeText(
                        this@ReportsActivity,
                        "Pobrano raport do folderu Pobrane: $fileName",
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    /*
                     * Android 9 lub starszy:
                     * zapis dokumentu bezpośrednio w publicznym folderze Download.
                     */
                    val downloadsFolder =
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        )

                    // Utworzenie folderu Pobrane, jeżeli jeszcze nie istnieje.
                    if (!downloadsFolder.exists()) {
                        downloadsFolder.mkdirs()
                    }

                    // Utworzenie pliku docelowego o nazwie wygenerowanej dla raportu.
                    val file = File(downloadsFolder, fileName)

                    // Skopiowanie danych PDF z odpowiedzi backendu do lokalnego pliku.
                    body.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Toast.makeText(
                        this@ReportsActivity,
                        "Pobrano raport do folderu Pobrane: $fileName",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                /*
                 * Obsługa nieoczekiwanych błędów zapisu,
                 * na przykład braku możliwości utworzenia pliku.
                 */
                Toast.makeText(
                    this@ReportsActivity,
                    "Nie udało się zapisać pliku: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}