package com.example.controlefinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


class EditarLancamentoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val lancamentoId = intent.getIntExtra("lancamento_id", -1)

        setContent {
            ControleFinanceiroTheme {
                EditarLancamentoScreen(lancamentoId)
            }
        }
    }
}

@Composable
fun EditarLancamentoScreen(lancamentoId: Int) {
    val contexto = LocalContext.current

    var descricao by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("entrada") }
    var data by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    // Carregar dados do lançamento ao abrir a tela
    LaunchedEffect(lancamentoId) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.15.38:5000/lancamento/$lancamentoId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ Erro ao carregar lançamento: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val resp = response.body?.string()
                if (resp != null) {
                    val json = JSONObject(resp)
                    (contexto as? ComponentActivity)?.runOnUiThread {
                        descricao = json.optString("descricao")
                        valor = json.optDouble("valor", 0.0).toString()
                        tipo = json.optString("tipo")
                        data = json.optString("data")
                        observacao = json.optString("observacao", "")
                    }
                }
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Editar Lançamento", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = valor,
            onValueChange = { valor = it },
            label = { Text("Valor") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tipo,
            onValueChange = { tipo = it },
            label = { Text("Tipo (entrada/saida)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = data,
            onValueChange = { data = it },
            label = { Text("Data (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = observacao,
            onValueChange = { observacao = it },
            label = { Text("Observação (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (descricao.isNotBlank() && valor.isNotBlank() && tipo.isNotBlank() && data.isNotBlank()) {
                    val client = OkHttpClient()
                    val json = JSONObject().apply {
                        put("descricao", descricao)
                        put("valor", valor.toDoubleOrNull() ?: 0.0)
                        put("tipo", tipo)
                        put("data", data)
                        put("observacao", if (observacao.isBlank()) null else observacao)
                    }

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("http://192.168.15.38:5000/lancamento/$lancamentoId")
                        .put(body)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            println("❌ Erro ao atualizar lançamento: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val resp = response.body?.string()
                            println("✅ Resposta: $resp")
                            (contexto as? ComponentActivity)?.runOnUiThread {
                                mensagem = "Lançamento atualizado com sucesso!"
                            }
                        }
                    })
                } else {
                    mensagem = "Preencha todos os campos obrigatórios!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar Alterações")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (mensagem.isNotBlank()) {
            Text(mensagem, fontWeight = FontWeight.Bold)
        }
    }
}
