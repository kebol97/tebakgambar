package com.cococue.kuistebakbola

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class UserScore(
    val userId: String = "",
    val userName: String = "Player",
    val score: Int = 0,
    val category: String = "Global",
    val tier: String = "Amatir",
    val timestamp: Long = System.currentTimeMillis()
)

object FirebaseManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun calculateTier(score: Int): String {
        return when {
            score >= 3501 -> "Legenda"
            score >= 1501 -> "Profesional"
            score >= 501 -> "Semi-Pro"
            else -> "Amatir"
        }
    }

    fun loginAnonymous(onSuccess: () -> Unit) {
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnSuccessListener {
                onSuccess()
            }
        } else {
            onSuccess()
        }
    }

    fun uploadScore(score: Int, category: String) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        
        var userName = user.displayName
        if (userName.isNullOrEmpty()) {
            val shortId = if (userId.length > 4) userId.takeLast(4).uppercase() else userId
            userName = "Player #$shortId"
        }
        
        // 1. Update Category Score (Accumulated)
        updateScoreAccumulated("scores_$category", userId, userName, score, category)
        
        // 2. Update Global Score (Accumulated)
        updateScoreAccumulated("scores_Global", userId, userName, score, "Global")
    }

    private fun updateScoreAccumulated(collection: String, userId: String, userName: String, sessionScore: Int, category: String) {
        db.collection(collection).document(userId).get().addOnSuccessListener { document ->
            val existingScore = document.toObject(UserScore::class.java)?.score ?: 0
            val totalScore = existingScore + sessionScore
            val newTier = calculateTier(totalScore)
            
            val userScore = UserScore(userId, userName, totalScore, category, newTier)
            db.collection(collection).document(userId).set(userScore)
                .addOnSuccessListener {
                    android.util.Log.d("FirebaseManager", "Score accumulated in $collection for $userName. New total: $totalScore")
                }
                .addOnFailureListener {
                    android.util.Log.e("FirebaseManager", "Failed to accumulate score in $collection: ${it.message}")
                }
        }.addOnFailureListener {
            // Jika dokumen tidak ada, set skor pertama kali
            val newTier = calculateTier(sessionScore)
            val userScore = UserScore(userId, userName, sessionScore, category, newTier)
            db.collection(collection).document(userId).set(userScore)
        }
    }

    fun getTopScores(category: String, callback: (List<UserScore>) -> Unit) {
        val collectionName = "scores_$category"
        db.collection(collectionName)
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val scores = result.toObjects(UserScore::class.java)
                android.util.Log.d("FirebaseManager", "Fetched ${scores.size} scores from $collectionName")
                callback(scores)
            }
            .addOnFailureListener {
                android.util.Log.e("FirebaseManager", "Error getTopScores: ${it.message}")
                callback(emptyList())
            }
    }

    fun getUserRank(category: String, callback: (Int, Int) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(0, 0)
            return
        }
        val collectionName = "scores_$category"
        
        db.collection(collectionName).document(userId).get().addOnSuccessListener { doc ->
            val myScore = doc.toObject(UserScore::class.java)?.score ?: 0
            
            if (myScore == 0) {
                callback(0, 0)
                return@addOnSuccessListener
            }

            db.collection(collectionName)
                .whereGreaterThan("score", myScore)
                .get()
                .addOnSuccessListener { result ->
                    val rank = result.size() + 1
                    callback(rank, myScore)
                }
                .addOnFailureListener {
                    android.util.Log.e("FirebaseManager", "Error getUserRank count: ${it.message}")
                    callback(0, myScore)
                }
        }.addOnFailureListener {
            android.util.Log.e("FirebaseManager", "Error getUserRank doc: ${it.message}")
            callback(0, 0)
        }
    }
}