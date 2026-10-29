package com.hfad.atomgit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hfad.atomgit.navigation.AtomGitNavHost
import com.hfad.atomgit.ui.theme.AtomGitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtomGitTheme {
                AtomGitNavHost(onExit = { finish() })
            }
        }
    }
}
