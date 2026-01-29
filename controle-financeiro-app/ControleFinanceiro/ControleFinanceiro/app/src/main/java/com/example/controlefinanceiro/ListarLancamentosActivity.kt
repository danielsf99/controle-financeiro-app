package com.example.controlefinanceiro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class Lancamento(
    val id: Int,
    val descricao: String,
    val valor: Double,
    val tipo: String,
    val data: String,
    val observacao: String?
)

class ListarLancamentosActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val usuarioId = intent.getIntExtra("usuario_id", -1)

        setContent {
            ControleFinanceiroTheme {
                ListarLancamentosScreen(usuarioId)
            }
        }
    }
}

@Composable
fun ListarLancamentosScreen(usuarioId: Int) {
    val contexto = LocalContext.current

    var lista by remember { mutableStateOf(listOf<Lancamento>()) }
    var carregando by remember { mutableStateOf(true) }

    // Buscar lançamentos
    LaunchedEffect(usuarioId) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.15.38:5000/lancamentos/$usuarioId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ Erro: ${e.message}")
                carregando = false
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    val jsonArray = JSONArray(body)
                    val tempLista = mutableListOf<Lancamento>()
                    for (i in 0 until jsonArray.length()) {
                        val l = jsonArray.getJSONObject(i)
                        tempLista.add(
                            Lancamento(
                                id = l.getInt("id"),
                                descricao = l.getString("descricao"),
                                valor = l.getDouble("valor"),
                                tipo = l.getString("tipo"),
                                data = l.getString("data"),
                                observacao = l.optString("observacao", null)
                            )
                        )
                    }
                    lista = tempLista
                    carregando = false
                }
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Seus Lançamentos", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)

        Spacer(modifier = Modifier.height(16.dp))

        if (carregando) {
            Text("Carregando...")
        } else {
            LazyColumn {
                items(lista) { lanc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${lanc.descricao} - R$ %.2f".format(lanc.valor), fontWeight = FontWeight.Bold)
                            Text("Tipo: ${lanc.tipo}")
                            Text("Data: ${lanc.data}")
                            lanc.observacao?.let {
                                Text("Obs: $it")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    val intent = Intent(contexto, EditarLancamentoActivity::class.java)
                                    intent.putExtra("lancamento_id", lanc.id)
                                    contexto.startActivity(intent)
                                }) { Text("Editar") }

                                Button(onClick = {
                                    val client = OkHttpClient()
                                    val request = Request.Builder()
                                        .url("http://192.168.15.38:5000/lancamento/${lanc.id}")
                                        .delete()
                                        .build()

                                    client.newCall(request).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            println("❌ Erro ao deletar: ${e.message}")
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            println("✅ Deletado: ${response.body?.string()}")
                                            // Atualizar lista após exclusão
                                            lista = lista.filter { it.id != lanc.id }
                                        }
                                    })
                                }) { Text("Excluir") }
                            }
                        }
                    }
                }
            }
        }
    }
}
