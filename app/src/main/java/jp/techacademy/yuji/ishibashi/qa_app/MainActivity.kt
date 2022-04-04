package jp.techacademy.yuji.ishibashi.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import jp.techacademy.yuji.ishibashi.qa_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {    // ← 修正
    private val TAG: String = "MainActivity"

    private lateinit var binding: ActivityMainBinding

    private var mGenre = 0    // ← 追加

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate start")
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)

        // idがtoolbarがインポート宣言により取得されているので
        // id名でActionBarのサポートを依頼
        setSupportActionBar(binding.appBarMain.toolbar)

        // fabにClickリスナーを登録
        binding.appBarMain.fab.setOnClickListener { view ->
            // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
            if (mGenre == 0) {
                Snackbar.make(
                    view,
                    getString(R.string.question_no_select_genre),
                    Snackbar.LENGTH_LONG
                ).show()
            } else {

            }
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.appBarMain.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        binding.appBarMain.contentMain.listView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 1:趣味を既定の選択とする
        if(mGenre == 0) {
            onNavigationItemSelected(binding.navView.menu.getItem(0))
        }
    }
    // --- ここまで追加する ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_hobby) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        } else if (id == R.id.nav_life) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        } else if (id == R.id.nav_health) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        } else if (id == R.id.nav_compter) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_compter_label)
            mGenre = 4
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        // --- ここから ---
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        binding.appBarMain.contentMain.listView.adapter = mAdapter

        // 一つ前のリスナーを消す
        snapshotListener?.remove()

        // 選択したジャンルにリスナーを登録する
        snapshotListener = FirebaseFirestore.getInstance()
            .collection(ContentsPATH)
            .whereEqualTo("genre", mGenre)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    // 取得エラー
                    return@addSnapshotListener
                }
                Log.d(TAG, "addSnapshotListener start")
                var questions = listOf<Question>()
                val results = querySnapshot?.toObjects(FirestoreQuestion::class.java)
                results?.also {
                    questions = it.map { firestoreQuestion ->
                        val bytes =
                            if (firestoreQuestion.image.isNotEmpty()) {
                                Base64.decode(firestoreQuestion.image, Base64.DEFAULT)
                            } else {
                                byteArrayOf()
                            }
                        Log.d(TAG, "id : " + firestoreQuestion.id)
                        Log.d(TAG, "title : " + firestoreQuestion.title)
                        Question(firestoreQuestion.title, firestoreQuestion.body, firestoreQuestion.name, firestoreQuestion.uid,
                            firestoreQuestion.id, firestoreQuestion.genre, bytes, firestoreQuestion.answers)
                    }
                }

                val userId = FirebaseAuth.getInstance().currentUser!!.uid
                questions.forEach {
                    FirebaseFirestore.getInstance().collection(FavoritePATH)
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("questionId", it.questionUid)
                        .addSnapshotListener {querySnapshot, firebaseFirestoreException ->
                            if (firebaseFirestoreException != null) {
                                // 取得エラー
                                return@addSnapshotListener
                            }
                            it.favorite = querySnapshot?.isEmpty!!
                        }
                }

                mQuestionArrayList.clear()
                mQuestionArrayList.addAll(questions)
                mAdapter.notifyDataSetChanged()
            }
        return true
    }

//    private val mEventListener = object : ChildEventListener {
//        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
//            val map = dataSnapshot.value as Map<String, String>
//            val title = map["title"] ?: ""
//            val body = map["body"] ?: ""
//            val name = map["name"] ?: ""
//            val uid = map["uid"] ?: ""
//            val imageString = map["image"] ?: ""
//            val bytes =
//                if (imageString.isNotEmpty()) {
//                    Base64.decode(imageString, Base64.DEFAULT)
//                } else {
//                    byteArrayOf()
//                }
//
//            val answerArrayList = ArrayList<Answer>()
//            val answerMap = map["answers"] as Map<String, String>?
//            if (answerMap != null) {
//                for (key in answerMap.keys) {
//                    val temp = answerMap[key] as Map<String, String>
//                    val answerBody = temp["body"] ?: ""
//                    val answerName = temp["name"] ?: ""
//                    val answerUid = temp["uid"] ?: ""
//                    val answer = Answer(answerBody, answerName, answerUid, key)
//                    answerArrayList.add(answer)
//                }
//            }
//
//            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
//                mGenre, bytes, answerArrayList)
//            mQuestionArrayList.add(question)
//            mAdapter.notifyDataSetChanged()
//        }
//
//        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
//            val map = dataSnapshot.value as Map<String, String>
//
//            // 変更があったQuestionを探す
//            for (question in mQuestionArrayList) {
//                if (dataSnapshot.key.equals(question.questionUid)) {
//                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
//                    question.answers.clear()
//                    val answerMap = map["answers"] as Map<String, String>?
//                    if (answerMap != null) {
//                        for (key in answerMap.keys) {
//                            val temp = answerMap[key] as Map<String, String>
//                            val answerBody = temp["body"] ?: ""
//                            val answerName = temp["name"] ?: ""
//                            val answerUid = temp["uid"] ?: ""
//                            val answer = Answer(answerBody, answerName, answerUid, key)
//                            question.answers.add(answer)
//                        }
//                    }
//
//                    mAdapter.notifyDataSetChanged()
//                }
//            }
//        }
//
//        override fun onChildRemoved(p0: DataSnapshot) {
//
//        }
//
//        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
//
//        }
//
//        override fun onCancelled(p0: DatabaseError) {
//
//        }
//    }
}