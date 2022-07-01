package com.example.bookshelfappkotlin

import android.app.Application
import android.content.ContentValues.TAG
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import java.util.*

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object {

        //static method to convert timestamp to proper date format
        fun formatTimestamp(timestamp: String) : String {
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp.toLong()
            //format dd/mm/yyyy
            return DateFormat.format("dd/mm/yyyy", cal).toString()
        }

        //function to get pdf size
        fun loadPdfSize(pdfUrl: String, pdfTitle: String, sizeTv: TextView) {
            val TAG = "PDF_SIZE_TAG"
            //using url we can get file and it's metadata from firebase storage
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.metadata
                .addOnSuccessListener { storageMetadata ->
                    Log.d(TAG, "loadPdfSize: get metadata")
                    val bytes = storageMetadata.sizeBytes.toDouble()
                    Log.d(TAG, "loadPdfSize: size bytes $bytes")

                    //convert bytes to KB/MB
                    val kb = bytes/1024
                    val mb = kb/1024
                    if (mb>1) {
                        sizeTv.text = "${String.format("%.2f", mb)} MB"
                    } else if (kb>=1) {
                        sizeTv.text = "${String.format("%.2f", kb)} KB"
                    } else {
                        sizeTv.text = "${String.format("%.2f", bytes)} bytes"
                    }
                }
                .addOnFailureListener { e ->
                //failed to get metadata
                Log.d(TAG, "loadPdfSize: Failed to get metadata due to ${e.message}")
                }
        }

        fun loadPdfFromUrlSinglePage(
            pdfUrl: String,
            pdfTitle: String,
            pdfView: PDFView,
            progressBar: ProgressBar,
            pagesTv: TextView?
        ){
            val TAG = "PDF_THUMBNAIL_TAG"
            //using url we can get file and it's metadata for firebase storage
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener { bytes ->
                    Log.d(TAG, "loadPdfSize: size bytes $bytes")

                    //set to pdf view
                    pdfView.fromBytes(bytes)
                        .pages(0) //show first page only
                        .spacing(0)
                        .swipeHorizontal(false)
                        .enableSwipe(false)
                        .onError { t ->
                            progressBar.visibility = View.INVISIBLE
                            Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onPageError { page, t ->
                            Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onLoad { nbPages ->
                            Log.d(TAG, "loadPdfFromUrlSinglePage: Pages: $nbPages")
                            //pdf loaded, we can set page count, pdf thumbnail
                            progressBar.visibility = View.INVISIBLE

                            //if pagesTv param is not null then set page numbers
                            if (pagesTv != null) {
                                pagesTv.text = "$nbPages"
                            }
                        }
                        .load()
                }
                .addOnFailureListener { e ->
                    //failed to get metadata
                    Log.d(TAG, "loadPdfSize: Failed to get metadata due to ${e.message}")

                }
        }

        fun loadCategory(categoryId: String, categoryTv: TextView) {
            //load category using category if from firebase
            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get category
                        val category: String = "${snapshot.child("").value}"

                        //set category
                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
    }
}
