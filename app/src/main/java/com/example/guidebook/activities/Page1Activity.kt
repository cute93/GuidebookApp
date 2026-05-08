package com.example.guidebook.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.guidebook.databinding.ActivityPage1Binding
import com.example.guidebook.models.AppUser
import com.example.guidebook.models.UserNote
import com.example.guidebook.viewmodels.Page1ViewModel

class Page1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPage1Binding
    private val viewModel: Page1ViewModel by viewModels()

    private lateinit var currentUser: AppUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPage1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = intent.getSerializableExtra("user") as? AppUser
            ?: run { finish(); return }

        viewModel.init(currentUser)
        setupButtons()
        observeViewModel()

        // Teacher sees upload button; students do not
        binding.btnUploadProblem.visibility =
            if (currentUser.role == "teacher") View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNotes()
    }

    private fun setupButtons() {
        binding.btnPrev.setOnClickListener { viewModel.goToPrev() }
        binding.btnNext.setOnClickListener { viewModel.goToNext() }
        binding.btnUploadProblem.setOnClickListener {
            startActivity(Intent(this, Page3Activity::class.java).apply {
                putExtra("user", currentUser)
            })
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun observeViewModel() {
        viewModel.problems.observe(this) { list ->
            val idx = viewModel.currentIndex.value ?: 0
            val problem = list.getOrNull(idx) ?: return@observe
            binding.tvProblemTitle.text = problem.title
            Glide.with(this).load(problem.imageUrl).into(binding.ivProblem)
            binding.btnPrev.isEnabled = idx > 0
            binding.btnNext.isEnabled = idx < list.size - 1
        }

        viewModel.currentIndex.observe(this) { idx ->
            val list = viewModel.problems.value ?: return@observe
            val problem = list.getOrNull(idx) ?: return@observe
            binding.tvProblemTitle.text = problem.title
            Glide.with(this).load(problem.imageUrl).into(binding.ivProblem)
            binding.btnPrev.isEnabled = idx > 0
            binding.btnNext.isEnabled = idx < list.size - 1
        }

        viewModel.notes.observe(this) { notes ->
            val problem = viewModel.currentProblem ?: return@observe
            val teacherNote = notes.find { it.role == "teacher" }
            val studentNotes = notes.filter { it.role == "student" }
            val hasMyStudentNote = studentNotes.any { it.userId == currentUser.uid }

            bindPanel(
                binding.tvTeacherName, binding.ivTeacherNote, binding.btnTeacherWrite,
                note = teacherNote,
                canEdit = currentUser.role == "teacher",
                problem = problem
            )

            listOf(
                Triple(binding.tvStudent1Name, binding.ivStudent1Note, binding.btnStudent1Write),
                Triple(binding.tvStudent2Name, binding.ivStudent2Note, binding.btnStudent2Write),
                Triple(binding.tvStudent3Name, binding.ivStudent3Note, binding.btnStudent3Write)
            ).forEachIndexed { i, (tvName, ivNote, btnWrite) ->
                val note = studentNotes.getOrNull(i)
                val canEdit = currentUser.role == "student" &&
                    (note?.userId == currentUser.uid || (note == null && !hasMyStudentNote))
                bindPanel(tvName, ivNote, btnWrite, note, canEdit, problem)
            }
        }

        viewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun bindPanel(
        tvName: android.widget.TextView,
        ivNote: android.widget.ImageView,
        btnWrite: android.widget.Button,
        note: UserNote?,
        canEdit: Boolean,
        problem: com.example.guidebook.models.Problem
    ) {
        tvName.text = note?.userName ?: ""
        if (note?.noteImageUrl != null) {
            Glide.with(this).load(note.noteImageUrl).into(ivNote)
        } else {
            ivNote.setImageDrawable(null)
        }
        btnWrite.isEnabled = canEdit
        btnWrite.setOnClickListener {
            if (canEdit) {
                startActivity(Intent(this, Page2Activity::class.java).apply {
                    putExtra("user", currentUser)
                    putExtra("problemId", problem.id)
                    putExtra("problemImageUrl", problem.imageUrl)
                    putExtra("problemTitle", problem.title)
                })
            } else {
                Toast.makeText(this, "자신의 해결포인트만 작성할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
