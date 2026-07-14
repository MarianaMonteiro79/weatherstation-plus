package com.example.meteo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/* Realizado por:
*  - Mariana Monteiro, a22306164
*  - Júlia Santos, a22301020
* */

/**
 * Ecrã onde o utilizador escolhe:
 * - Tipo de dado (temperatura, humidade, qualidade do ar)
 * - Intervalo temporal
 * para gerar o gráfico.
 */
class DetalhesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // Elementos do layout
        val spinnerTipo = findViewById<Spinner>(R.id.spinnerTipo)
        val spinnerTempo = findViewById<Spinner>(R.id.spinnerTempo)
        val btnGerarGrafico = findViewById<Button>(R.id.btnGerarGrafico)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        // Spinner: tipo de dado
        val opcoesTipo = listOf(
            "Selecionar tipo de dado",
            "Temperatura",
            "Humidade",
            "Qualidade do Ar (Partículas)"
        )

        val adapterTipo = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcoesTipo
        )
        adapterTipo.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spinnerTipo.adapter = adapterTipo

        // Spinner: intervalo de tempo
        val opcoesTempo = listOf(
            "Selecionar intervalo",
            "Últimas 24 horas",
            "Últimos 7 dias",
            "Últimos 30 dias"
        )

        val adapterTempo = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcoesTempo
        )
        adapterTempo.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spinnerTempo.adapter = adapterTempo

        // Botao geral gráficos
        btnGerarGrafico.setOnClickListener {

            // Validação das escolhas
            if (spinnerTipo.selectedItemPosition == 0 ||
                spinnerTempo.selectedItemPosition == 0
            ) {
                Toast.makeText(
                    this,
                    "Por favor selecione o tipo de dado e o intervalo de tempo",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Dados selecionados
            val tipoSelecionado = spinnerTipo.selectedItem as String
            val tempoSelecionado = spinnerTempo.selectedItem as String

            // Abre a activity do gráfico
            val intent = Intent(this, GraficoActivity::class.java)
            intent.putExtra("tipo", tipoSelecionado)
            intent.putExtra("intervalo", tempoSelecionado)
            startActivity(intent)
        }


        // Botão voltar
        btnVoltar.setOnClickListener {
            finish()
        }
    }
}
