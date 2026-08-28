package com.cococue.kuistebakbola

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.card.MaterialCardView

class CategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        val headerLayout = findViewById<LinearLayout>(R.id.headerLayout)
        val adViewContainer = findViewById<FrameLayout>(R.id.adViewContainer)
        val cardTebakPemain = findViewById<MaterialCardView>(R.id.cardTebakPemain)
        val cardSurvey = findViewById<MaterialCardView>(R.id.cardSurvey)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            headerLayout.setPadding(0, systemBars.top, 0, 0)
            adViewContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        cardTebakPemain.setOnClickListener {
            val intent = Intent(this, GameplayActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        cardSurvey.setOnClickListener {
            val intent = Intent(this, SurveyActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        loadBannerAd(adViewContainer)
    }

    private fun loadBannerAd(container: FrameLayout) {
        val adView = AdView(this)
        adView.adUnitId = RemoteConfig.getBannerId()
        adView.setAdSize(AdSize.BANNER)
        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }
}