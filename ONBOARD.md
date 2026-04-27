# ONBOARD.md

GuidebookApp 신규 개발자를 위한 온보딩 문서입니다.

---

## 프로젝트 개요

**함께 만드는 지도서** — 교사와 학생이 함께 교육 문제를 풀고 주석을 달 수 있는 안드로이드 태블릿 앱.

- 교사가 문제 이미지를 업로드하면, 교사와 학생이 각각 스타일러스 펜으로 풀이를 작성
- 작성된 메모는 2×2 그리드 뷰에서 모두가 공유
- 언어: Kotlin / 최소 SDK 26 / 대상 SDK 35

---

## 빌드 명령어

```bash
./gradlew assembleDebug       # 디버그 APK 빌드
./gradlew assembleRelease     # 릴리스 APK 빌드
./gradlew build               # 전체 빌드
./gradlew lint                # 린트 검사
./gradlew test                # 단위 테스트
```

---

## 전체 파일 목록 (14개)

```
activities/
  LoginActivity.kt        # 앱 진입점, 로그인 처리
  Page1Activity.kt        # 메인 허브 — 문제 목록 + 2×2 메모 그리드
  Page2Activity.kt        # 드로잉 캔버스 (스타일러스)
  Page3Activity.kt        # 문제 업로드 (교사 전용)

viewmodels/
  Page1ViewModel.kt
  Page2ViewModel.kt
  Page3ViewModel.kt

repository/
  GuidebookRepository.kt  # Firebase + Cloudinary 모든 I/O 집중
  CloudinaryHelper.kt     # 이미지 업로드 서명 생성

models/
  AppUser.kt              # 전 화면에 전달되는 사용자 객체
  Problem.kt
  UserNote.kt

views/
  DrawingView.kt          # 압력 감지 스타일러스 캔버스 (핵심 기술)

adapters/
  UserPanelAdapter.kt     # 2×2 메모 패널 RecyclerView 어댑터
```

---

## 아키텍처

**패턴:** MVVM + Activity 기반 내비게이션 (Fragment / NavComponent 미사용)

### 화면 흐름

```
LoginActivity
  └─→ Page1Activity  (문제 브라우저 + 2×2 메모 그리드)
        ├─→ Page2Activity  (스타일러스 드로잉 — 모든 사용자)
        └─→ Page3Activity  (문제 업로드 — 교사 전용)
```

### 레이어 역할

| 레이어 | 위치 | 역할 |
|---|---|---|
| Activities | `activities/` | UI 렌더링, LiveData 관찰, 화면 전환 |
| ViewModels | `viewmodels/` | UI 상태 관리, Repository 호출, LiveData 노출 |
| Repository | `repository/GuidebookRepository.kt` | Firebase + Cloudinary 전담 |
| Models | `models/` | Firestore 문서 매핑 데이터 클래스 |
| Views | `views/DrawingView.kt` | 압력 감지 스타일러스 커스텀 View |
| Adapters | `adapters/UserPanelAdapter.kt` | 2×2 메모 패널 그리드 |

---

## 백엔드

- **Firebase Auth** — 이메일/비밀번호 로그인, `users/{uid}` Firestore에 프로필 저장
- **Firestore** — `problems/`, `notes/` 컬렉션
- **Cloudinary** — 모든 이미지 저장 (Firebase Storage 미사용)

### Firestore 스키마

| 컬렉션 | 키 | 주요 필드 |
|---|---|---|
| `users/{uid}` | Firebase UID | uid, name, email, role |
| `problems/{problemId}` | 자동 ID | id, title, imageUrl, subject, createdAt, teacherId |
| `notes/{problemId}_{userId}` | 복합 키 | id, problemId, userId, userName, role, noteImageUrl, updatedAt |

노트는 `{problemId}_{userId}` 키로 저장 — 사용자당 문제당 1개, 재업로드 시 덮어쓰기.

### Cloudinary 업로드 경로

| 대상 | 경로 |
|---|---|
| 문제 이미지 | `/problems/{problemId}` |
| 메모 드로잉 | `/notes/{problemId}_{userId}.png` |

인증은 `CloudinaryHelper.kt`에서 SHA-1 HMAC 서명으로 처리.

---

## 버튼 클릭 → 화면 전환 코드 흐름

### 1. 로그인 버튼 → Page1

```
LoginActivity.kt:33  btnLogin.setOnClickListener
  └─ lifecycleScope.launch
       └─ repo.login(email, pw)
            └─ .onSuccess { user }
                 └─ navigateToPage1(user)          // :58
                      └─ startActivity(→ Page1Activity)
                           putExtra("user", user)
                           flags = NEW_TASK | CLEAR_TASK   // 백 스택 제거
```

### 2. 메모 패널 클릭 → Page2

```
Page1Activity.kt:94  UserPanelAdapter { item ->
  └─ 조건: item.userId == currentUser.uid
           또는 교사가 교사 패널 클릭
       ├─ [통과] startActivity(→ Page2Activity)   // :99
       │          putExtra("user", "problemId", "problemImageUrl", "problemTitle")
       └─ [실패] Toast "자신의 해결포인트만 작성할 수 있습니다."
```

### 3. 문제 업로드 버튼 → Page3 (교사만 버튼 표시)

```
Page1Activity.kt:61  btnUploadProblem.setOnClickListener
  └─ startActivity(→ Page3Activity)
       putExtra("user", currentUser)
```

### 4. Page2 업로드 완료 → Page1 복귀

```
Page2Activity.kt:60  btnUpload.setOnClickListener
  └─ viewModel.uploadDrawing(...)
       └─ saveState → Uploaded
            └─ finish()   // :81
                 └─ Page1Activity.onResume() → viewModel.refreshNotes()
```

### 5. Page3 업로드 완료 → Page1 복귀

```
Page3Activity.kt:44  btnUploadProblem.setOnClickListener
  └─ viewModel.uploadProblem(...)
       └─ uploadState → Success
            └─ finish()   // :72
                 └─ Page1Activity.onResume() → viewModel.refreshNotes()
```

---

## 핵심 구현 패턴

| 패턴 | 설명 |
|---|---|
| `Intent + putExtra` | 화면 간 데이터 전달 방식 (모든 화면 공통) |
| `AppUser` Serializable | 로그인 후 전 화면에 사용자 정보 전파 |
| `finish()` | 하위 화면 종료 시 자동 상위 화면 복귀 |
| `onResume()` | Page1 복귀 시마다 노트 목록 새로고침 |
| `LiveData observe` | ViewModel 작업 완료 시 UI 전환 트리거 |
| `viewModelScope.launch` + `await()` | 모든 Firebase 비동기 처리 |
| `role == "teacher"` 분기 | 교사 전용 기능 접근 제어 |

---

## 권장 코드 읽기 순서

처음 투입된 경우 아래 순서로 읽으면 전체 구조를 빠르게 파악할 수 있습니다.

```
1. GuidebookRepository.kt  — 백엔드 연동 전체 파악
2. AppUser.kt              — 사용자 모델 및 role 구조 이해
3. Page1Activity.kt        — 앱의 메인 허브 흐름 파악
4. DrawingView.kt          — 핵심 기술(스타일러스) 이해
```
