package com.example.bookshelfappkotlin

import android.widget.Filter
import com.example.bookshelfappkotlin.adapters.AdapterPDFUser
import com.example.bookshelfappkotlin.model.ModelPdf

class FilterPdfUser: Filter {

    //arrayList in which we want to search
    var filterList: ArrayList<ModelPdf>

    //adapter in which filter needs to be implemented
    var adapterPdfUser: AdapterPDFUser

    //constructor
    constructor(filterList: ArrayList<ModelPdf>, adapterPdfUser: AdapterPDFUser) : super() {
        this.filterList = filterList
        this.adapterPdfUser = adapterPdfUser
    }

    override fun performFiltering(constraint: CharSequence): FilterResults {
        var constraint: CharSequence? = constraint

        val results = FilterResults()
        //value to be searched should not be null and not be empty
        if (constraint != null && constraint.isNotEmpty()) {
            //not null or empty

            //change to uppercase to remove case sensitivity
            constraint = constraint.toString().uppercase()
            val filteredModels = ArrayList<ModelPdf>()
            for (i in filterList.indices) {
                //validate if match
                if (filterList[i].title.uppercase().contains(constraint)) {
                    //searched value matches with title, aadd to list
                    filteredModels.add(filterList[i])
                }
            }
            //return filtered list and size
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            //either it is null or empty
            //return original list and size
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        //apply filter changes
        adapterPdfUser.pdfArrayList = results.values as ArrayList<ModelPdf>

        //notify changes
        adapterPdfUser.notifyDataSetChanged()
    }
}
