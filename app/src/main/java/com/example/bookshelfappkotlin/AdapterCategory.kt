package com.example.bookshelfappkotlin

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshelfappkotlin.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterCategory : RecyclerView.Adapter<AdapterCategory.ViewHolderCategory> {

    private val context: Context
    private val categoryArrayList: ArrayList<ModelCategory>

    private lateinit var binding: RowCategoryBinding

    //constructor
    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCategory {
        //inflate/bind row_category.xml
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)

        return ViewHolderCategory(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolderCategory, position: Int) {
        /*--------Get data, set data, handle clicks--------*/

        //get data
        val model = categoryArrayList[position]
        val id = model.id
        val category = model.category
        val uid = model.uid
        val timestamp = model.timestamp

        //set data
        holder.categoryTv.text = category

        //handle clicks, delete category
        holder.deleteBtn.setOnClickListener {
            //confirm before delete
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete")
                .setMessage("Are you sure you want to delete this category?")
                .setPositiveButton("Confirm") { a, d ->
                    Toast.makeText(context, "Deleting...", Toast.LENGTH_SHORT).show()
                    deleteCategory(model, holder)
                }
                .setNegativeButton("Cancel") { a, d ->
                    a.dismiss()
                }
        }
    }

    private fun deleteCategory(model: ModelCategory, holder: ViewHolderCategory) {
        //get id of category to delete
        val id = model.id
        //Firebase DB > Categories > categoryId
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted...", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Unable to delete due to ${e.message}...", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int {
        return categoryArrayList.size //number of items in list
    }

    //ViewHolder class to hold/init UI views for row_category.xml
    inner class ViewHolderCategory(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //init ui views
        var categoryTv: TextView = binding.categoryTv
        var deleteBtn: ImageButton = binding.deleteBtn
    }

}
