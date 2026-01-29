package com.example.controlefinanceiro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    var carregando by remember { mutableStateOf(false) }
    var erroMensagem by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Caixa Pessoal",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Text(
            text = "Sua vida financeira organizada em um só lugar.",
            fontSize = 16.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        // CAMPO USUÁRIO
        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuário", color = Color.Black) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            // CORES FIXADAS PARA PRETO
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Black,
                focusedLeadingIconColor = Color.Black,
                unfocusedLeadingIconColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CAMPO SENHA
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha", color = Color.Black) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Black,
                focusedLeadingIconColor = Color.Black,
                unfocusedLeadingIconColor = Color.Black
            )
        )

        TextButton(
            onClick = { /* Estético */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Esqueci minha senha", fontSize = 14.sp, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (usuario.isNotBlank() && senha.isNotBlank()) {
                    carregando = true
                    fazerLogin(usuario, senha) { sucesso, usuarioId ->
                        carregando = false
                        if (sucesso) {
                            val intent = Intent(context, DashboardActivity::class.java)
                            intent.putExtra("usuario_id", usuarioId)
                            context.startActivity(intent)
                        } else {
                            erroMensagem = "Usuário ou senha incorretos."
                        }
                    }
                } else {
                    erroMensagem = "Preencha todos os campos."
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !carregando,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            if (carregando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Entrar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (erroMensagem.isNotBlank()) {
            Text(erroMensagem, color = Color.Red, modifier = Modifier.padding(top = 16.dp))
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
        override fun onFailure(call: Call, e: IOException) { callback(false, -1) }
        override fun onResponse(call: Call, response: Response) {
            val b = response.body?.string()
            if (response.isSuccessful && b != null) {
                try {
                    val uid = JSONObject(b).getInt("usuario_id")
                    callback(true, uid)
                } catch (e: Exception) { callback(false, -1) }
            } else { callback(false, -1) }
        }
    })
}