package com.example.bookshelfappkotlin.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.bookshelfappkotlin.MyApplication
import com.example.bookshelfappkotlin.R
import com.example.bookshelfappkotlin.adapters.AdapterPdfFavorite
import com.example.bookshelfappkotlin.databinding.ActivityProfileBinding
import com.example.bookshelfappkotlin.models.ModelPdf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding: ActivityProfileBinding

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    //Firebase current user
    private lateinit var firebaseUser: FirebaseUser

    //arrayList to hold books
    private lateinit var booksArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfFavorite: AdapterPdfFavorite

    //progress Dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //reset to default values
        binding.accountTypeTv.text = "N/A"
        binding.memberDateTv.text = "N/A"
        binding.favoriteBookCountTv.text = "N/A"
        binding.accountStatusTv.text = "N/A"

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        //init/set up progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...!")
        progressDialog.setCanceledOnTouchOutside(false)

        loadUserInfo()
        loadFavoriteBooks()

        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, open edit profile
        binding.profileEditBtn.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }

        //handle click, verify user if not
        binding.accountStatusTv.setOnClickListener {
            if (firebaseUser.isEmailVerified) {
                //User is verified
                Toast.makeText(this, "Already verified...", Toast.LENGTH_SHORT).show()
            } else {
                //user isn't verified, show confirmation dialog before verification
                emailVerificationDialog()
            }
        }
    }

    private fun emailVerificationDialog() {
        //show confirmation dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verify Email")
            .setMessage("Are you sure you want to send email verification instructions to ${firebaseUser.email}")
            .setPositiveButton("SEND") { d, e ->
                sendEmailVerification()
            }
            .setNegativeButton("CANCEL") { d, e ->
                d.dismiss()
            }
            .show()
    }

    private fun sendEmailVerification() {
        //show progress dialog
        progressDialog.setMessage("Sending email verification instructions to ${firebaseUser.email}")
        progressDialog.show()

        //send instructions
        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                //successfully sent
                progressDialog.dismiss()
                Toast.makeText(this, "Instructions sent! Check your email ${firebaseUser.email}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                //failed to send
                progressDialog.dismiss()
                Toast.makeText(this, "Instructions failed to send due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserInfo() {
        //check if user is verified or not, changes take effect after re-login when you verify
        if (firebaseUser.isEmailVerified) {
            binding.accountStatusTv.text = "Verified"
        } else {
            binding.accountStatusTv.text = "Not Verified"
        }

        //db reference to load user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    val uid = "${snapshot.child("uid").value}"
                    val userType = "${snapshot.child("userType").value}"

                    //convert timestamp to proper format
                    val formattedDate = MyApplication.formatTimeStamp(timestamp)

                    //set data
                    binding.nameTv.text = name
                    binding.emailTv.text = email
                    binding.memberDateTv.text = formattedDate
                    binding.accountTypeTv.text = userType

                    //set image
                    try {
                        Glide.with(this@ProfileActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileIv)
                    }
                    catch (e: Exception) {

                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadFavoriteBooks() {
        //init arrayList
        booksArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear arraylist before adding data
                    booksArrayList.clear()
                    for (ds in snapshot.children) {
                        //get only id of the books, we have the rest loaded in adapter class
                        val bookId = "${ds.child("bookId").value}"

                        //set to model
                        val modelPdf =  ModelPdf()
                        modelPdf.id = bookId

                        //add model to list
                        booksArrayList.add(modelPdf)
                    }
                    //set number of favorite books
                    binding.favoriteBookCountTv.text = "${booksArrayList.size}"

                    //set up adapter
                    adapterPdfFavorite = AdapterPdfFavorite(this@ProfileActivity, booksArrayList)
                    //set adapter to recyclerview
                    binding.favoriteRv.adapter = adapterPdfFavorite
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

    }
}
