# CLAUDE.md

## 작업규칙
- 모든 응답은 한국어로 작성

## 프로젝트 개요
- 함께 만드는 지도서 (GuidebookApp) — 교사와 학생이 함께 교육 문제를 해결하기 위한 안드로이드 태블릿 앱. 교사가 문제를 업로드하면, 교사와 학생이 각각 스타일러스 펜으로 주석을 달고, 메모는 2×2 그리드 뷰에서 공유됩니다.
- 언어: Kotlin
- 최소 SDK: 26 / 대상 SDK: 35
- 애플리케이션 ID: com.example.guidebook

## 빌드 명령어
./gradlew assembleDebug       # 디버그 APK 빌드
./gradlew assembleRelease     # 릴리스 APK 빌드
./gradlew build               # 전체 빌드 (모든 변형)
./gradlew lint                # 린트 검사 실행
./gradlew test                # 단위 테스트 실행

## 아키텍처

**Pattern:** MVVM with Activity-based navigation (no Fragments, no NavComponent).

### 화면흐름
LoginActivity
  └─→ Page1Activity  (문제 브라우저 + 2×2 메모 그리드)
        ├─→ Page2Activity  (스타일러스 드로잉 캔버스 — 모든 사용자)
        └─→ Page3Activity  (문제 업로드 — 교사 전용)
Activity 간 데이터는 Intent extras로 전달됩니다. AppUser는 Serializable로 구현되어 화면 간 전달됩니다.

### 레이어역할

| Layer | Location | Role |
|---|---|---|
| Activities | `activities/` | UI, observes ViewModel LiveData, handles navigation |
| ViewModels | `viewmodels/` | UI state, calls Repository, exposes LiveData |
| Repository | `repository/GuidebookRepository.kt` | All Firebase + Cloudinary operations |
| Models | `models/` | `AppUser`, `Problem`, `UserNote` data classes |
| Views | `views/DrawingView.kt` | Custom canvas with pressure-sensitive stylus input |
| Adapters | `adapters/UserPanelAdapter.kt` | 2×2 RecyclerView grid of teacher/student notes |

### 백엔드

-Firebase Auth — 이메일/비밀번호 로그인; 사용자 프로필은 Firestore users/{uid}에 저장
-Firestore — problems/ 및 notes/ 컬렉션 (아래 스키마 참고)
-Cloudinary — 모든 이미지 저장소 (문제 이미지 + 메모 드로잉). Firebase Storage는 사용하지 않음

### Firestore 스키마
-users/{uid}           uid, name, email, role ("teacher"|"student")
-problems/{problemId}  id, title, imageUrl, subject, createdAt, teacherId
-notes/{problemId}_{userId}   id, problemId, userId, userName, role, noteImageUrl, updatedAt
Notes are keyed as `{problemId}_{userId}` — one note per user per problem. Uploading again overwrites.

### Cloudinary
Credentials are hardcoded in `CloudinaryHelper.kt` (cloud name: `dulqmyj05`). Upload paths:
- Problems: `/problems/{problemId}`
- Notes: `/notes/{problemId}_{userId}.png`

Authentication uses SHA-1 HMAC signatures generated at upload time.

## Key Implementation Details
-DrawingView는 오프스크린 Bitmap에 렌더링 후 PNG로 업로드 (GuidebookRepository.uploadNoteDrawing()). MotionEvent.TOOL_TYPE_STYLUS의 압력 데이터를 활용해 선 굵기를 조절합니다.
-Page1Activity는 UserPanelAdapter를 사용해 최대 4개의 패널(교사 + 최대 3명의 학생)을 표시합니다. 현재 사용자 패널은 강조되거나 조작 가능하게 표시됩니다.
-Page3Activity는 조건부 접근 가능 — 툴바 버튼은 appUser.role == "teacher"일 때만 나타납니다.
-모든 비동기 작업은 Kotlin 코루틴(viewModelScope.launch)을 사용합니다. Firebase 작업은 kotlinx-coroutines-play-services의 await()로 처리됩니다.