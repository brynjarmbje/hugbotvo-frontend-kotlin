package com.mytestwork2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The layout contains the NavHostFragment referencing nav_graph
        setContentView(R.layout.activity_main)
    }
}
