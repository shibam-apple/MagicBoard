package com.doubtboard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.doubtboard.app.ui.DoubtBoardRoot
import com.doubtboard.app.ui.theme.DoubtBoardTheme
import dagger.hilt.android.AndroidEntryPoint

/** The single activity. Navigation between screens happens inside Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoubtBoardTheme {
                DoubtBoardRoot()
            }
        }
    }
}
