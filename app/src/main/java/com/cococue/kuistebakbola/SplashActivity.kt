package com.cococue.kuistebakbola

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private var appOpenAd: AppOpenAd? = null
    private var isAdShown = false
    private var isTimeUp = false
    private val apiService by lazy { QuizApiService.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Jalankan timer 3 detik
        Handler(Looper.getMainLooper()).postDelayed({
            isTimeUp = true
            checkNavigation()
        }, 3000)

        // Selalu ambil konfigurasi iklan agar ID Banner & Rewarded terupdate
        fetchConfigAndMaybeLoadAd()
    }

    private fun shouldShowAd(): Boolean {
        val prefs = getSharedPreferences("ad_prefs", MODE_PRIVATE)
        val lastShowTime = prefs.getLong("last_open_ad_time", 0L)
        val currentTime = System.currentTimeMillis()
        val cooldownMillis = 4 * 60 * 60 * 1000L // 4 jam
        return (currentTime - lastShowTime) > cooldownMillis
    }

    private fun updateAdTimestamp() {
        val prefs = getSharedPreferences("ad_prefs", MODE_PRIVATE)
        prefs.edit().putLong("last_open_ad_time", System.currentTimeMillis()).apply()
    }

    private fun fetchConfigAndMaybeLoadAd() {
        lifecycleScope.launch {
            try {
                RemoteConfig.adConfig = apiService.getAdConfig(RemoteConfig.AD_CONFIG_URL)
            } catch (e: Exception) {
                // Fallback default
            }

            // Hanya load iklan jika masa cooldown sudah lewat
            if (shouldShowAd()) {
                loadAppOpenAd()
            }
        }
    }

    private fun loadAppOpenAd() {
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            this,
            RemoteConfig.getAppOpenId(),
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    // Jika timer 3 detik sudah habis saat iklan siap, langsung tampilkan
                    if (isTimeUp) {
                        showAppOpenAd()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    checkNavigation()
                }
            }
        )
    }

    private fun showAppOpenAd() {
        if (isAdShown) return
        isAdShown = true
        
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                updateAdTimestamp()
                navigateToMain()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                navigateToMain()
            }
        }
        appOpenAd?.show(this)
    }

    private fun checkNavigation() {
        if (isTimeUp) {
            if (appOpenAd != null && shouldShowAd()) {
                showAppOpenAd()
            } else if (!isAdShown) {
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}