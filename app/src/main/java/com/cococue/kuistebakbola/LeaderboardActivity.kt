package com.cococue.kuistebakbola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.tabs.TabLayout

class LeaderboardActivity : AppCompatActivity() {

    private val adapter = LeaderboardAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val headerLayout = findViewById<LinearLayout>(R.id.headerLayout)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val rvLeaderboard = findViewById<RecyclerView>(R.id.rvLeaderboard)
        val adViewContainer = findViewById<FrameLayout>(R.id.adViewContainer)
        val myRankContainer = findViewById<LinearLayout>(R.id.myRankContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            headerLayout.setPadding(0, systemBars.top, 0, 0)
            adViewContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        rvLeaderboard.layoutManager = LinearLayoutManager(this)
        rvLeaderboard.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                loadLeaderboard(if (tab?.position == 0) "Global" else "GuessPlayer")
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadLeaderboard("Global")
        loadBannerAd(adViewContainer)
    }

    private fun loadLeaderboard(category: String) {
        FirebaseManager.getTopScores(category) { scores ->
            adapter.setData(scores)
        }
        
        FirebaseManager.getUserRank(category) { rank, score ->
            val tvMyRank = findViewById<TextView>(R.id.tvMyRank)
            val tvMyScore = findViewById<TextView>(R.id.tvMyScore)
            tvMyRank.text = "Peringkat Kamu: $rank"
            tvMyScore.text = "$score Poin"
        }
    }

    private fun loadBannerAd(container: FrameLayout) {
        val adView = AdView(this)
        adView.adUnitId = RemoteConfig.getBannerId()
        adView.setAdSize(AdSize.BANNER)
        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {
        private var list: List<UserScore> = emptyList()

        fun setData(newList: List<UserScore>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.text1.text = "${position + 1}. ${item.userName}"
            holder.text2.text = "${item.score} Poin"
            
            holder.text1.setTextColor(android.graphics.Color.WHITE)
            holder.text2.setTextColor(android.graphics.Color.YELLOW)
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }
    }
}