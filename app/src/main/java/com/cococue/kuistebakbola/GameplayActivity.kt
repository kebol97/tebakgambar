package com.cococue.kuistebakbola

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.cococue.kuistebakbola.databinding.ActivityGameplayBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.launch

class GameplayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameplayBinding
    private val apiService by lazy { QuizApiService.create() }
    
    private var questionList: List<Question> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private var lifelineCount = 2
    
    private var countDownTimer: CountDownTimer? = null
    private var baseTimeInMillis = 15000L // 15 seconds start
    
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        binding = ActivityGameplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            
            // Atur padding untuk topBar agar tidak tertutup status bar/cutout
            binding.topBar.setPadding(
                binding.topBar.paddingLeft,
                systemBars.top,
                binding.topBar.paddingRight,
                binding.topBar.paddingBottom
            )
            
            // Atur padding untuk container bawah agar tidak tertutup navigasi bar
            binding.bottomNavContainer.setPadding(
                binding.bottomNavContainer.paddingLeft,
                binding.bottomNavContainer.paddingTop,
                binding.bottomNavContainer.paddingRight,
                systemBars.bottom
            )
            
            insets
        }

        setupGame()
        loadAds()
    }

    private fun loadAds() {
        // 1. Banner Ad
        val adView = AdView(this)
        adView.adUnitId = RemoteConfig.getBannerId()
        adView.setAdSize(AdSize.BANNER)
        binding.adViewContainer.removeAllViews()
        binding.adViewContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())

        // 2. Interstitial Ad (Load early)
        InterstitialAd.load(this, RemoteConfig.getInterstitialId(), AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            })

        // 3. Rewarded Ad (Load early)
        RewardedAd.load(this, RemoteConfig.getRewardedId(), AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mRewardedAd = null
                }
            })
    }

    private fun showInterstitialThenFinish() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    finish()
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    finish()
                }
            }
            mInterstitialAd?.show(this)
        } else {
            finish()
        }
    }

    private fun setupGame() {
        binding.loadingOverlay.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            try {
                val fullList = apiService.getQuestions(RemoteConfig.getQuestionsUrl())
                binding.loadingOverlay.visibility = android.view.View.GONE
                if (fullList.isNotEmpty()) {
                    // Randomize and take 10
                    questionList = fullList.shuffled().take(10)
                    displayQuestion()
                } else {
                    Toast.makeText(this@GameplayActivity, getString(R.string.msg_data_empty), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.loadingOverlay.visibility = android.view.View.GONE
                Toast.makeText(this@GameplayActivity, getString(R.string.msg_load_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }

        setupAnswerButtons()
        setupHelpButtons()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        
        // Time gets faster: 15s, 14s, 13s ... minimum 5s
        val timeForThisQuestion = maxOf(5000L, baseTimeInMillis - (currentQuestionIndex * 1000L))
        
        countDownTimer = object : CountDownTimer(timeForThisQuestion, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvTimer.text = "${millisUntilFinished / 1000}s"
                if (millisUntilFinished < 5000) {
                    binding.tvTimer.setTextColor(android.graphics.Color.RED)
                } else {
                    binding.tvTimer.setTextColor(android.graphics.Color.YELLOW)
                }
            }

            override fun onFinish() {
                Toast.makeText(this@GameplayActivity, getString(R.string.msg_wrong), Toast.LENGTH_SHORT).show()
                nextQuestion()
            }
        }.start()
    }

    private fun setupHelpButtons() {
        updateLifelineButtonText()
        binding.btnHelp5050.setOnClickListener {
            if (lifelineCount <= 0) {
                Toast.makeText(this, getString(R.string.msg_lifeline_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mRewardedAd != null) {
                mRewardedAd?.show(this) {
                    lifelineCount--
                    updateLifelineButtonText()
                    apply5050()
                    mRewardedAd = null
                    loadAds() // Load next rewarded ad
                }
            } else {
                Toast.makeText(this, getString(R.string.msg_ad_not_ready), Toast.LENGTH_SHORT).show()
                loadAds() // Try to reload if it was null
            }
        }

        binding.btnHelpAudience.setOnClickListener {
            Toast.makeText(this, getString(R.string.msg_feature_coming_soon), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLifelineButtonText() {
        binding.btnHelp5050.text = getString(R.string.btn_help_5050) + " ($lifelineCount)"
    }

    private fun apply5050() {
        val currentQuestion = questionList[currentQuestionIndex]
        val correctIndex = currentQuestion.correctAnswerIndex
        
        // List of wrong indices
        val wrongIndices = mutableListOf(0, 1, 2, 3).apply { remove(correctIndex) }
        
        // Randomly pick 2 to hide
        wrongIndices.shuffle()
        val toHide = wrongIndices.take(2)
        
        toHide.forEach { index ->
            when (index) {
                0 -> binding.btnOptionA.visibility = android.view.View.INVISIBLE
                1 -> binding.btnOptionB.visibility = android.view.View.INVISIBLE
                2 -> binding.btnOptionC.visibility = android.view.View.INVISIBLE
                3 -> binding.btnOptionD.visibility = android.view.View.INVISIBLE
            }
        }
        Toast.makeText(this, getString(R.string.msg_lifeline_applied), Toast.LENGTH_SHORT).show()
    }

    private fun displayQuestion() {
        val currentQuestion = questionList[currentQuestionIndex]
        startTimer()
        
        // Pastikan semua tombol jawaban terlihat kembali untuk soal baru
        binding.btnOptionA.visibility = android.view.View.VISIBLE
        binding.btnOptionB.visibility = android.view.View.VISIBLE
        binding.btnOptionC.visibility = android.view.View.VISIBLE
        binding.btnOptionD.visibility = android.view.View.VISIBLE

        binding.tvQuestionProgress.text = "${currentQuestionIndex + 1}/${questionList.size}"
        binding.tvScore.text = "${getString(R.string.label_score)}: $score"
        binding.tvQuestionText.text = currentQuestion.questionText
        
        // Load gambar menggunakan Coil
        binding.imgQuestion.load(currentQuestion.imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_report_image)
        }

        // Set teks pada tombol (Asumsi minimal 4 opsi sesuai JSON)
        if (currentQuestion.options.size >= 4) {
            binding.btnOptionA.text = "A. ${currentQuestion.options[0]}"
            binding.btnOptionB.text = "B. ${currentQuestion.options[1]}"
            binding.btnOptionC.text = "C. ${currentQuestion.options[2]}"
            binding.btnOptionD.text = "D. ${currentQuestion.options[3]}"
        }
    }

    private fun setupAnswerButtons() {
        binding.btnOptionA.setOnClickListener { checkAnswer(0) }
        binding.btnOptionB.setOnClickListener { checkAnswer(1) }
        binding.btnOptionC.setOnClickListener { checkAnswer(2) }
        binding.btnOptionD.setOnClickListener { checkAnswer(3) }
    }

    private fun checkAnswer(selectedIndex: Int) {
        countDownTimer?.cancel()
        val currentQuestion = questionList[currentQuestionIndex]
        if (selectedIndex == currentQuestion.correctAnswerIndex) {
            score += 10
            SoundManager.playSound(this, "correct")
            Toast.makeText(this, getString(R.string.msg_correct), Toast.LENGTH_SHORT).show()
        } else {
            SoundManager.playSound(this, "wrong")
            SoundManager.vibrate(this)
            Toast.makeText(this, getString(R.string.msg_wrong), Toast.LENGTH_SHORT).show()
        }

        nextQuestion()
    }

    private fun nextQuestion() {
        if (currentQuestionIndex < (questionList.size - 1)) {
            currentQuestionIndex++
            displayQuestion()
        } else {
            SoundManager.playSound(this, "victory")
            Toast.makeText(this, getString(R.string.msg_quiz_finished, score), Toast.LENGTH_LONG).show()
            FirebaseManager.uploadScore(score, "GuessPlayer")
            showInterstitialThenFinish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}