package com.cococue.kuistebakbola

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface QuizApiService {
    @GET
    suspend fun getQuestions(@Url url: String): List<Question>

    @GET
    suspend fun getAdConfig(@Url url: String): AdConfig

    @GET
    suspend fun getSurveyQuestions(@Url url: String): List<SurveyQuestion>

    companion object {
        fun create(): QuizApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(QuizApiService::class.java)
        }
    }
}