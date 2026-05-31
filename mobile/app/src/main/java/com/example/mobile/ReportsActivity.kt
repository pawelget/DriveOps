package com.example.mobile

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

class ReportsActivity : AppCompatActivity() {

    // Używamy MutableList, żeby móc usuwać elementy "na żywo" z ekranu
    private var reportsList: MutableList<ReportRecord> = mutableListOf()
    private var adapter: ReportAdapter? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        val reportsRoot = findViewById<View>(R.id.reportsRoot)

        val originalLeftPadding = reportsRoot.paddingLeft
        val originalTopPadding = reportsRoot.paddingTop
        val originalRightPadding = reportsRoot.paddingRight
        val originalBottomPadding = reportsRoot.paddingBottom

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

        ViewCompat.requestApplyInsets(reportsRoot)

        val lvReports = findViewById<ListView>(R.id.lvReports)
        val btnBackFromReports = findViewById<Button>(R.id.btnBackFromReports)


        btnBackFromReports.setOnClickListener { finish() }

        token = MainActivity.authToken
        if (token == null) {
            Toast.makeText(this, "Brak tokenu!", Toast.LENGTH_SHORT).show()
            return
        }

        // Pobieranie raportów na start
        fetchReports(lvReports)
    }

    private fun fetchReports(listView: ListView) {
        ApiClient.authService.getReports("Bearer $token").enqueue(object : Callback<List<ReportRecord>> {
            override fun onResponse(call: Call<List<ReportRecord>>, response: Response<List<ReportRecord>>) {
                if (response.isSuccessful) {
                    reportsList = response.body()?.toMutableList() ?: mutableListOf()
                    adapter = ReportAdapter(this@ReportsActivity, reportsList)
                    listView.adapter = adapter
                } else {
                    Toast.makeText(this@ReportsActivity, "Błąd pobierania raportów", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ReportRecord>>, t: Throwable) {
                Toast.makeText(this@ReportsActivity, "Błąd sieci: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- Adapter do Raportów z obsługą przycisków ---
    inner class ReportAdapter(context: AppCompatActivity, private val reports: MutableList<ReportRecord>) :
        ArrayAdapter<ReportRecord>(context, 0, reports) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_report, parent, false)
            val report = getItem(position) ?: return view

            val tvReportNumber = view.findViewById<TextView>(R.id.tvReportNumber)
            val tvReportCar = view.findViewById<TextView>(R.id.tvReportCar)
            val tvReportDetails = view.findViewById<TextView>(R.id.tvReportDetails)
            val btnDownloadPdf = view.findViewById<Button>(R.id.btnDownloadPdf)
            val btnDeleteReport = view.findViewById<Button>(R.id.btnDeleteReport)
            val btnSendEmail = view.findViewById<Button>(R.id.btnSendEmail)

            tvReportNumber.text = report.numer_raportu ?: "Nieznany raport"
            val car = report.samochod
            tvReportCar.text = "${car?.marka ?: ""} ${car?.model ?: ""} (${car?.numer_rejestracyjny ?: ""})"

            val koszt = report.wpis_serwisowy?.koszt_calkowity ?: 0.0
            val dataWyg = report.wygenerowano_w?.take(10) ?: "-"
            tvReportDetails.text = "Wygenerowano: $dataWyg\nKoszt naprawy: $koszt PLN"

            // --- AKCJA: WYŚLIJ EMAIL ---
            btnSendEmail.setOnClickListener {
                Toast.makeText(this@ReportsActivity, "Zlecono wysyłkę...", Toast.LENGTH_SHORT).show()

                ApiClient.authService.sendReportEmail("Bearer $token", report.id, EmailRequest())
                    .enqueue(object : Callback<EmailResponse> {
                        override fun onResponse(call: Call<EmailResponse>, response: Response<EmailResponse>) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@ReportsActivity, "Wysłano pomyślnie na Twój e-mail!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@ReportsActivity, "Błąd: Serwer odrzucił wysyłkę", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<EmailResponse>, t: Throwable) {
                            Toast.makeText(this@ReportsActivity, "Błąd sieci: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            // --- AKCJA: USUŃ ---
            btnDeleteReport.setOnClickListener {
                ApiClient.authService.deleteReport("Bearer $token", report.id).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            reports.remove(report) // Usuwamy z listy w pamięci
                            notifyDataSetChanged() // Odświeżamy ekran
                            Toast.makeText(this@ReportsActivity, "Usunięto raport", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                })
            }

            // --- AKCJA: POBIERZ PDF ---
            btnDownloadPdf.setOnClickListener {
                Toast.makeText(this@ReportsActivity, "Rozpoczynam pobieranie...", Toast.LENGTH_SHORT).show()
                ApiClient.authService.downloadReportPdf("Bearer $token", report.id).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful && response.body() != null) {
                            // Funkcja zapisująca plik do folderu Pobrane
                            savePdfToDisk(response.body()!!, "raport_${report.numer_raportu?.replace("/", "_")}.pdf")
                        } else {
                            Toast.makeText(this@ReportsActivity, "Błąd pliku", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
                })
            }

            return view
        }

        // Funkcja pomocnicza do zapisu pliku PDF
        // Funkcja pomocnicza zapisująca pobrany raport PDF w publicznym folderze Pobrane.
        private fun savePdfToDisk(body: ResponseBody, fileName: String) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    /*
                     * Od Androida 10 zapisujemy dokument przez MediaStore.
                     * Dzięki temu plik pojawi się w publicznym folderze Pobrane
                     * i będzie widoczny w zwykłym menedżerze plików telefonu.
                     */
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val resolver = contentResolver
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

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        body.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

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
                     * Starsze wersje Androida zapisują plik bezpośrednio
                     * w publicznym folderze Download.
                     */
                    val downloadsFolder =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                    if (!downloadsFolder.exists()) {
                        downloadsFolder.mkdirs()
                    }

                    val file = File(downloadsFolder, fileName)

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
                Toast.makeText(
                    this@ReportsActivity,
                    "Nie udało się zapisać pliku: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}