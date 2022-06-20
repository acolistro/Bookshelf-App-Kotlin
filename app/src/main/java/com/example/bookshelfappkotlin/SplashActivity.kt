package com.example.bookshelfappkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SplashActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()

        //Show splash for 2 seconds before jumping to main activity
        Handler().postDelayed( {
            checkUser()
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
        }, 1000)
    }

    private fun checkUser() {
        //get current user, if logged in or not
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            //user not logged in, go to main screen
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // user logged in, check user type, same as done in login screen
            val firebaseUser = firebaseAuth.currentUser!!
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get user type user/admin
                        val userType = snapshot.child("userType").value
                        if (userType == "user") {
                            // regular user, open user dashboard
                            startActivity(Intent(this@SplashActivity, DashboardUserActivity::class.java))
                            finish()
                        } else if (userType == "admin") {
                            // admin status user, open admin dashboard
                            startActivity(Intent(this@SplashActivity, DashboardAdminActivity::class.java))
                            finish()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
    }
}
/* Keep user logged in
* 1) Check if user is logged in
* 2) Check type of user
* */
