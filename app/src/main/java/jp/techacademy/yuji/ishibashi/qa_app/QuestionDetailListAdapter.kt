package jp.techacademy.yuji.ishibashi.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jp.techacademy.yuji.ishibashi.qa_app.databinding.ListAnswerBinding
import jp.techacademy.yuji.ishibashi.qa_app.databinding.ListQuestionDetailBinding

class QuestionDetailListAdapter(context: Context, private val mQustion: Question) : BaseAdapter() {
    private val TAG: String = "QuestionDetailListAdapter"

    private lateinit var binding_question_detail: ListQuestionDetailBinding
    private lateinit var binding_answer: ListAnswerBinding

    companion object {
        private val TYPE_QUESTION = 0
        private val TYPE_ANSWER = 1
    }

    private var mLayoutInflater: LayoutInflater? = null

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return 1 + mQustion.answers.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Any {
        return mQustion
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view

        if (getItemViewType(position) == TYPE_QUESTION) {
            if (convertView == null) {
                //convertView = mLayoutInflater!!.inflate(R.layout.list_question_detail, parent, false)!!
                binding_question_detail = ListQuestionDetailBinding.inflate(mLayoutInflater!!)
                convertView = binding_question_detail.root
            }
            val body = mQustion.body
            val name = mQustion.name

            val bodyTextView = binding_question_detail.bodyTextView
            bodyTextView.text = body

            val nameTextView = binding_question_detail.nameTextView
            nameTextView.text = name

            val bytes = mQustion.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                val imageView = binding_question_detail.imageView
                imageView.setImageBitmap(image)
            }

            var isFavorite = mQustion.favorite
            val favoriteImageView = binding_question_detail.favoriteImageView
            favoriteImageView.apply {
                setImageResource(if (isFavorite) R.drawable.ic_favorite_star else R.drawable.ic_favorite_star_border)
                setOnClickListener {
                    val userId = FirebaseAuth.getInstance().currentUser!!.uid
                    if(isFavorite) {
                        Log.d(TAG, "delete favorite start")
                        //お気に入りデータを削除
                        FirebaseFirestore.getInstance().collection(FavoritePATH)
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("questionId", mQustion.questionUid)
                            .addSnapshotListener {querySnapshot, firebaseFirestoreException ->
                                if (firebaseFirestoreException != null) {
                                    // 取得エラー
                                    Log.d(TAG, "firebaseFirestoreException")
                                    return@addSnapshotListener
                                }
                                querySnapshot!!.documents.forEach {
                                    Log.d(TAG, "favorite delete")
                                    it.reference.delete()
                                }
                            }
                    } else {
                        Log.d(TAG, "add favorite start")
                        //お気に入りデータを追加
                        // FirestoreQuestionのインスタンスを作成し、値を詰めていく
                        var firestoreFavoriteQuestion = FirestoreFavoriteQuestion()
                        firestoreFavoriteQuestion.userId = userId
                        firestoreFavoriteQuestion.questionId = mQustion.questionUid

                        FirebaseFirestore.getInstance()
                            .collection(FavoritePATH)
                            .document(firestoreFavoriteQuestion.id)
                            .set(firestoreFavoriteQuestion)
                            .addOnSuccessListener {
                                Log.d(TAG, "favorite add success")
                                Toast.makeText(context, "お気に入り登録に成功しました", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener {
                                Log.d(TAG, "favorite add failure")
                                it.printStackTrace()
                                Toast.makeText(context, "お気に入り登録に失敗しました", Toast.LENGTH_LONG).show()
                            }
                    }

                    //お気に入りフラグを変更
                    mQustion.favorite = !mQustion.favorite
                    notifyDataSetChanged()
                }
            }
        } else {
            if (convertView == null) {
                //convertView = mLayoutInflater!!.inflate(R.layout.list_answer, parent, false)!!
                binding_answer = ListAnswerBinding.inflate(mLayoutInflater!!)
                convertView = binding_answer.root
            }

            val answer = mQustion.answers[position - 1]
            val body = answer.body
            val name = answer.name

            val bodyTextView = binding_answer.bodyTextView
            bodyTextView.text = body

            val nameTextView = binding_answer.nameTextView
            nameTextView.text = name
        }

        return convertView
    }
}