package com.example.controlefinanceiro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val usuarioId = intent.getIntExtra("usuario_id", -1)
        if (usuarioId == -1) {
            finish()
            return
        }

        setContent {
            ControleFinanceiroTheme {
                DashboardScreen(usuarioId)
            }
        }
    }
}

@Composable
fun DashboardScreen(usuarioId: Int) {
    val contexto = LocalContext.current
    val activity = contexto as? ComponentActivity

    var entradas by remember { mutableStateOf(0.0) }
    var saidas by remember { mutableStateOf(0.0) }

    // Buscar lançamentos
    LaunchedEffect(usuarioId) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.15.38:5000/lancamentos/$usuarioId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ Erro ao buscar lançamentos: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val json = JSONArray(body)
                var e = 0.0
                var s = 0.0
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    val valor = item.getDouble("valor")
                    if (item.getString("tipo") == "entrada") e += valor
                    else s += valor
                }

                activity?.runOnUiThread {
                    entradas = e
                    saidas = s
                }
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dashboard",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text("Entradas: R$ $entradas", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text("Saídas: R$ $saidas", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text("Saldo: R$ ${entradas - saidas}", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(30.dp))

        // Função auxiliar para abrir Activity
        fun abrirTela(destino: Class<*>) {
            activity?.startActivity(Intent(activity, destino).apply {
                putExtra("usuario_id", usuarioId)
            })
        }

        Button(
            onClick = { abrirTela(AdicionarLancamentoActivity::class.java) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Adicionar Lançamento") }

        Spacer(modifier = Modifier.height(12.dp))




        Button(
            onClick = { abrirTela(ListarLancamentosActivity::class.java) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Visualizar Todos os Lançamentos") }

        Spacer(modifier = Modifier.height(20.dp))

        // Botão Sair
        Button(
            onClick = {
                activity?.startActivity(
                    Intent(activity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sair", color = androidx.compose.ui.graphics.Color.White)
        }
    }
}
