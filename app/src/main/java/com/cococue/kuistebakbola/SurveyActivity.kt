package com.cococue.kuistebakbola

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch
import java.util.Locale

class SurveyActivity : AppCompatActivity() {

    private val apiService by lazy { QuizApiService.create() }
    
    private var surveyQuestions: List<SurveyQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private var currentQuestion: SurveyQuestion? = null
    private var score = 0

    private lateinit var tvQuestion: TextView
    private lateinit var tvScore: TextView
    private lateinit var etAnswer: EditText
    private lateinit var btnSubmit: Button
    private lateinit var adViewContainer: FrameLayout
    
    private lateinit var tvAnswers: List<TextView>
    private lateinit var tvPoints: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)

        tvQuestion = findViewById(R.id.tvQuestion)
        tvScore = findViewById(R.id.tvScore)
        etAnswer = findViewById(R.id.etAnswer)
        btnSubmit = findViewById(R.id.btnSubmit)
        adViewContainer = findViewById(R.id.adViewContainer)

        tvAnswers = listOf(
            findViewById(R.id.tvAnswer1),
            findViewById(R.id.tvAnswer2),
            findViewById(R.id.tvAnswer3),
            findViewById(R.id.tvAnswer4),
            findViewById(R.id.tvAnswer5)
        )

        tvPoints = listOf(
            findViewById(R.id.tvPoints1),
            findViewById(R.id.tvPoints2),
            findViewById(R.id.tvPoints3),
            findViewById(R.id.tvPoints4),
            findViewById(R.id.tvPoints5)
        )

        val topBar = findViewById<LinearLayout>(R.id.topBar)
        val inputContainer = findViewById<LinearLayout>(R.id.inputContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            topBar.setPadding(0, systemBars.top, 0, 0)
            inputContainer.setPadding(inputContainer.paddingLeft, 
                inputContainer.paddingTop, 
                inputContainer.paddingRight, 
                systemBars.bottom)
            insets
        }

        loadSurveyData()
        setupListeners()
        loadBannerAd()
    }

    private fun loadSurveyData() {
        lifecycleScope.launch {
            try {
                surveyQuestions = apiService.getSurveyQuestions(RemoteConfig.getSurveyUrl())
                if (surveyQuestions.isNotEmpty()) {
                    displayQuestion()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SurveyActivity, getString(R.string.msg_load_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayQuestion() {
        currentQuestion = surveyQuestions[currentQuestionIndex]
        tvQuestion.text = currentQuestion?.questionText
        tvScore.text = "${getString(R.string.label_score)}: $score"
        resetBoard()
    }

    private fun resetBoard() {
        tvAnswers.forEach { it.text = "---" }
        tvPoints.forEach { it.visibility = View.GONE }
    }

    private fun setupListeners() {
        btnSubmit.setOnClickListener {
            val userGuess = etAnswer.text.toString().trim()
            if (userGuess.isNotEmpty()) {
                checkAnswer(userGuess)
                etAnswer.setText("")
            }
        }
    }

    private fun checkAnswer(guess: String) {
        val answers = currentQuestion?.answers ?: return
        var found = false
        
        for (i in answers.indices) {
            val surveyAnswer = answers[i]
            if (surveyAnswer.answer.lowercase(Locale.ROOT) == guess.lowercase(Locale.ROOT)) {
                if (!surveyAnswer.isRevealed) {
                    surveyAnswer.isRevealed = true
                    tvAnswers[i].text = surveyAnswer.answer
                    tvPoints[i].text = surveyAnswer.points.toString()
                    tvPoints[i].visibility = View.VISIBLE
                    
                    score += surveyAnswer.points
                    tvScore.text = "${getString(R.string.label_score)}: $score"
                    found = true
                    Toast.makeText(this, getString(R.string.msg_correct_survey, surveyAnswer.points), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.msg_already_revealed), Toast.LENGTH_SHORT).show()
                    return
                }
                break
            }
        }

        if (!found) {
            Toast.makeText(this, getString(R.string.msg_not_in_survey), Toast.LENGTH_SHORT).show()
        }

        if (answers.all { it.isRevealed }) {
            Toast.makeText(this, getString(R.string.msg_survey_complete), Toast.LENGTH_LONG).show()
            FirebaseManager.uploadScore(score, "Survey")
        }
    }

    private fun loadBannerAd() {
        val adView = AdView(this)
        adView.adUnitId = RemoteConfig.getBannerId()
        adView.setAdSize(AdSize.BANNER)
        adViewContainer.removeAllViews()
        adViewContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}