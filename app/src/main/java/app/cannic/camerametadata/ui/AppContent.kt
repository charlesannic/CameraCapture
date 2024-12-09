package app.cannic.camerametadata.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cannic.camerametadata.ui.home.HomeContent


@Composable
fun AppContent() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeContent()
        }
    }
}