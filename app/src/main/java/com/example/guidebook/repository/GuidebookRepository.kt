package com.example.guidebook.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.guidebook.models.AppUser
import com.example.guidebook.models.Problem
import com.example.guidebook.models.UserNote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class GuidebookRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<AppUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        getUserProfile(uid) ?: error("사용자 프로필을 찾을 수 없습니다.")
    }

    suspend fun getUserProfile(uid: String): AppUser? {
        val doc = db.collection("users").document(uid).get().await()
        return if (doc.exists()) doc.toObject(AppUser::class.java) else null
    }

    fun getCurrentUid(): String? = auth.currentUser?.uid
    fun logout() = auth.signOut()

    // ── Problems ──────────────────────────────────────────────────────────────

    fun observeProblems(): Flow<Result<List<Problem>>> = callbackFlow {
        val listener = db.collection("problems")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(Problem::class.java) }
                    ?: emptyList()
                trySend(Result.success(list))
            }
        awaitClose { listener.remove() }
    }

    /**
     * 교사가 이미지(Uri)를 Cloudinary에 업로드 후 Firestore에 Problem 저장
     */
    suspend fun uploadProblem(
        title: String,
        subject: String,
        imageUri: Uri,
        context: android.content.Context
    ): Result<Problem> = runCatching {
        val uid = getCurrentUid() ?: error("로그인이 필요합니다.")
        val problemId = UUID.randomUUID().toString()

        // Uri → ByteArray
        val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
            ?: error("이미지를 읽을 수 없습니다.")

        // Cloudinary 업로드
        val imageUrl = CloudinaryHelper.uploadBytes(bytes, "problems/$problemId")

        val problem = Problem(
            id        = problemId,
            title     = title,
            subject   = subject,
            imageUrl  = imageUrl,
            createdAt = System.currentTimeMillis(),
            teacherId = uid
        )
        db.collection("problems").document(problemId).set(problem).await()
        problem
    }

    // ── UserNotes ─────────────────────────────────────────────────────────────

    suspend fun getUserNotes(problemId: String): Result<List<UserNote>> = runCatching {
        db.collection("notes")
            .whereEqualTo("problemId", problemId)
            .get().await()
            .documents.mapNotNull { it.toObject(UserNote::class.java) }
    }

    /**
     * 학생/교사가 그린 Bitmap을 Cloudinary에 업로드 후 Firestore 노트 저장
     */
    suspend fun uploadNoteDrawing(
        problemId: String,
        userId: String,
        userName: String,
        role: String,
        bitmap: Bitmap
    ): Result<UserNote> = runCatching {
        val noteId = "${problemId}_${userId}"

        // Bitmap → Cloudinary
        val url = CloudinaryHelper.uploadBitmap(bitmap, "notes/$noteId")

        val note = UserNote(
            id           = noteId,
            problemId    = problemId,
            userId       = userId,
            userName     = userName,
            role         = role,
            noteImageUrl = url,
            updatedAt    = System.currentTimeMillis()
        )
        db.collection("notes").document(noteId).set(note).await()
        note
    }
}
