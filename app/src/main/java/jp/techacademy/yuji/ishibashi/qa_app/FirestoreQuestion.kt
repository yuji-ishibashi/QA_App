package jp.techacademy.yuji.ishibashi.qa_app

import java.util.*
import kotlin.collections.ArrayList

class FirestoreQuestion {
    var id = UUID.randomUUID().toString()
    var title = ""
    var body = ""
    var name = ""
    var uid = ""
    var image = ""
    var genre = 0
    var answers: ArrayList<Answer> = arrayListOf()
}