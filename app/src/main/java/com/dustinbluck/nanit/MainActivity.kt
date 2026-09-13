package com.dustinbluck.nanit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.dustinbluck.nanit.ui.theme.NanitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanitTheme {
                val backStack = remember { mutableStateListOf<Screen>(Screen.Main) }
                NavDisplay(backStack = backStack, onBack = { backStack.removeLastOrNull() }) { key ->
                    NavEntry(key) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when (key) {
                                Screen.Main -> Button(onClick = { backStack.add(Screen.Birthday) }) { Text("Birthday") }
                                Screen.Birthday -> Text("Birthday")
                            }
                        }
                    }
                }
            }
        }
    }
}
