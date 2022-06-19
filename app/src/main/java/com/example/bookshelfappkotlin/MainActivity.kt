package com.example.bookshelfappkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bookshelfappkotlin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    //View binding
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Handle click, login
        binding.loginBtn.setOnClickListener {
            //tbd
        }

        binding.skipBtn.setOnClickListener {
            //tbd
        }


    }
}
