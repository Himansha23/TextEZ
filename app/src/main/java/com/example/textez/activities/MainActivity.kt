package com.example.textez.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.textez.R
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardNew = findViewById<MaterialCardView>(R.id.cardNew)
        val cardOpen = findViewById<MaterialCardView>(R.id.cardOpen)

        cardNew.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    EditorActivity::class.java
                )
            )
        }

        cardOpen.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    OpenFileActivity::class.java
                )
            )
        }
    }
}