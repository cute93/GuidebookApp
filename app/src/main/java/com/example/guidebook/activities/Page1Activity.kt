package com.example.guidebook.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.guidebook.adapters.UserPanelAdapter
import com.example.guidebook.databinding.ActivityPage1Binding
import com.example.guidebook.models.AppUser
import com.example.guidebook.models.Problem
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
        setupRecyclerView()
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

    private fun setupRecyclerView() {
        binding.rvPanels.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupButtons() {
        binding.btnPrev.setOnClickListener { viewModel.goToPrev() }
        binding.btnNext.setOnClickListener { viewModel.goToNext() }
        binding.btnUploadProblem.setOnClickListener {
            startActivity(Intent(this, Page3Activity::class.java).apply {
                putExtra("user", currentUser)
            })
        }
    }

    private fun updateProblemUi(idx: Int, list: List<Problem>) {
        val problem = list.getOrNull(idx) ?: return
        binding.tvProblemTitle.text = problem.title
        Glide.with(this).load(problem.imageUrl).into(binding.ivProblem)
        binding.btnPrev.isEnabled = idx > 0
        binding.btnNext.isEnabled = idx < list.size - 1
    }

    private fun observeViewModel() {
        viewModel.problems.observe(this) { list ->
            updateProblemUi(viewModel.currentIndex.value ?: 0, list)
        }

        viewModel.currentIndex.observe(this) { idx ->
            updateProblemUi(idx, viewModel.problems.value ?: return@observe)
        }

        viewModel.notes.observe(this) { notes ->
            val problem = viewModel.currentProblem ?: return@observe
            val panels = buildPanels(notes, problem)
            binding.rvPanels.adapter = UserPanelAdapter(panels) { item ->
                if (item.userId == currentUser.uid ||
                    (currentUser.role == "teacher" && item.role == "teacher")
                ) {
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

        viewModel.error.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun buildPanels(notes: List<UserNote>, problem: Problem): List<UserPanelAdapter.PanelItem> {
        val result = mutableListOf<UserPanelAdapter.PanelItem>()

        val teacherNote = notes.find { it.role == "teacher" }
        result.add(UserPanelAdapter.PanelItem(
            userId   = teacherNote?.userId ?: problem.teacherId,
            userName = "교사",
            role     = "teacher",
            note     = teacherNote
        ))

        if (currentUser.role == "student") {
            val myNote = notes.find { it.userId == currentUser.uid }
            result.add(UserPanelAdapter.PanelItem(
                userId   = currentUser.uid,
                userName = currentUser.name,
                role     = "student",
                note     = myNote
            ))
        }

        notes.filter { it.role == "student" && it.userId != currentUser.uid }
            .forEach { note -> if (result.size < 4) result.add(UserPanelAdapter.PanelItem(note.userId, note.userName, "student", note)) }

        var idx = 0
        while (result.size < 4) {
            result.add(UserPanelAdapter.PanelItem("empty_$idx", "학생${++idx}", "student", null))
        }

        return result.take(4)
    }
}
