package com.example.guidebook.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guidebook.databinding.ActivityLoginBinding
import com.example.guidebook.repository.GuidebookRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repo = GuidebookRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Auto-login if already signed in
        val uid = repo.getCurrentUid()
        if (uid != null) {
            lifecycleScope.launch {
                val user = repo.getUserProfile(uid)
                if (user != null) navigateToPage1(user) else repo.logout()
            }
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pw = binding.etPassword.text.toString()
            if (email.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false

            lifecycleScope.launch {
                repo.login(email, pw)
                    .onSuccess { user ->
                        binding.progressBar.visibility = View.GONE
                        navigateToPage1(user)
                    }
                    .onFailure {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, "로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun navigateToPage1(user: com.example.guidebook.models.AppUser) {
        startActivity(Intent(this, Page1Activity::class.java).apply {
            putExtra("user", user)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
