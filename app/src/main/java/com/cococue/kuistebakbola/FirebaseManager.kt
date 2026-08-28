package com.cococue.kuistebakbola

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class UserScore(
    val userId: String = "",
    val userName: String = "Player",
    val score: Int = 0,
    val category: String = "Global",
    val timestamp: Long = System.currentTimeMillis()
)

object FirebaseManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

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
        val userName = if (user.displayName.isNullOrEmpty()) "Player" else user.displayName!!
        
        // 1. Update Category Score
        updateIfHigher("scores_$category", userId, userName, score, category)
        
        // 2. Update Global Score (Total/Highest from any category)
        updateIfHigher("scores_Global", userId, userName, score, "Global")
    }

    private fun updateIfHigher(collection: String, userId: String, userName: String, score: Int, category: String) {
        db.collection(collection).document(userId).get().addOnSuccessListener { document ->
            val existingScore = document.toObject(UserScore::class.java)?.score ?: 0
            if (score > existingScore) {
                val userScore = UserScore(userId, userName, score, category)
                db.collection(collection).document(userId).set(userScore)
                    .addOnSuccessListener {
                        android.util.Log.d("FirebaseManager", "Score updated in $collection for $userName")
                    }
                    .addOnFailureListener {
                        android.util.Log.e("FirebaseManager", "Failed to update score in $collection: ${it.message}")
                    }
            } else {
                android.util.Log.d("FirebaseManager", "Score not higher. Current: $score, Existing: $existingScore")
            }
        }.addOnFailureListener {
            // If document doesn't exist, this might still trigger success with document.exists() == false
            // But if it's a real failure (like permissions):
            android.util.Log.e("FirebaseManager", "Error checking existing score in $collection: ${it.message}")
            
            // Try to set anyway if it's just a "not found" type of error (though get() usually succeeds for missing docs)
            val userScore = UserScore(userId, userName, score, category)
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