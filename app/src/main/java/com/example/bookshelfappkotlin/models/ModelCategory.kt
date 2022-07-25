package com.example.bookshelfappkotlin.models

class ModelCategory {

    //variables, must match as in firebase
    var id: String = ""
    var category: String = ""
    var timestamp: String = ""
    var uid: String = ""

    //empty constructor, required by firebase
    constructor()

    //parameterized constructor
    constructor(id: String, category: String, timestamp: String, uid: String) {
        this.id = id
        this.category = category
        this.timestamp = timestamp
        this.uid = uid
    }

}
