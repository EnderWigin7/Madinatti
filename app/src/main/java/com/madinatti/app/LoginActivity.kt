package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val ivToggle = findViewById<ImageView>(R.id.ivTogglePassword)
        val btnLogin = findViewById<TextView>(R.id.btnLogin)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Password show/hide toggle
        ivToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.transformationMethod = if (passwordVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text.length)
            ivToggle.alpha = if (passwordVisible) 1f else 0.5f
        }

        // Input focus effects
        listOf(etEmail, etPassword).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                field.background = if (hasFocus)
                    androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.bg_input_field_focused)
                else
                    androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.bg_input_field)
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty()) {
                etEmail.error = "Email requis"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Mot de passe requis"
                return@setOnClickListener
            }

            animateButton(btnLogin) {
                // TODO: Firebase auth here later
                // For now navigate to MainActivity
                val prefs = getSharedPreferences("madinatti_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("has_seen_splash", true).apply()
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }

        tvForgot.setOnClickListener {
            // TODO: Forgot password screen later
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun animateButton(view: TextView, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.96f).scaleY(0.96f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                    .withEndAction { onEnd() }
                    .start()
            }.start()
    }
}