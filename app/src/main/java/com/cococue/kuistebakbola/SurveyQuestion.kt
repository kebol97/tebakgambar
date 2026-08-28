package com.cococue.kuistebakbola

data class SurveyAnswer(
    val answer: String,
    val points: Int,
    var isRevealed: Boolean = false
)

data class SurveyQuestion(
    val id: Int,
    val questionText: String,
    val answers: List<SurveyAnswer>
)