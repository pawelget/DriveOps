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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Aktywność odpowiedzialna za widok alertów systemowych.
 *
 * Widok umożliwia:
 * - pobranie alertów dotyczących pojazdów użytkownika,
 * - wyświetlenie ich w postaci listy kart,
 * - prezentację tytułu, opisu, daty i priorytetu alertu,
 * - kolorystyczne wyróżnienie ważności komunikatu,
 * - powrót do poprzedniego ekranu aplikacji.
 */
class AlertsActivity : AppCompatActivity() {

    /**
     * Lista alertów pobranych z backendu.
     * Po poprawnej odpowiedzi serwera jest przekazywana do adaptera,
     * który odpowiada za wyświetlenie poszczególnych komunikatów.
     */
    private var alertsList: List<Alert> = emptyList()

    /**
     * Metoda wykonywana podczas tworzenia ekranu alertów.
     * Odpowiada za przygotowanie interfejsu, bezpiecznych odstępów
     * od pasków systemowych oraz pobranie danych z backendu.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Podpięcie pliku XML określającego wygląd widoku alertów.
        setContentView(R.layout.activity_alerts)

        /*
         * Pobranie głównego kontenera ekranu.
         * Kontener zostanie odsunięty od górnego paska statusu
         * oraz dolnego paska nawigacyjnego telefonu.
         */
        val alertsRoot = findViewById<View>(R.id.alertsRoot)

        // Zapamiętanie odstępów ustawionych początkowo w layoucie XML.
        val originalLeftPadding = alertsRoot.paddingLeft
        val originalTopPadding = alertsRoot.paddingTop
        val originalRightPadding = alertsRoot.paddingRight
        val originalBottomPadding = alertsRoot.paddingBottom

