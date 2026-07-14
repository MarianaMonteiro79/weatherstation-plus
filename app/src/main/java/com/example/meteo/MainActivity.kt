package com.example.meteo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Realizado por:
*  - Mariana Monteiro, a22306164
*  - Júlia Santos, a22301020
* */

/**
 * Ecrã principal da aplicação.
 * Mostra a última leitura disponível da estação meteorológica:
 * - Temperatura
 * - Humidade
 * - Qualidade do ar (partículas)
 * - Data/hora da leitura
 */
class MainActivity : AppCompatActivity() {

    // TextViews onde os valores serão apresentados
    private lateinit var valueTemp: TextView
    private lateinit var valueHumidity: TextView
    private lateinit var valueParticles: TextView
    private lateinit var valueTimestamp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Ajusta automaticamente os espaçamentos para barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Ligação dos elementos do layout
        valueTemp = findViewById(R.id.valueTemp)
        valueHumidity = findViewById(R.id.valueHumidity)
        valueParticles = findViewById(R.id.valueParticles)
        valueTimestamp = findViewById(R.id.valueTimestamp)

        // Botão para abrir o ecrã de detalhes
        findViewById<Button>(R.id.btnVerDetalhes).setOnClickListener {
            startActivity(Intent(this, DetalhesActivity::class.java))
        }

        // Carrega a última leitura da base de dados
        carregarUltimaLeitura()
    }

    /**
     * Lê a leitura mais recente da coleção STATION_00 no Firestore
     * e apresenta os valores no ecrã principal.
     */
    private fun carregarUltimaLeitura() {

        val db = FirebaseFirestore.getInstance()

        db.collection("STATION_00")
            .orderBy("timestamp", Query.Direction.DESCENDING) // ordena do mais recente
            .limit(1) // apenas o último registo
            .get()
            .addOnSuccessListener { result ->

                if (!result.isEmpty) {
                    val doc = result.documents[0]

                    // Temperatura
                    valueTemp.text =
                        doc.getDouble("temperatura")?.let { "$it °C" } ?: "--"

                    // Humidade
                    valueHumidity.text =
                        doc.getDouble("humidade")?.let { "$it %" } ?: "--"

                    // Qualidade do ar (partículas)
                    valueParticles.text =
                        doc.getDouble("particulas")?.let { "$it µg/m³" } ?: "--"

                    // Conversão do timestamp para data legível
                    val timestamp = doc.getLong("timestamp")
                    if (timestamp != null) {
                        val date = Date(timestamp * 1000)
                        val format = SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()
                        )
                        valueTimestamp.text = format.format(date)
                    } else {
                        valueTimestamp.text = "--"
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao ler dados", e)
            }
    }
}
