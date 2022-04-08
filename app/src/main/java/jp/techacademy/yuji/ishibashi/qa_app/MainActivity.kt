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
import com.google.android.gms.tasks.Task
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import jp.techacademy.yuji.ishibashi.qa_app.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
        } else {
            //別Activityから戻ってきた場合はログオフしている可能性があるためジャンル別の質問一覧を表示する。
            initQuestionList(false)
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

        var isFavoriteList = false

        if (id == R.id.nav_hobby) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        } else if (id == R.id.nav_life) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        } else if (id == R.id.nav_health) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        } else if (id == R.id.nav_computer) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_computer_label)
            mGenre = 4
        } else if (id == R.id.nav_favorite) {
            binding.appBarMain.toolbar.title = getString(R.string.menu_favorite_label)
            isFavoriteList = true
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        initQuestionList(isFavoriteList)

        return true
    }

    private fun initQuestionList(isFavoriteList: Boolean) {
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        binding.appBarMain.contentMain.listView.adapter = mAdapter

        // 一つ前のリスナーを消す
        snapshotListener?.remove()

        val userId = if(FirebaseAuth.getInstance().currentUser != null) FirebaseAuth.getInstance().currentUser!!.uid else ""
        if(userId != "") {
            //ユーザーIDがある場合はお気に入り質問リストから取得。
            getFavoriteQuestionList(userId, isFavoriteList)
        } else {
            //ユーザーIDがない場合はお気に入り質問リスト取得は行わない。
            updateGenreQuestionList(FirestoreFavoriteQuestion())
        }
    }

    /**
     * お気に入り質問IDリスト取得用関数
     * ユーザーIDに紐づくお気に入り質問IDリストを取得する。
     */
    private fun getFavoriteQuestionList(userId: String, isFavoriteList: Boolean) {
        FirebaseFirestore.getInstance()
            .collection(FavoritePATH)
            .whereEqualTo(FieldPath.documentId(), userId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "get favorite success")
                    val results = task.result.toObjects(FirestoreFavoriteQuestion::class.java)
                    results.forEach { firestoreFavoriteQuestion ->
                        if(isFavoriteList) {
                            //お気に入り一覧を表示する場合
                            updateFavoriteQuestionList(firestoreFavoriteQuestion)
                        } else {
                            //ジャンルの質問リストを表示する場合
                            updateGenreQuestionList(firestoreFavoriteQuestion)
                        }
                    }
                } else {
                    Log.d(TAG, "get favorite failed")
                }
            }
    }

    /**
     * お気に入り質問一覧表示用関数
     * お気に入り質問IDリストより、ユーザーに紐づくお気に入り質問を取得し画面に表示する。
     */
    private fun updateFavoriteQuestionList(firestoreFavoriteQuestion :FirestoreFavoriteQuestion) {
        FirebaseFirestore.getInstance()
            .collection(ContentsPATH)
            .whereIn("id", firestoreFavoriteQuestion.questionIdList)
            .get()
            .addOnCompleteListener {
                var questions = listOf<Question>()
                val results = it.result.toObjects(FirestoreQuestion::class.java)
                results?.also { questionList ->
                    questions = questionList.map { firestoreQuestion ->
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
                questions.forEach { question ->
                    //お気に入り一覧のため、お気に入りフラグは全てtrue
                    question.favorite = true
                }
                //ListViewを更新
                updateListView(questions)
            }
    }

    /**
     * ジャンル別質問一覧表示用関数
     * ジャンル別の質問を取得し画面に表示する。
     */
    private fun updateGenreQuestionList(firestoreFavoriteQuestion :FirestoreFavoriteQuestion) {
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
                questions.forEach { question ->
                    //パラメータで渡されたお気に入り質問IDリストを確認して、お気に入りフラグを更新する。
                    question.favorite = firestoreFavoriteQuestion.questionIdList.contains(question.questionUid)
                }
                //ListViewを更新
                updateListView(questions)

            }
    }

    private fun updateListView(questions : List<Question>) {
        Log.d(TAG, "updateListView start")
        mQuestionArrayList.clear()
        mQuestionArrayList.addAll(questions)
        mAdapter.notifyDataSetChanged()
    }
}