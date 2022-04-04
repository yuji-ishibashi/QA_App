package jp.techacademy.yuji.ishibashi.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import jp.techacademy.yuji.ishibashi.qa_app.databinding.ListQuestionsBinding

class QuestionsListAdapter(context: Context) : BaseAdapter() {

    private lateinit var binding: ListQuestionsBinding

    private var mLayoutInflater: LayoutInflater
    private var mQuestionArrayList = ArrayList<Question>()

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return mQuestionArrayList.size
    }

    override fun getItem(position: Int): Any {
        return mQuestionArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view

        if (convertView == null) {
            //convertView = mLayoutInflater.inflate(R.layout.list_questions, parent, false)
            binding = ListQuestionsBinding.inflate(mLayoutInflater)
            convertView = binding.root
        }

        val titleText = binding.titleTextView as TextView
        titleText.text = mQuestionArrayList[position].title

        val nameText = binding.nameTextView as TextView
        nameText.text = mQuestionArrayList[position].name

        val resText = binding.resTextView as TextView
        val resNum = mQuestionArrayList[position].answers.size
        resText.text = resNum.toString()

        val bytes = mQuestionArrayList[position].imageBytes
        if (bytes.isNotEmpty()) {
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
            val imageView = binding.imageView as ImageView
            imageView.setImageBitmap(image)
        }

        val isFavorite = mQuestionArrayList[position].favorite
        val favoriteImageView = binding.favoriteImageView
        favoriteImageView.setImageResource(if (isFavorite) R.drawable.ic_favorite_star else R.drawable.ic_favorite_star_border)

        return convertView
    }

    fun setQuestionArrayList(questionArrayList: ArrayList<Question>) {
        mQuestionArrayList = questionArrayList
    }
}