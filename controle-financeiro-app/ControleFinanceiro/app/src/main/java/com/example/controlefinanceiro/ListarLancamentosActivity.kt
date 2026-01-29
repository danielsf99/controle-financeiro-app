package com.example.controlefinanceiro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlefinanceiro.ui.theme.ControleFinanceiroTheme
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

// DATA CLASS FORA DA CLASSE PARA ACESSO GLOBAL
data class Lancamento(
    val id: Int,
    val descricao: String,
    val valor: Double,
    val tipo: String,
    val data: String,
    val observacao: String?
)

class ListarLancamentosActivity : ComponentActivity() {


    private var listaOriginal = mutableStateOf<List<Lancamento>>(listOf())
    private var carregando = mutableStateOf(true)
    private var usuarioId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        usuarioId = intent.getIntExtra("usuario_id", -1)

        setContent {
            ControleFinanceiroTheme {
                ListarLancamentosScreen(usuarioId, listaOriginal.value, carregando.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (usuarioId != -1) buscarDados()
    }

    private fun buscarDados() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.15.38:5000/lancamentos/$usuarioId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { carregando.value = false }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    val jsonArray = JSONArray(body)
                    val tempLista = mutableListOf<Lancamento>()
                    for (i in 0 until jsonArray.length()) {
                        val l = jsonArray.getJSONObject(i)
                        tempLista.add(Lancamento(
                            id = l.getInt("id"),
                            descricao = l.getString("descricao"),
                            valor = l.getDouble("valor"),
                            tipo = l.getString("tipo"),
                            data = l.getString("data"),
                            observacao = l.optString("observacao", null)
                        ))
                    }
                    runOnUiThread {
                        listaOriginal.value = tempLista
                        carregando.value = false
                    }
                } catch (e: Exception) {
                    runOnUiThread { carregando.value = false }
                }
            }
        })
    }

    fun deletarItem(id: Int) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.15.38:5000/lancamento/$id")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    listaOriginal.value = listaOriginal.value.filter { it.id != id }
                }
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarLancamentosScreen(usuarioId: Int, listaParaExibir: List<Lancamento>, estaCarregando: Boolean) {
    val contexto = LocalContext.current
    val activity = contexto as? ListarLancamentosActivity

    var filtroSelecionado by remember { mutableStateOf("todos") }

    //  FILTRO
    val listaFiltrada = remember(listaParaExibir, filtroSelecionado) {
        when (filtroSelecionado) {
            "entrada" -> listaParaExibir.filter { it.tipo.lowercase() == "entrada" }
            "saida" -> listaParaExibir.filter { it.tipo.lowercase() == "saida" }
            else -> listaParaExibir
        }
    }

    var mostrarDialogoExclusao by remember { mutableStateOf(false) }
    var lancamentoParaExcluir by remember { mutableStateOf<Lancamento?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seu Histórico", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            // FILTROS
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedFilterChip(
                    selected = filtroSelecionado == "todos",
                    onClick = { filtroSelecionado = "todos" },
                    label = { Text("Tudo") }
                )
                ElevatedFilterChip(
                    selected = filtroSelecionado == "entrada",
                    onClick = { filtroSelecionado = "entrada" },
                    label = { Text("Entradas") }
                )
                ElevatedFilterChip(
                    selected = filtroSelecionado == "saida",
                    onClick = { filtroSelecionado = "saida" },
                    label = { Text("Saídas") }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (estaCarregando) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (listaFiltrada.isEmpty()) {
                    Text("Nenhum registro encontrado.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listaFiltrada) { lanc ->
                            ItemLancamentoCard(lanc,
                                onEdit = {
                                    val intent = Intent(contexto, EditarLancamentoActivity::class.java)
                                    intent.putExtra("lancamento_id", lanc.id)
                                    contexto.startActivity(intent)
                                },
                                onDelete = {
                                    lancamentoParaExcluir = lanc
                                    mostrarDialogoExclusao = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoExclusao = false },
            title = { Text("Excluir?") },
            text = { Text("Deseja apagar '${lancamentoParaExcluir?.descricao}'?") },
            confirmButton = {
                TextButton(onClick = {
                    lancamentoParaExcluir?.let { activity?.deletarItem(it.id) }
                    mostrarDialogoExclusao = false
                }) { Text("Excluir", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoExclusao = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ItemLancamentoCard(lancamento: Lancamento, onEdit: () -> Unit, onDelete: () -> Unit) {
    val corTipo = if (lancamento.tipo.lowercase() == "entrada") Color(0xFF2E7D32) else Color(0xFFC62828)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.width(4.dp).height(40.dp), color = corTipo, shape = RoundedCornerShape(2.dp)) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(lancamento.descricao, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(lancamento.data, fontSize = 12.sp, color = Color.DarkGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("R$ %.2f".format(lancamento.valor), color = corTipo, fontWeight = FontWeight.ExtraBold)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = Color.Gray) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = Color(0xFFC62828)) }
                }
            }
        }
    }
}