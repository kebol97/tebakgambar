package com.cococue.kuistebakbola

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var imgProfileLarge: ImageView
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnSignOut: MaterialButton

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        imgProfileLarge = findViewById(R.id.imgProfileLarge)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnSignOut = findViewById(R.id.btnSignOut)

        val headerLayout = findViewById<LinearLayout>(R.id.headerLayout)
        val adViewContainer = findViewById<FrameLayout>(R.id.adViewContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            headerLayout.setPadding(0, systemBars.top, 0, 0)
            adViewContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
        btnSignOut.setOnClickListener { signOut() }

        updateUI()
        loadBannerAd(adViewContainer)
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                updateUI()
            } else {
                Toast.makeText(this, "Firebase Auth failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signOut() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            updateUI()
        }
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            tvUserName.text = user.displayName ?: "User"
            tvUserEmail.text = user.email
            imgProfileLarge.load(user.photoUrl) {
                placeholder(R.drawable.ic_launcher_foreground)
            }
            btnGoogleSignIn.visibility = View.GONE
            btnSignOut.visibility = View.VISIBLE
        } else {
            tvUserName.text = "Guest"
            tvUserEmail.text = "Not signed in"
            imgProfileLarge.setImageResource(R.drawable.ic_launcher_foreground)
            btnGoogleSignIn.visibility = View.VISIBLE
            btnSignOut.visibility = View.GONE
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
}