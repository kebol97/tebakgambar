package com.cococue.kuistebakbola

data class Question(
    val id: Int,
    val imageUrl: String,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)