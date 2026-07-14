package com.example.meteo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Realizado por:
*  - Mariana Monteiro, a22306164
*  - Júlia Santos, a22301020
* */

/**
 * Mostra um gráfico temporal dos dados meteorológicos
 * e permite visualizar:
 * - Média simples
 * - Média móvel
 * - Tendência (regressão linear)
 */
class GraficoActivity : AppCompatActivity() {

    // Elementos do layout
    private lateinit var graph: GraphView
    private lateinit var title: TextView
    private lateinit var txtComparacao: TextView

    private lateinit var btnMediaSimples: Button
    private lateinit var btnMediaMovel: Button
    private lateinit var btnTendencia: Button
    private lateinit var btnVoltar: Button

    // Firestore
    private val db = FirebaseFirestore.getInstance()

    // Pontos do gráfico principal
    private var pontos: List<DataPoint> = emptyList()

    // Intervalo selecionado pelo utilizador
    private lateinit var intervaloSelecionado: String

    // Séries opcionais (para ativar/desativar)
    private var serieMediaSimples: LineGraphSeries<DataPoint>? = null
    private var serieMediaMovel: LineGraphSeries<DataPoint>? = null
    private var serieTendencia: LineGraphSeries<DataPoint>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grafico)

        // Ligação dos elementos
        graph = findViewById(R.id.graphView)
        title = findViewById(R.id.graphTitle)
        txtComparacao = findViewById(R.id.textComparacao)

        btnMediaSimples = findViewById(R.id.btnMediaSimples)
        btnMediaMovel = findViewById(R.id.btnMedia)
        btnTendencia = findViewById(R.id.btnTendencia)
        btnVoltar = findViewById(R.id.btnVoltar)

        // Dados recebidos da DetalhesActivity
        val tipo = intent.getStringExtra("tipo") ?: "Temperatura"
        intervaloSelecionado =
            intent.getStringExtra("intervalo") ?: "Últimas 24 horas"

        title.text = "Gráfico de $tipo — $intervaloSelecionado"

        // Carrega dados da base de dados
        carregarDados(tipo)

        // Botões de funcionalidades
        btnMediaSimples.setOnClickListener { toggleMediaSimples() }
        btnMediaMovel.setOnClickListener { toggleMediaMovel() }
        btnTendencia.setOnClickListener { toggleTendencia() }
        btnVoltar.setOnClickListener { finish() }
    }

    //Obtém o timestamp mais recente para definir o intervalo temporal
    private fun carregarDados(tipo: String) {

        db.collection("STATION_00")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    title.text = "Sem dados disponíveis"
                    return@addOnSuccessListener
                }

                val agora = snapshot.documents[0].getLong("timestamp")
                    ?: return@addOnSuccessListener

                val duracao = when (intervaloSelecionado) {
                    "Últimas 24 horas" -> 24 * 3600
                    "Últimos 7 dias" -> 7 * 24 * 3600
                    "Últimos 30 dias" -> 30 * 24 * 3600
                    else -> 24 * 3600
                }

                val inicioAtual = agora - duracao
                val inicioAnterior = inicioAtual - duracao

                carregarPeriodo(tipo, inicioAtual, agora) { atual ->
                    carregarPeriodo(tipo, inicioAnterior, inicioAtual) { anterior ->
                        mostrarComparacao(atual, anterior)
                    }
                }

                carregarDadosComLimite(tipo, inicioAtual)
            }
    }

    //Carrega os dados dentro de um intervalo específico (usado na comparação)
    private fun carregarPeriodo(
        tipo: String,
        inicio: Long,
        fim: Long,
        callback: (List<Double>) -> Unit
    ) {
        db.collection("STATION_00")
            .whereGreaterThan("timestamp", inicio)
            .whereLessThanOrEqualTo("timestamp", fim)
            .get()
            .addOnSuccessListener { result ->
                val valores = result.mapNotNull {
                    when (tipo) {
                        "Temperatura" -> it.getDouble("temperatura")
                        "Humidade" -> it.getDouble("humidade")
                        "Qualidade do Ar (Partículas)" -> it.getDouble("particulas")
                        else -> null
                    }
                }
                callback(valores)
            }
    }

    //Mostra a comparação entre dois períodos consecutivos
    private fun mostrarComparacao(atual: List<Double>, anterior: List<Double>) {
        if (atual.isEmpty() || anterior.isEmpty()) {
            txtComparacao.text = "Comparação indisponível"
            return
        }

        val mediaAtual = atual.average()
        val mediaAnterior = anterior.average()
        val variacao = ((mediaAtual - mediaAnterior) / mediaAnterior) * 100

        txtComparacao.text =
            "Período atual: %.2f\nPeríodo anterior: %.2f\nVariação: %.1f %%"
                .format(mediaAtual, mediaAnterior, variacao)
    }

    //Carrega os dados dentro do intervalo selecionado
    private fun carregarDadosComLimite(tipo: String, limite: Long) {

        db.collection("STATION_00")
            .whereGreaterThan("timestamp", limite)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->

                val lista = ArrayList<DataPoint>()

                for (doc in result) {
                    val ts = doc.getLong("timestamp") ?: continue
                    val x = ts.toDouble()

                    val y = when (tipo) {
                        "Temperatura" -> doc.getDouble("temperatura")
                        "Humidade" -> doc.getDouble("humidade")
                        "Qualidade do Ar (Partículas)" -> doc.getDouble("particulas")
                        else -> null
                    }

                    if (y != null) {
                        lista.add(DataPoint(x, y))
                    }
                }

                if (lista.isEmpty()) {
                    title.text = "Sem dados disponíveis para $tipo"
                    return@addOnSuccessListener
                }

                pontos = lista
                desenharGraficoBase()
            }
    }

    //Desenha o gráfico principal (linha base)
    private fun desenharGraficoBase() {
        graph.removeAllSeries()

        serieMediaSimples = null
        serieMediaMovel = null
        serieTendencia = null

        val seriePrincipal = LineGraphSeries(pontos.toTypedArray())
        graph.addSeries(seriePrincipal)

        graph.viewport.isScalable = true
        graph.viewport.isScrollable = true
        graph.gridLabelRenderer.numHorizontalLabels = 5

        graph.gridLabelRenderer.labelFormatter =
            object : DefaultLabelFormatter() {
                override fun formatLabel(value: Double, isValueX: Boolean): String {
                    return if (isValueX) {
                        val sdf = when (intervaloSelecionado) {
                            "Últimas 24 horas" ->
                                SimpleDateFormat("HH:mm", Locale.getDefault())
                            else ->
                                SimpleDateFormat("dd/MM", Locale.getDefault())
                        }
                        sdf.format(Date((value * 1000).toLong()))
                    } else {
                        String.format("%.1f", value)
                    }
                }
            }
    }

    //Ativa/desativa a linha da média simples
    private fun toggleMediaSimples() {
        if (serieMediaSimples != null) {
            graph.removeSeries(serieMediaSimples)
            serieMediaSimples = null
            return
        }

        val media = pontos.map { it.y }.average()
        val xInicio = pontos.first().x
        val xFim = pontos.last().x

        serieMediaSimples = LineGraphSeries(
            arrayOf(
                DataPoint(xInicio, media),
                DataPoint(xFim, media)
            )
        )

        graph.addSeries(serieMediaSimples)
    }

    //Ativa/desativa a média móvel (janela de 5 pontos)
    private fun toggleMediaMovel() {
        if (serieMediaMovel != null) {
            graph.removeSeries(serieMediaMovel)
            serieMediaMovel = null
            return
        }

        if (pontos.size < 5) return

        val janela = 5
        val lista = ArrayList<DataPoint>()

        for (i in janela - 1 until pontos.size) {
            val subLista = pontos.subList(i - janela + 1, i + 1)
            val media = subLista.map { it.y }.average()
            lista.add(DataPoint(pontos[i].x, media))
        }

        serieMediaMovel = LineGraphSeries(lista.toTypedArray())
        graph.addSeries(serieMediaMovel)
    }

    //Ativa/desativa a linha de tendência (regressão linear)
    private fun toggleTendencia() {
        if (serieTendencia != null) {
            graph.removeSeries(serieTendencia)
            serieTendencia = null
            return
        }

        val n = pontos.size
        val sumX = pontos.sumOf { it.x }
        val sumY = pontos.sumOf { it.y }
        val sumXY = pontos.sumOf { it.x * it.y }
        val sumX2 = pontos.sumOf { it.x * it.x }

        val a = (n * sumXY - sumX * sumY) /
                (n * sumX2 - sumX * sumX)
        val b = (sumY - a * sumX) / n

        val xInicio = pontos.first().x
        val xFim = pontos.last().x

        serieTendencia = LineGraphSeries(
            arrayOf(
                DataPoint(xInicio, a * xInicio + b),
                DataPoint(xFim, a * xFim + b)
            )
        )

        graph.addSeries(serieTendencia)
    }
}
