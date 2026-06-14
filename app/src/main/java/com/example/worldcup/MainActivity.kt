package com.example.worldcup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.worldcup.data.local.database.DatabaseSeeder
import com.example.worldcup.data.local.database.WorldCupDatabase
import com.example.worldcup.navigation.NavGraph
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seed the database on first launch (no-op if already populated)
        val db = WorldCupDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            DatabaseSeeder.seedIfEmpty(applicationContext, db)
        }

        setContent {
            NavGraph()
        }
    }
}