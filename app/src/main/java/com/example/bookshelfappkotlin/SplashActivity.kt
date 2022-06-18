package com.example.bookshelfappkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        //Show splash for 2 seconds before jumping to main activity
        Handler().postDelayed( {
            startActivity(Intent(this, MainActivity::class.java))
        }, 2000)
    }
}
