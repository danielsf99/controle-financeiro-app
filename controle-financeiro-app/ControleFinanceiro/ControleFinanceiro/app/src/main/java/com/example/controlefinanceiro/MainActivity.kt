package com.example.controlefinanceiro

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControleFinanceiroTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Controle Financeiro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuário") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                Log.d("LOGIN_TESTE", "Botão clicado")
                fazerLogin(usuario, senha) { sucesso, usuarioId ->
                    if (sucesso) {
                        val intent = Intent(context, DashboardActivity::class.java)
                        intent.putExtra("usuario_id", usuarioId)
                        context.startActivity(intent)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
    }
}

fun fazerLogin(usuario: String, senha: String, callback: (Boolean, Int) -> Unit) {
    val client = OkHttpClient()

    val json = JSONObject().apply {
        put("username", usuario)
        put("password", senha)
    }

    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

    val request = Request.Builder()
        .url("http://192.168.15.38:5000/login")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("API", "❌ Erro de conexão: ${e.message}")
            callback(false, -1)
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            Log.d("API", "Resposta login: $body")
            if (response.isSuccessful && body != null) {
                val usuarioId = JSONObject(body).getInt("usuario_id")
                callback(true, usuarioId)
            } else {
                callback(false, -1)
            }
        }
    })
}
