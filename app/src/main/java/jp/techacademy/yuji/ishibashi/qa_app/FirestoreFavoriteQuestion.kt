package jp.techacademy.yuji.ishibashi.qa_app

import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class FirestoreFavoriteQuestion {
    var id = UUID.randomUUID().toString()
    var userId = ""
    var questionId = ""
}