package com.cococue.kuistebakbola

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.cococue.kuistebakbola.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            
            // Atur padding untuk header agar tidak tertutup status bar/cutout
            binding.headerLayout.setPadding(
                binding.headerLayout.paddingLeft,
                systemBars.top,
                binding.headerLayout.paddingRight,
                binding.headerLayout.paddingBottom
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

        MobileAds.initialize(this) {}
        SoundManager.init(this)

        loadBannerAd()
        setupMenuClickListeners()
        FirebaseManager.loginAnonymous {}
    }

    private fun setupMenuClickListeners() {
        binding.btnCategory.setOnClickListener {
            val intent = Intent(this, CategoryActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binding.btnLeaderboard.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    private fun loadBannerAd() {
        val adView = AdView(this)
        adView.adUnitId = RemoteConfig.getBannerId()
        adView.setAdSize(AdSize.BANNER)

        binding.adViewContainer.removeAllViews()
        binding.adViewContainer.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}