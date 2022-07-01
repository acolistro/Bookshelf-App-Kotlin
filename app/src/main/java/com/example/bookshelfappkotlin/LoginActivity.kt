package com.example.bookshelfappkotlin

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.bookshelfappkotlin.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    //View binding
    private lateinit var binding: ActivityLoginBinding

    //Firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    //Progress dialogue
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //init progress dialogue, will show while creating account | Register user
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false) // goes away when user taps outside the area

        //handle click, not have account, go to register screen
        binding.noAccountTv.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        //handle click, begin login
        binding.loginBtn.setOnClickListener {
            /*Steps
            * 1) Input data
            * 2) Validate data
            * 3) Login - Firebase Auth
            * 4) Check user type - Firebase Auth
            *   If User - move to user dashboard
            * If Admin - Move to admin dashboard
             */
            validateData()
        }
    }

    private var email = ""
    private var password = ""

    private fun validateData() {
        // 1) Input data
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()

        // 2) Validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format...", Toast.LENGTH_SHORT).show()
        } else if (password.isEmpty()) {
            Toast.makeText(this, "Please enter password...", Toast.LENGTH_SHORT).show()
        } else {
            loginUser()
        }
    }

    private fun loginUser() {
        // 3) Login - Firebase Auth

        //Show Progress
        progressDialog.setMessage("Logging in...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //login successful
                checkUser()
            }
            .addOnFailureListener { e ->
                //failed login
                Toast.makeText(this, "Login failed due to ${e.message}...", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }

    private fun checkUser() {
        /* 4) Check user type - Firebase Auth
              * If User - move to user dashboard
              * If Admin - Move to admin dashboard */
        progressDialog.setMessage("Checking User...")

        val firebaseUser = firebaseAuth.currentUser!!
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()
                    //get user type user/admin
                    val userType = snapshot.child("userType").value
                    if (userType == "user") {
                        // regular user, open user dashboard
                        startActivity(Intent(this@LoginActivity, DashboardUserActivity::class.java))
                        finish()
                    } else if (userType == "admin") {
                        // admin status user, open admin dashboard
                        startActivity(Intent(this@LoginActivity, DashboardAdminActivity::class.java))
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}
