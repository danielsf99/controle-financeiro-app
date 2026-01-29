package com.example.controlefinanceiro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.Calendar

class DashboardActivity : ComponentActivity() {

    private var todosLancamentos = mutableStateOf<List<Map<String, Any>>>(listOf())
    private var usuarioId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        usuarioId = intent.getIntExtra("usuario_id", -1)
        if (usuarioId == -1) { finish(); return }

        setContent {
            ControleFinanceiroTheme {
                DashboardScreen(usuarioId, todosLancamentos.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (usuarioId != -1) atualizarDados()
    }

    private fun atualizarDados() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.15.38:5000/lancamentos/$usuarioId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    val json = JSONArray(body)
                    val tempLista = mutableListOf<Map<String, Any>>()
                    for (i in 0 until json.length()) {
                        val obj = json.getJSONObject(i)
                        tempLista.add(mapOf(
                            "valor" to obj.getDouble("valor"),
                            "tipo" to obj.getString("tipo").lowercase(),
                            "data" to obj.getString("data"),
                            "descricao" to obj.getString("descricao") // Agora tratada como Categoria
                        ))
                    }
                    runOnUiThread { todosLancamentos.value = tempLista }
                } catch (err: Exception) {}
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(usuarioId: Int, lancamentos: List<Map<String, Any>>) {
    val contexto = LocalContext.current
    val activity = contexto as? ComponentActivity

    val meses = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
    val cal = Calendar.getInstance()
    var mesSelecionadoIdx by remember { mutableStateOf(cal.get(Calendar.MONTH)) }

    val mesFormatado = "%02d".format(mesSelecionadoIdx + 1)
    val anoAtual = "2026"

    var entradasMes = 0.0
    var saidasMes = 0.0
    val categoriasMap = mutableMapOf<String, Double>()

    // Lógica de Processamento
    lancamentos.forEach { lanc ->
        val data = lanc["data"] as String
        if (data.startsWith("$anoAtual-$mesFormatado")) {
            val valor = lanc["valor"] as Double
            val desc = lanc["descricao"] as String

            if (lanc["tipo"] == "entrada") {
                entradasMes += valor
            } else {
                saidasMes += valor
                // Agrupar apenas saídas para o gráfico de categorias
                categoriasMap[desc] = categoriasMap.getOrDefault(desc, 0.0) + valor
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caixa Pessoal", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()), // Habilitar scroll para ver tudo
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // SELETOR DE MÊS
            ScrollableTabRow(
                selectedTabIndex = mesSelecionadoIdx,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                meses.forEachIndexed { index, nome ->
                    Tab(selected = mesSelecionadoIdx == index, onClick = { mesSelecionadoIdx = index }, text = { Text(nome) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CARDS DE RESUMO
            FinanceCard("Saldo do Mês", entradasMes - saidasMes, Color(0xFF1976D2), Icons.Default.Star)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { MiniCard("Entradas", entradasMes, Color(0xFF2E7D32), Icons.Default.KeyboardArrowUp) }
                Box(modifier = Modifier.weight(1f)) { MiniCard("Saídas", saidasMes, Color(0xFFC62828), Icons.Default.KeyboardArrowDown) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÕES DE AÇÃO
            ActionButton("Novo Lançamento", Icons.Default.Add) {
                val intent = Intent(contexto, AdicionarLancamentoActivity::class.java)
                intent.putExtra("usuario_id", usuarioId); contexto.startActivity(intent)
            }
            ActionButton("Ver Histórico", Icons.Default.List) {
                val intent = Intent(contexto, ListarLancamentosActivity::class.java)
                intent.putExtra("usuario_id", usuarioId); contexto.startActivity(intent)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- GRÁFICO DE CATEGORIAS (SAÍDAS) ---
            Text("Gastos por Categoria", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))

            if (categoriasMap.isEmpty()) {
                Text("Sem gastos neste mês.", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        categoriasMap.forEach { (nome, valor) ->
                            CategoryRow(nome, valor, saidasMes)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            TextButton(onClick = { activity?.finish() }) {
                Icon(Icons.Default.ExitToApp, null, tint = Color.Gray)
                Text(" Sair da Conta", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CategoryRow(nome: String, valor: Double, totalSaidas: Double) {
    val porcentagem = if (totalSaidas > 0) (valor / totalSaidas).toFloat() else 0f

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(nome, fontWeight = FontWeight.Medium, color = Color.Black)
            Text("R$ %.2f".format(valor), fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = porcentagem,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFFC62828),
            trackColor = Color(0xFFF5F5F5),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}


@Composable
fun FinanceCard(titulo: String, valor: Double, cor: Color, icone: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontSize = 14.sp, color = cor)
                Text("R$ %.2f".format(valor), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = cor)
            }
            Icon(icone, contentDescription = null, modifier = Modifier.size(48.dp), tint = cor)
        }
    }
}

@Composable
fun MiniCard(titulo: String, valor: Double, cor: Color, icone: ImageVector) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icone, contentDescription = null, tint = cor)
            Text(titulo, fontSize = 12.sp, color = cor)
            Text("R$ %.2f".format(valor), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cor)
        }
    }
}

@Composable
fun ActionButton(texto: String, icone: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
        Icon(icone, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(texto)
    }
}