        /*
         * Obsługa marginesów systemowych urządzenia.
         * Do podstawowych odstępów widoku dodawane są rzeczywiste
         * rozmiary pasków systemowych aktualnego telefonu.
         */
        ViewCompat.setOnApplyWindowInsetsListener(alertsRoot) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                originalLeftPadding + systemBars.left,
                originalTopPadding + systemBars.top,
                originalRightPadding + systemBars.right,
                originalBottomPadding + systemBars.bottom
            )

            windowInsets
        }

        // Wymuszenie zastosowania poprawnych odstępów po utworzeniu widoku.
        ViewCompat.requestApplyInsets(alertsRoot)

        // Pobranie listy alertów oraz przycisku powrotu z layoutu.
        val lvAlerts = findViewById<ListView>(R.id.lvAlerts)
        val btnBackFromAlerts = findViewById<Button>(R.id.btnBackFromAlerts)

        /*
         * Obsługa przycisku powrotu.
         * Zamknięcie bieżącej aktywności powoduje przejście
         * do poprzedniego widoku, czyli ekranu pojazdów.
         */
        btnBackFromAlerts.setOnClickListener {
            finish()
        }

        /*
         * Pobranie tokenu JWT zapisanego po poprawnym logowaniu.
         * Token jest wymagany, ponieważ alerty są przypisane
         * do konkretnego zalogowanego użytkownika.
         */
        val token = MainActivity.authToken

        if (token == null) {
            Toast.makeText(
                this,
                "Brak tokenu!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * Wysłanie żądania do backendu w celu pobrania alertów.
         * Token JWT jest przekazywany w nagłówku Authorization
         * w formacie Bearer <token>.
         */
        ApiClient.authService.getAlerts("Bearer $token")
            .enqueue(object : Callback<AlertsResponse> {

                /**
                 * Metoda wykonywana po otrzymaniu odpowiedzi HTTP od backendu.
                 * Przy poprawnej odpowiedzi lista alertów jest wyświetlana
                 * z użyciem adaptera AlertAdapter.
                 */
                override fun onResponse(
                    call: Call<AlertsResponse>,
                    response: Response<AlertsResponse>
                ) {
                    if (response.isSuccessful) {
                        /*
                         * Backend zwraca obiekt AlertsResponse,
                         * wewnątrz którego znajduje się właściwa lista alertów.
                         */
                        alertsList = response.body()?.alerts ?: emptyList()

                        // Przekazanie pobranych danych do adaptera listy.
                        lvAlerts.adapter = AlertAdapter(
                            this@AlertsActivity,
                            alertsList
                        )
                    } else {
                        Toast.makeText(
                            this@AlertsActivity,
                            "Błąd pobierania danych",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                /**
                 * Metoda wykonywana w przypadku problemu z połączeniem.
                 *
                 * Błąd może wystąpić między innymi wtedy, gdy:
                 * - backend nie jest uruchomiony,
                 * - telefon nie znajduje się w tej samej sieci co serwer,
                 * - adres backendu w aplikacji jest niepoprawny.
                 */
                override fun onFailure(
                    call: Call<AlertsResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@AlertsActivity,
                        "Błąd sieci: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Adapter odpowiedzialny za tworzenie kart alertów
     * wyświetlanych w elemencie ListView.
     *
     * @param context kontekst aktywności potrzebny do tworzenia widoków,
     * @param alerts lista alertów, które mają zostać wyświetlone.
     */
    inner class AlertAdapter(
        context: AppCompatActivity,
        private val alerts: List<Alert>
    ) : ArrayAdapter<Alert>(context, 0, alerts) {

        // Zwraca liczbę alertów wyświetlanych na liście.
        override fun getCount(): Int = alerts.size

        // Zwraca alert znajdujący się pod wskazanym indeksem.
        override fun getItem(position: Int): Alert = alerts[position]

        /**
         * Tworzy lub aktualizuje widok pojedynczego alertu.
         *
         * Każda karta prezentuje:
         * - tytuł komunikatu,
         * - opis alertu,
         * - datę,
         * - priorytet wraz z odpowiadającym mu kolorem.
         */
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            /*
             * Jeżeli Android przekazał istniejący widok, wykorzystujemy go ponownie.
             * W przeciwnym przypadku tworzona jest nowa karta na podstawie
             * layoutu item_alert.xml.
             */
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_alert, parent, false)

            // Pobranie alertu odpowiadającego aktualnej pozycji listy.
            val alert = getItem(position)

            // Pobranie elementów tekstowych z layoutu pojedynczej karty.
            val tvAlertTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
            val tvAlertDesc = view.findViewById<TextView>(R.id.tvAlertDesc)
            val tvAlertDate = view.findViewById<TextView>(R.id.tvAlertDate)
            val tvAlertPriority = view.findViewById<TextView>(R.id.tvAlertPriority)

            // Wyświetlenie tytułu oraz opisu alertu.
            tvAlertTitle.text = alert.tytul.ifBlank { "Brak tytułu" }
            tvAlertDesc.text = alert.opis

            /*
             * Wyświetlenie daty alertu.
             * Jeżeli backend zwraca datę razem z godziną,
             * prezentowane jest wyłącznie pierwsze 10 znaków,
             * czyli część zgodna z formatem RRRR-MM-DD.
             */
            val fullDate = alert.data
            tvAlertDate.text =
                if (fullDate.length > 10) fullDate.substring(0, 10) else fullDate

            /*
             * Kolorystyczne oznaczanie priorytetu alertu.
             *
             * - alert krytyczny jest wyróżniany kolorem czerwonym,
             * - ostrzeżenie kolorem pomarańczowym,
             * - informacja kolorem niebieskim,
             * - nieznany typ kolorem szarym.
             */
            when (alert.priorytet) {
                "critical" -> {
                    tvAlertPriority.text = "KRYTYCZNE"
                    tvAlertPriority.setTextColor(
                        Color.parseColor("#FF4C4C")
                    )
                }

                "warning" -> {
                    tvAlertPriority.text = "OSTRZEŻENIE"
                    tvAlertPriority.setTextColor(
                        Color.parseColor("#FFA500")
                    )
                }

                "info" -> {
                    tvAlertPriority.text = "INFO"
                    tvAlertPriority.setTextColor(
                        Color.parseColor("#4C9AFF")
                    )
                }

                else -> {
                    tvAlertPriority.text = alert.priorytet.uppercase()
                    tvAlertPriority.setTextColor(Color.GRAY)
                }
            }

            return view
        }
    }
}