package com.cococue.kuistebakbola

import com.google.gson.annotations.SerializedName
import java.util.Locale

data class AdConfig(
    @SerializedName("appOpenId") val appOpenId: String,
    @SerializedName("bannerId") val bannerId: String,
    @SerializedName("interstitialId") val interstitialId: String,
    @SerializedName("rewardedId") val rewardedId: String
)

object RemoteConfig {
    // --- GANTI SEMUA LINK GITHUB ANDA DI SINI ---
    const val AD_CONFIG_URL = "https://raw.githubusercontent.com/kebol97/gametbcococue/refs/heads/main/ad_config.json"
    
    // URL Soal Bahasa Indonesia
    private const val QUESTIONS_URL_ID = "https://raw.githubusercontent.com/kebol97/gametbcococue/refs/heads/main/quiz.json"
    private const val SURVEY_URL_ID = "https://raw.githubusercontent.com/kebol97/gametbcococue/refs/heads/main/quiz.json"
    
    // URL Soal Bahasa Inggris (Default)
    private const val QUESTIONS_URL_EN = "https://raw.githubusercontent.com/kebol97/gametbcococue/refs/heads/main/quiz.json"
    private const val SURVEY_URL_EN = "https://raw.githubusercontent.com/kebol97/gametbcococue/refs/heads/main/survey.json"
    // --------------------------------------------

    var adConfig: AdConfig? = null
    
    // Default Test IDs as fallback
    val DEFAULT_AD_CONFIG = AdConfig(
        appOpenId = "ca-app-pub-3940256099942544/9257395921x",
        bannerId = "ca-app-pub-3940256099942544/6300978111x",
        interstitialId = "ca-app-pub-3940256099942544/1033173712x",
        rewardedId = "ca-app-pub-3940256099942544/5224354917x"
    )

    fun getAppOpenId() = adConfig?.appOpenId ?: DEFAULT_AD_CONFIG.appOpenId
    fun getBannerId() = adConfig?.bannerId ?: DEFAULT_AD_CONFIG.bannerId
    fun getInterstitialId() = adConfig?.interstitialId ?: DEFAULT_AD_CONFIG.interstitialId
    fun getRewardedId() = adConfig?.rewardedId ?: DEFAULT_AD_CONFIG.rewardedId
    
    fun getQuestionsUrl(): String {
        val lang = Locale.getDefault().language
        return if (lang == "id" || lang == "in") QUESTIONS_URL_ID else QUESTIONS_URL_EN
    }
    
    fun getSurveyUrl(): String {
        val lang = Locale.getDefault().language
        return if (lang == "id" || lang == "in") SURVEY_URL_ID else SURVEY_URL_EN
    }
}