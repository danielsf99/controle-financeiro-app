package com.example.controlefinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class DeletarLancamentoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ControleFinanceiroTheme {
                DeletarLancamentoScreen()
            }
        }
    }
}

@Composable
fun DeletarLancamentoScreen() {
    var lancamentoId by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Deletar Lançamento", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lancamentoId,
            onValueChange = { lancamentoId = it },
            label = { Text("ID do lançamento") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (lancamentoId.isNotBlank()) {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("http://192.168.15.38:5000/lancamento/$lancamentoId")
                        .delete()
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            println("❌ Erro ao deletar lançamento: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val resp = response.body?.string()
                            println("✅ Resposta: $resp")
                            mensagem = if (response.isSuccessful) {
                                "Lançamento deletado com sucesso!"
                            } else {
                                "Erro ao deletar lançamento"
                            }
                        }
                    })
                } else {
                    mensagem = "Digite o ID do lançamento!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Deletar")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (mensagem.isNotBlank()) {
            Text(mensagem, fontWeight = FontWeight.Bold)
        }
    }
}
