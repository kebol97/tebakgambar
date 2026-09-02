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
    
    private var currentRemainingTime: Long = 0
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
        
        binding.btnBack.setOnClickListener {
            finish()
        }
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
        currentRemainingTime = timeForThisQuestion
        
        countDownTimer = object : CountDownTimer(timeForThisQuestion, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentRemainingTime = millisUntilFinished
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

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun getRemaining5050ForToday(): Int {
        val prefs = getSharedPreferences("lifeline_prefs", MODE_PRIVATE)
        val today = getTodayDateString()
        val savedDate = prefs.getString("KEY_5050_DATE", "")

        if (savedDate != today) {
            // Hari baru, reset hitungan harian menjadi 0
            prefs.edit()
                .putString("KEY_5050_DATE", today)
                .putInt("KEY_5050_COUNT", 0)
                .apply()
            return 2
        }

        val usedCount = prefs.getInt("KEY_5050_COUNT", 0)
        return maxOf(0, 2 - usedCount)
    }

    private fun use5050ForToday() {
        val prefs = getSharedPreferences("lifeline_prefs", MODE_PRIVATE)
        val today = getTodayDateString()
        val usedCount = prefs.getInt("KEY_5050_COUNT", 0)

        prefs.edit()
            .putString("KEY_5050_DATE", today)
            .putInt("KEY_5050_COUNT", usedCount + 1)
            .apply()
    }

    private fun setupHelpButtons() {
        updateLifelineButtonText()
        binding.btnHelp5050.setOnClickListener {
            lifelineCount = getRemaining5050ForToday()
            if (lifelineCount <= 0) {
                Toast.makeText(this, getString(R.string.msg_lifeline_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mRewardedAd != null) {
                // Hentikan waktu saat iklan bantuan muncul
                countDownTimer?.cancel()
                binding.tvTimer.text = "PAUSED"
                binding.tvTimer.setTextColor(android.graphics.Color.WHITE)

                mRewardedAd?.show(this) {
                    use5050ForToday()
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
    }

    private fun updateLifelineButtonText() {
        lifelineCount = getRemaining5050ForToday()
        binding.btnHelp5050.text = "${getString(R.string.btn_help_5050)} ($lifelineCount/2)"
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

    private fun setGameContentVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.INVISIBLE
        binding.tvQuestionText.visibility = visibility
        binding.btnOptionA.visibility = visibility
        binding.btnOptionB.visibility = visibility
        binding.btnOptionC.visibility = visibility
        binding.btnOptionD.visibility = visibility
        binding.btnHelp5050.visibility = visibility
        binding.tvTimer.visibility = visibility
    }

    private fun displayQuestion() {
        val currentQuestion = questionList[currentQuestionIndex]
        
        // Sembunyikan semua konten kuis sampai gambar siap
        setGameContentVisibility(false)

        // Tampilkan loading spinner pada frame gambar
        binding.layoutImageLoading.visibility = android.view.View.VISIBLE

        binding.tvQuestionProgress.text = "${currentQuestionIndex + 1}/${questionList.size}"
        binding.tvScore.text = "${getString(R.string.label_score)}: $score"
        binding.tvQuestionText.text = currentQuestion.questionText
        
        // Sembunyikan gambar sementara agar bisa di-animate
        binding.imgQuestion.alpha = 0f

        // Load gambar menggunakan Coil
        binding.imgQuestion.load(currentQuestion.imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_report_image)
            // Tambahkan User-Agent agar Wikimedia tidak memblokir permintaan
            addHeader("User-Agent", "KuisTebakBola/1.0 (Android; contact: support@cococue.com)")
            listener(
                onStart = {
                    binding.layoutImageLoading.visibility = android.view.View.VISIBLE
                },
                onSuccess = { _, _ ->
                    // Sembunyikan loading spinner gambar
                    binding.layoutImageLoading.visibility = android.view.View.GONE

                    // Gambar Berhasil Muncul -> Tampilkan Konten & Mulai Timer
                    setGameContentVisibility(true)
                    binding.imgQuestion.animate().alpha(1f).setDuration(400).start()
                    startTimer()
                },
                onError = { _, result ->
                    binding.layoutImageLoading.visibility = android.view.View.GONE
                    android.util.Log.e("GameplayActivity", "Coil Error: ${result.throwable.message}")
                    // Jika gambar gagal dimuat, cari soal yang lain (skip)
                    Toast.makeText(this@GameplayActivity, "Gagal memuat gambar, mencari soal lain...", Toast.LENGTH_SHORT).show()
                    nextQuestion()
                }
            )
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
            val bonus = (currentRemainingTime / 1000).toInt()
            val sessionPoints = 10 + bonus
            score += sessionPoints
            
            animateScorePop()
            SoundManager.playSound(this, "correct")
            Toast.makeText(this, "Benar! +$sessionPoints (Bonus: $bonus)", Toast.LENGTH_SHORT).show()
        } else {
            SoundManager.playSound(this, "wrong")
            SoundManager.vibrate(this)
            Toast.makeText(this, getString(R.string.msg_wrong), Toast.LENGTH_SHORT).show()
        }

        nextQuestion()
    }

    private fun animateScorePop() {
        binding.tvScore.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                binding.tvScore.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun nextQuestion() {
        if (currentQuestionIndex < (questionList.size - 1)) {
            currentQuestionIndex++
            displayQuestion()
        } else {
            FirebaseManager.uploadScore(score, "GuessPlayer")
            showVictoryDialog(score)
        }
    }

    private fun showVictoryDialog(finalScore: Int) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_victory)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val tvFinalScore = dialog.findViewById<android.widget.TextView>(R.id.tvFinalScore)
        val btnFinish = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFinish)

        tvFinalScore.text = "Skor Akhir: $finalScore"
        
        SoundManager.playSound(this, "victory")

        btnFinish.setOnClickListener {
            dialog.dismiss()
            showInterstitialThenFinish()
        }

        dialog.show()
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