package com.example.bookshelfappkotlin.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bookshelfappkotlin.Constants
import com.example.bookshelfappkotlin.MyApplication
import com.example.bookshelfappkotlin.MyApplication.Companion.incrementBookViewCount
import com.example.bookshelfappkotlin.R
import com.example.bookshelfappkotlin.databinding.ActivityPdfDetailBinding
import com.example.bookshelfappkotlin.databinding.DialogCommentAddBinding
import com.example.bookshelfappkotlin.models.ModelComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream
import java.lang.Exception

class PdfDetailActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding: ActivityPdfDetailBinding

    private companion object {
        //TAG
        const val TAG = "BOOK_DETAILS_TAG"
    }

    //book id, get from intent
    private var bookId = ""
    //get from firebase
    private var bookTitle = ""
    private var bookUrl = ""

    //Will hold a bool value to indicate either in favs or not of current user
    private var isInMyFavorites = false

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    //arrayList to cold comments
    private lateinit var commentArrayList: ArrayList<ModelComment>
    //adapter to be set to recyclerview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get book id from intent
        bookId = intent.getStringExtra("bookId")!!

        //init progress bar
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null) {
            //user is logged in, check if book is in fav or not
            checkIsFavorite()
        }

        //increment book view count whenever this screen starts
        incrementBookViewCount(bookId)

        loadBookDetails()
        showComments()

        //handle back button click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, open pdf view activity
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }

        //handle click, download book/pdf
        binding.downloadBookBtn.setOnClickListener {
            //Check WRITE_EXTERNAL_STORAGE permission first, if granted download book, if not granted request permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: STORAGE PERMISSION is already granted")
                downloadBook()
            } else {
                Log.d(TAG, "onCreate: STORAGE PERMISSION was not granted, must be requested")
                requestStoragePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        //handle click, add/remove favorite
        binding.favoriteBtn.setOnClickListener {
            //We can add only if user is logged in
            //1)Check if user is logged in or not
            if (firebaseAuth.currentUser == null) {
                //user not logged in, can't do favorite functionality
                Toast.makeText(this, "You're not logged in", Toast.LENGTH_SHORT).show()
            } else {
                //user is logged in, favorite functionality available
                if (isInMyFavorites) {
                    //Already in favorites, remove
                    MyApplication.removeFromFavorites(this, bookId)
                } else {
                    //Not in favorites, add
                    addToFavorites()
                }
            }
        }

        //handle click, show add comment dialog
        binding.addCommentBtn.setOnClickListener {
            /*To add a comment, user must be logged in, if not just show a message 'You're not logged in'*/
            if (firebaseAuth.currentUser == null) {
                //user not logged in, don't allow adding comment
                Toast.makeText(this, "You're not logged in", Toast.LENGTH_SHORT).show()
            } else {
                //user logged in, allow adding comment
                addCommentDialog()
            }
        }

    }

    private fun showComments() {

    }

    private var comment = ""

    private fun addCommentDialog() {
        //inflate/bind view for dialog dialog_comment_add.xml
        val commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this))

        //set up alert dialog
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(commentAddBinding.root)

        //create and show alert dialog
        val alertDialog = builder.create()
        alertDialog.show()

        //handle click, dismiss dialog
        commentAddBinding.backBtn.setOnClickListener {
            alertDialog.dismiss()
        }

        //handle click, add comment
        commentAddBinding.submitBtn.setOnClickListener {
            //get data
            comment = commentAddBinding.commentEt.text.toString().trim()
            //validate data
            if (comment.isEmpty()) {
                Toast.makeText(this, "Enter comment..", Toast.LENGTH_SHORT).show()
            } else {
                alertDialog.dismiss()
                addComment()
            }

        }
    }

    private fun addComment() {
        //show progress
        progressDialog.setMessage("Adding Comment")
        progressDialog.show()

        //timestamp for comment id, comment timestamp etc
        val timestamp = "" + System.currentTimeMillis()

        //set up data to add in db for comment
        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["bookId"] = "$bookId"
        hashMap["timestamp"] = "$timestamp"
        hashMap["comment"] = "$comment"
        hashMap["uid"] = "${firebaseAuth.uid}"

        //Db path to add data into it
        //Books > bookId > Comments > commentId > commentData
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Comment added...", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to add comment due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted:Boolean ->
        //check if granted or not
        if (isGranted) {
            Log.d(TAG, "onCreate: STORAGE PERMISSION is already granted")
            downloadBook()
        } else {
            Log.d(TAG, "onCreate: STORAGE PERMISSION is denied")
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadBook() {
        Log.d(TAG, "downloadBook: Downloading book...")
        progressDialog.setMessage("Downloading Book")
        progressDialog.show()

        //download book from firebase storage using url
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "downloadBook: Book downloaded...")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.d(TAG, "downloadBook: Failed to download book due to ${e.message}")
                Toast.makeText(this, "Failed to download book due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray) {
        Log.d(TAG, "saveToDownloadsFolder: saving downloaded book...")

        val nameWithExtension = "$bookTitle"
        //val nameWithExtension = "${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs() //create folder if it doesn't exist

            val filePath = downloadsFolder.path + "/" + nameWithExtension

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this, "Saved to Downloads Folder", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "saveToDownloadsFolder: Saved to Downloads Folder")
            progressDialog.dismiss()
            incrementDownloadCount()
        }
        catch (e: Exception) {
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadsFolder: failed to save due to ${e.message}")
            Toast.makeText(this, "Failed to save book due to ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        //increment downloads count to firebase db
        Log.d(TAG, "incrementDownloadCount: ")
        
        //Step 1) Get previous downloads count
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get downloads count
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDataChange: Current Downloads Count: $downloadsCount")
                    
                    if (downloadsCount == "" || downloadsCount == "null") {
                        downloadsCount = "0"
                    }
                    
                    //convert to long and increment 1
                    val newDownloadCount: Long = downloadsCount.toLong() + 1
                    Log.d(TAG, "onDataChange: New Downloads Count: $newDownloadCount")
                    
                    //setup data to update to db
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadCount
                    
                    //Step 2) Update new incremented downloads count to db
                    val dbref = FirebaseDatabase.getInstance().getReference("Books")
                    dbref.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Downloads count incremented")
                        }
                        .addOnFailureListener { e ->
                            Log.d(TAG, "onDataChange: FAILED to increment due to ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    
                }
            })
    }

    private fun loadBookDetails() {
        //Books > bookId > Details
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp)

                    //load pdf category
                    MyApplication.loadCategory(categoryId, binding.categoryTv)
                    //load pdf thumbnail, pages count
                    MyApplication.loadPdfFromUrlSinglePage("$bookUrl", "$bookTitle", binding.pdfView, binding.progressBar, binding.pagesTv)
                    //load pdf size
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    //set data
                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: Checking if book is in fav or not")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorites = snapshot.exists()
                    if (isInMyFavorites) {
                        //Available in favorites
                        Log.d(TAG, "onDataChange: Available on favorites")
                            //Set drawable top icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_filled_white, 0, 0)
                        binding.favoriteBtn.text = "Remove Favorite"
                    } else {
                        //Not available in favorites
                        Log.d(TAG, "onDataChange: Not available in favorites")
                            //Set drawable top icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_border_white, 0, 0)
                        binding.favoriteBtn.text = "Add to Favorite"
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun addToFavorites() {
        Log.d(TAG, "addToFavorites: Adding to favorites")
        val timestamp = System.currentTimeMillis()

        //set up data to add in db
        val hashMap = HashMap<String, Any>()
        hashMap["bookId"] = bookId
        hashMap["timestamp"] = timestamp

        //save to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                //added to favorites
                Log.d(TAG, "addToFavorites: Added to favorites")
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                //failed to add to favorites
                Log.d(TAG, "addToFavorites: Failed to add to favorites due to ${e.message}")
                Toast.makeText(this, "Failed to add to favorites due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
