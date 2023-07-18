package com.example.dial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Greeting(startTime = 3, endTime = 18, editType = TimeEditType.SPLIT)
            }
        }
    }
}

@Composable
fun Greeting(startTime: Int, endTime: Int, editType: TimeEditType, modifier: Modifier = Modifier) {
    DialView(start = startTime, end = endTime, editType = editType, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting(startTime = 3, endTime = 15, editType = TimeEditType.EDIT)
}