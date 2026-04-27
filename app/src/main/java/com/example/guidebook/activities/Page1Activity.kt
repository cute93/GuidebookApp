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
import com.example.guidebook.viewmodels.Page1ViewModel

class Page1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPage1Binding
    private val viewModel: Page1ViewModel by viewModels()

    private lateinit var currentUser: AppUser

    // Fixed participant slots (teacher + 3 students) mirroring Figma layout
    private val participantSlots = listOf(
        "teacher" to "교사",
        "student1" to "학생1",
        "student2" to "학생2",
        "student3" to "학생3"
    )

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
            val panels = participantSlots.map { (uid, name) ->
                val role = if (uid == "teacher") "teacher" else "student"
                val note = notes.find { it.userId == uid }
                UserPanelAdapter.PanelItem(uid, name, role, note)
            }
            binding.rvPanels.adapter = UserPanelAdapter(panels) { item ->
                // Only current user can open their own writing screen
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
}
