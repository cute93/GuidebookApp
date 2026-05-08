package com.example.guidebook.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.guidebook.databinding.ActivityPage2Binding
import com.example.guidebook.models.AppUser
import com.example.guidebook.viewmodels.Page2ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class Page2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPage2Binding
    private val viewModel: Page2ViewModel by viewModels()

    private lateinit var currentUser: AppUser
    private lateinit var problemId: String
    private lateinit var problemImageUrl: String
    private lateinit var problemTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPage2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = intent.getSerializableExtra("user") as? AppUser ?: run { finish(); return }
        problemId = intent.getStringExtra("problemId") ?: run { finish(); return }
        problemImageUrl = intent.getStringExtra("problemImageUrl") ?: ""
        problemTitle = intent.getStringExtra("problemTitle") ?: ""

        binding.tvProblemTitle2.text = problemTitle
        Glide.with(this).load(problemImageUrl).into(binding.ivProblemRef)

        setupToolbar()
        setupButtons()
        observeViewModel()
        binding.drawingView.post { loadDraftIfExists() }
    }

    private fun setupToolbar() {
        binding.btnColorBlack.setOnClickListener { binding.drawingView.setColor(Color.BLACK) }
        binding.btnColorBlue.setOnClickListener  { binding.drawingView.setColor(Color.BLUE) }
        binding.btnColorRed.setOnClickListener   { binding.drawingView.setColor(Color.RED) }
        binding.btnEraser.setOnClickListener     { binding.drawingView.setEraserMode(true) }
        binding.btnUndo.setOnClickListener       { binding.drawingView.undo() }
        binding.btnClear.setOnClickListener      { binding.drawingView.clear() }

        binding.sbStroke.addOnChangeListener { _, value, _ ->
            binding.drawingView.setStrokeWidth(value)
        }
    }

    private fun setupButtons() {
        binding.btnSaveDraft.setOnClickListener { saveDraftLocally() }

        binding.btnUpload.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnUpload.isEnabled = false
            viewModel.uploadDrawing(
                problemId = problemId,
                userId = currentUser.uid,
                userName = currentUser.name,
                role = currentUser.role,
                bitmap = binding.drawingView.getBitmap()
            )
        }
    }

    private fun saveDraftLocally() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dir = File(filesDir, "drafts").also { it.mkdirs() }
            val file = File(dir, "${problemId}_${currentUser.uid}.png")
            file.outputStream().use { out ->
                binding.drawingView.getBitmap().compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Page2Activity, "임시 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDraftIfExists() {
        val file = File(filesDir, "drafts/${problemId}_${currentUser.uid}.png")
        if (file.exists()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            binding.drawingView.loadBitmap(bmp)
        }
    }

    private fun observeViewModel() {
        viewModel.saveState.observe(this) { state ->
            binding.progressBar.visibility = View.GONE
            binding.btnUpload.isEnabled = true
            when (state) {
                Page2ViewModel.SaveState.Uploaded -> {
                    Toast.makeText(this, "업로드 완료! Page1에 반영되었습니다.", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is Page2ViewModel.SaveState.Error ->
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }
}
