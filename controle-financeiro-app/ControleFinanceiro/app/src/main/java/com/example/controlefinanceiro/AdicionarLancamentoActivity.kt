package com.example.controlefinanceiro

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class AdicionarLancamentoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val usuarioId = intent.getIntExtra("usuario_id", -1)

        setContent {
            ControleFinanceiroTheme {
                AdicionarLancamentoScreen(usuarioId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdicionarLancamentoScreen(usuarioId: Int) {
    val contexto = LocalContext.current
    val activity = contexto as? ComponentActivity

    var categoria by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("entrada") }
    var data by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    // Calendário
    val calendario = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        contexto,
        { _, ano, mes, dia ->

            data = "%04d-%02d-%02d".format(ano, mes + 1, dia)
        },
        calendario.get(Calendar.YEAR),
        calendario.get(Calendar.MONTH),
        calendario.get(Calendar.DAY_OF_MONTH)
    )

    val campoCores = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.LightGray,
        cursorColor = Color.White,
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.Gray
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Lançamento", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = categoria,
                onValueChange = { categoria = it },
                label = { Text("Categoria (Ex: Alimentação, Lazer)") },
                modifier = Modifier.fillMaxWidth(),
                colors = campoCores
            )

            OutlinedTextField(
                value = valor,
                onValueChange = { valor = it },
                label = { Text("Valor (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = campoCores
            )

            Text("Tipo:", fontWeight = FontWeight.Bold, color = Color.White)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ElevatedFilterChip(
                    selected = tipo == "entrada",
                    onClick = { tipo = "entrada" },
                    label = { Text("Entrada") },
                    modifier = Modifier.weight(1f)
                )
                ElevatedFilterChip(
                    selected = tipo == "saida",
                    onClick = { tipo = "saida" },
                    label = { Text("Saída") },
                    modifier = Modifier.weight(1f)
                )
            }

            // DATA COM CALENDÁRIO
            OutlinedTextField(
                value = data,
                onValueChange = { },
                label = { Text("Data do Lançamento") },
                readOnly = true, // Impede digitar manualmente
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }, // Abre ao clicar no campo
                enabled = false, // Desabilita interação direta para usar o clique do Box
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.White,
                    disabledLabelColor = Color.White,
                    disabledBorderColor = Color.Gray,
                    disabledLeadingIconColor = Color.White
                ),
                leadingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                }
            )

            Button(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth().height(56.dp).offset(y = (-72).dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {}

            OutlinedTextField(
                value = observacao,
                onValueChange = { observacao = it },
                label = { Text("Observação (Opcional)") },
                modifier = Modifier.fillMaxWidth().offset(y = (-56).dp),
                minLines = 3,
                colors = campoCores
            )

            Button(
                onClick = {
                    if (categoria.isNotBlank() && valor.isNotBlank() && data.isNotBlank()) {
                        val client = OkHttpClient()
                        val json = JSONObject().apply {
                            put("descricao", categoria)
                            put("valor", valor.replace(",", ".").toDoubleOrNull() ?: 0.0)
                            put("tipo", tipo)
                            put("data", data)
                            put("observacao", if (observacao.isBlank()) null else observacao)
                            put("usuario_id", usuarioId)
                        }

                        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                        val request = Request.Builder()
                            .url("http://192.168.15.38:5000/lancamentos")
                            .post(body)
                            .build()

                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {}
                            override fun onResponse(call: Call, response: Response) {
                                activity?.runOnUiThread {
                                    mensagem = "Salvo com sucesso!"
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        activity.finish()
                                    }, 1000)
                                }
                            }
                        })
                    } else {
                        mensagem = "Preencha os campos obrigatórios!"
                    }
                },
                modifier = Modifier.fillMaxWidth().offset(y = (-56).dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, null)
                Text(" Salvar Lançamento")
            }
        }
    }
}