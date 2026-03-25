package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    lateinit var binding: ActivityAuthBinding
    private var isLoginActive = true
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "❌ Google Sign-In échoué", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = android.graphics.Color.parseColor("#132D1F")
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser != null) {
            goToMain()
            return
        }

        // ── Setup Google Sign-In ──
        setupGoogleSignIn()

        showFragment(LoginFragment(), slideRight = false)
        updateToggle(loginActive = true)

        binding.tvSegmentLogin.setOnClickListener {
            if (!isLoginActive) {
                isLoginActive = true
                updateToggle(loginActive = true)
                showFragment(LoginFragment(), slideRight = false)
                binding.tvSocialLabel.text = "Connexion via réseau social"
                binding.particleView.triggerRippleFromView(binding.tvSegmentLogin)
            }
        }

        binding.tvSegmentRegister.setOnClickListener {
            if (isLoginActive) {
                isLoginActive = false
                updateToggle(loginActive = false)
                showFragment(RegisterFragment(), slideRight = true)
                binding.tvSocialLabel.text = "S'inscrire via réseau social"
                binding.particleView.triggerRippleFromView(binding.tvSegmentRegister)
            }
        }

        binding.btnAuthAction.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.btnAuthAction)
            if (isLoginActive) {
                (supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        as? LoginFragment)?.attemptLogin()
            } else {
                (supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        as? RegisterFragment)?.attemptRegister()
            }
        }

        binding.btnSocialLogin.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.btnSocialLogin)

            val location = IntArray(2)
            binding.btnSocialLogin.getLocationOnScreen(location)
            val screenHeight = resources.displayMetrics.heightPixels
            val offsetFromBottom = screenHeight - location[1] +
                    (8 * resources.displayMetrics.density).toInt()

            SocialLoginDialog().apply {
                anchorY = offsetFromBottom
            }.show(supportFragmentManager, SocialLoginDialog.TAG)
        }
    }

    // ── Google Sign-In Setup ──
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    // Called from SocialLoginDialog
    fun launchGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user!!
                val isNewUser = result.additionalUserInfo?.isNewUser == true

                if (isNewUser) {
                    // New Google user — show "Complete Profile" dialog
                    showCompleteProfileDialog(user)
                } else {
                    Toast.makeText(this, "👋 Bon retour ${user.displayName}!", Toast.LENGTH_SHORT).show()
                    goToMain()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Erreur: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showCompleteProfileDialog(user: com.google.firebase.auth.FirebaseUser) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_profile, null)

        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.etDialogPhone)
        val etAge = dialogView.findViewById<android.widget.EditText>(R.id.etDialogAge)
        val tvCity = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogCity)
        var selectedCity = ""

        tvCity.setOnClickListener {
            CityPickerBottomSheet.newInstance { city ->
                selectedCity = city
                tvCity.text = city
                tvCity.setTextColor(0xFFFFFFFF.toInt())
            }.show(supportFragmentManager, "cityPicker")
        }

        android.app.AlertDialog.Builder(this, R.style.AppAlertDialog)
            .setTitle("Compléter votre profil")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Continuer") { _, _ ->
                val phone = etPhone.text.toString().trim()
                val ageStr = etAge.text.toString().trim()
                val age = ageStr.toIntOrNull() ?: 0

                val userProfile = hashMapOf(
                    "uid" to user.uid,
                    "name" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "phone" to phone,
                    "age" to age,
                    "city" to selectedCity,
                    "avatarUrl" to (user.photoUrl?.toString() ?: ""),
                    "bio" to "",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "adsCount" to 0,
                    "rating" to 0.0,
                    "isVerified" to true,
                    "authProvider" to "google"
                )

                db.collection("users").document(user.uid)
                    .set(userProfile)
                    .addOnCompleteListener {
                        Toast.makeText(this, "✅ Bienvenue ${user.displayName}!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    }
            }
            .show()
    }

    private fun goToMain() {
        val prefs = getSharedPreferences("madinatti_prefs", 0)
        prefs.edit().putBoolean("has_seen_splash", true).apply()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    fun triggerParticleRipple(view: View) {
        binding.particleView.triggerRippleFromView(view)
    }

    private fun updateToggle(loginActive: Boolean) {
        val indicator = binding.segmentIndicator
        indicator.post {
            indicator.animate()
                .translationX(if (loginActive) 0f else indicator.width.toFloat())
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        indicator.setBackgroundResource(
            if (loginActive) R.drawable.bg_segment_left
            else R.drawable.bg_segment_right
        )
        binding.tvSegmentLogin.setTextColor(
            if (loginActive) android.graphics.Color.parseColor("#0D1F17")
            else android.graphics.Color.WHITE
        )
        binding.tvSegmentRegister.setTextColor(
            if (loginActive) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#0D1F17")
        )
        binding.btnAuthAction.text = if (loginActive) "Se connecter" else "S'inscrire"
    }

    private fun showFragment(fragment: Fragment, slideRight: Boolean) {
        val enter = if (slideRight) R.anim.slide_in_right else R.anim.slide_in_left
        val exit = if (slideRight) R.anim.slide_out_left else R.anim.slide_out_right
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enter, exit)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val location = IntArray(2)
        binding.particleView.getLocationOnScreen(location)
        binding.particleView.onExternalTouch(
            ev.rawX - location[0],
            ev.rawY - location[1],
            ev.action != MotionEvent.ACTION_UP && ev.action != MotionEvent.ACTION_CANCEL
        )
        return super.dispatchTouchEvent(ev)
    }
}