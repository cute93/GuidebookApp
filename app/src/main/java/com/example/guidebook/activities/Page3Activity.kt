package com.example.guidebook.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.guidebook.databinding.ActivityPage3Binding
import com.example.guidebook.models.AppUser
import com.example.guidebook.viewmodels.Page3ViewModel

class Page3Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPage3Binding
    private val viewModel: Page3ViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).into(binding.ivPreview)
            binding.tvImageHint.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPage3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = intent.getSerializableExtra("user") as? AppUser

        binding.btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnUploadProblem.setOnClickListener {
            val uri = selectedImageUri ?: run {
                Toast.makeText(this, "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.uploadProblem(
                title   = binding.etTitle.text.toString(),
                subject = binding.etSubject.text.toString(),
                imageUri = uri,
                context = this
            )
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uploadState.observe(this) { state ->
            when (state) {
                Page3ViewModel.UploadState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnUploadProblem.isEnabled = false
                }
                is Page3ViewModel.UploadState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnUploadProblem.isEnabled = true
                    Toast.makeText(this, "문제가 등록되었습니다. Page1에서 확인하세요.", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is Page3ViewModel.UploadState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnUploadProblem.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                null -> {}
            }
        }
    }
}
