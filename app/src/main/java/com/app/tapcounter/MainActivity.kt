package com.app.tapcounter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.tapcounter.ui.theme.TapCounterTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val viewModel: CounterViewModel by viewModels()
		viewModel.updateCount(getPref(PrefKey.COUNT).toIntOrDef())
		
		enableEdgeToEdge()
		setContent {
			TapCounterTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					val vm: CounterViewModel = viewModel()
					val count by vm.currentCount.observeAsState()
					var data by rememberSaveable { mutableStateOf(listOf("")) }
					
					LaunchedEffect (data) {
						setPref(data.toString(), PrefKey.DATA)
					}
					LaunchedEffect (count) {
						setPref(count.toString(), PrefKey.COUNT)
					}
					CounterApp( Modifier.padding(innerPadding), count ?: 0,
						{
							vm.incrementCount()
							data = vm.getNewData(count ?: 0, data)
						},
						{ vm.incrementCount(-1)},
						{
							vm.updateCount(0)
							data = emptyList()
						},
						{copyToClipboard(data.toString())},
						data
					)
				}
			}
		}
	}
	

	
	private fun copyToClipboard(data: String){
		val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		val clip: ClipData = ClipData.newPlainText("count data", data)
		clipboard.setPrimaryClip(clip)
	}
	
	private fun setPref(value: String, key: PrefKey){
		val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
		with (sharedPref.edit()) {
			putString(key.toString(), value)
			apply()
		}
	}
	
	private fun getPref(key: PrefKey): String {
		val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return ""
		return sharedPref.getString(key.toString(), "") ?: ""
	}
	
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
	Text(
		text = "Hello $name!",
		modifier = modifier
	)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
	TapCounterTheme {
		Greeting("Android")
	}
}

@Preview(showBackground = true)
@Composable
private fun CounterAppPreview() {
	CounterApp(Modifier, 5, {}, {}, {}, {}, listOf("First, 00:01"))
}

@Composable
fun CounterApp(
	modifier: Modifier = Modifier,
	value: Int = 0,
	onClick: ()-> Unit = {},
	minusClick: () -> Unit = {},
	clearAllClick: () -> Unit,
	copyClick: () -> Unit,
	data: List<String>
) {
	var showMinusDialog by remember { mutableStateOf(false) }
	var showClearAllDialog by remember { mutableStateOf(false) }
	if (showMinusDialog) AlertDialogExample( {showMinusDialog = false}, minusClick, "Reduce Count")
	if (showClearAllDialog) AlertDialogExample( {showClearAllDialog = false}, clearAllClick, "Clear all")
	Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
		Text(text = value.toString(), fontSize = 50.sp, fontWeight = FontWeight(500))
		Spacer(modifier = Modifier.height(30.dp))
		Button(modifier = Modifier
			.fillMaxWidth(0.8f)
			.height(100.dp), onClick = onClick){
			Text(text = "Click here", fontSize = 20.sp)
		}
		Spacer(modifier = Modifier.height(30.dp))
		Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
			Button(onClick = copyClick){
				Text(text = "Copy", fontSize = 20.sp)
			}
			Button(onClick = { if ( value > 0) showMinusDialog = true}){
				Text(text = "Minus", fontSize = 20.sp)
			}
			Button(onClick = {showClearAllDialog = true}){
				Text(text = "Clear", fontSize = 20.sp)
			}
			
		}
		Spacer(modifier = Modifier.height(30.dp))
		CounterTable(data = data)
	}
}

@Composable
fun CounterTable(
	data: List<String>
){
	LazyColumn {
		items(data) {
			Text(it)
		}
	}

}

@Composable
fun AlertDialogExample(
	onDismissRequest: () -> Unit,
	onConfirmation: () -> Unit,
	dialogTitle: String
) {
	AlertDialog(
		title = {
			Text(text = dialogTitle)
		},
		onDismissRequest = {
			onDismissRequest()
		},
		confirmButton = {
			TextButton(
				onClick = {
					onConfirmation()
					onDismissRequest()
				}
			) {
				Text("Yes")
			}
		}
	)
}

@HiltViewModel
class CounterViewModel @Inject constructor() : ViewModel(){
	var currentCount = MutableLiveData(0)
		private set
	
	fun updateCount(value: Int){
		currentCount.value = value
	}
	
	fun incrementCount(inc: Int? = null){
		currentCount.value = (currentCount.value ?: 0) + (inc ?: 1)
	}
	
	fun getNewData(count: Int, oldData: List<String>): List<String> {
		val time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))
		return listOf("$count: $time") + oldData
	}
	
}

enum class PrefKey{
	COUNT,
	DATA
}

fun String.toIntOrDef(default: Int = 0): Int {
	return this.toIntOrNull() ?: default
}