package com.doubtboard.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

const val ROOT_TAG = "doubt_board_root"

/**
 * M0 placeholder. The camera screen (§6.1) replaces this in M5; the board renderer arrives in M1.
 */
@Composable
fun DoubtBoardRoot() {
    Scaffold(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Doubt Board", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Preview
@Composable
private fun DoubtBoardRootPreview() {
    DoubtBoardRoot()
}
