package jp.techacademy.yuji.ishibashi.qa_app

import java.io.Serializable
import java.util.ArrayList

class Question(val title: String, val body: String, val name: String, val uid: String, val questionUid: String, val genre: Int, bytes: ByteArray, val answers: ArrayList<Answer>) : Serializable {
    val imageBytes: ByteArray
    var favorite: Boolean = false

    init {
        imageBytes = bytes.clone()
    }
}
