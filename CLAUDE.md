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
- DrawingView는 오프스크린 Bitmap에 렌더링 후 JPEG 75%로 압축해 업로드 (GuidebookRepository.uploadNoteDrawing()). MotionEvent.TOOL_TYPE_STYLUS의 압력 데이터를 활용해 선 굵기를 조절합니다. `getBitmap()` / `loadBitmap(bitmap)` 으로 드래프트 저장·복원을 지원합니다.
- Page1Activity는 교사 패널(위, 크게) + 학생 3개 패널(아래, 작게)을 직접 뷰 바인딩으로 표시합니다. `bindPanel()` 헬퍼로 권한·이미지·클릭 처리를 일괄 담당합니다.
- Page2Activity는 `lifecycleScope + Dispatchers.IO` 로 로컬 드래프트를 `filesDir/drafts/{problemId}_{uid}.png` 에 저장하고, 진입 시 `drawingView.post { loadDraftIfExists() }` 로 복원합니다 (post() 없이 호출하면 canvasBitmap이 null 상태).
- Page3Activity는 조건부 접근 가능 — 툴바 버튼은 appUser.role == "teacher"일 때만 나타납니다.
- 모든 비동기 작업은 Kotlin 코루틴(viewModelScope.launch)을 사용합니다. Firebase 작업은 kotlinx-coroutines-play-services의 await()로 처리됩니다.
- Repository의 문제 목록 조회는 `observeProblems(): Flow<Result<List<Problem>>>` — 구 `getProblems()` 메서드는 삭제됨. 테스트에서는 `every { mockRepo.observeProblems() } returns flowOf(...)` 패턴 사용.

## 빌드 오류 해결 이력

### 1. `gradle-wrapper.jar` 누락
**증상:** `./gradlew` 실행 시 `bash: ./gradlew: No such file or directory` 또는 jar 관련 오류  
**원인:** `.gitignore`의 `*.jar` 규칙으로 `gradle/wrapper/gradle-wrapper.jar`가 추적되지 않음  
**해결:**
```bash
# 방법 A — 다른 브랜치/캐시에서 복사
cp .claude/worktrees/<worktree-path>/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/

# 방법 B — Gradle Wrapper 재생성 (Gradle 설치된 환경)
gradle wrapper --gradle-version 8.11.1
```
> `.gitignore`에서 `!gradle/wrapper/gradle-wrapper.jar` 예외 규칙을 추가하거나, CI에서 `gradle wrapper` 단계를 두는 것이 근본 해결책.

---

### 2. `local.properties` 누락
**증상:** `Could not find method signingConfig()` 또는 `storeFile file('')` 관련 빌드 오류  
**원인:** `local.properties`는 `.gitignore`에 포함되어 클론 직후 없음. `build.gradle`이 서명 설정을 이 파일에서 읽음  
**해결:** 루트에 `local.properties` 생성:
```properties
sdk.dir=C\:/Users/<사용자명>/AppData/Local/Android/Sdk
SIGNING_STORE_FILE=placeholder.jks
SIGNING_STORE_PASSWORD=placeholder
SIGNING_KEY_ALIAS=placeholder
SIGNING_KEY_PASSWORD=placeholder
```
> 릴리스 빌드가 필요한 경우 실제 keystore 경로와 비밀번호로 교체.

---

### 3. 단위테스트 컴파일 오류 — 구 Repository API 참조
**증상:** `./gradlew test` 실행 시 `Unresolved reference: getProblems`  
**원인:** `GuidebookRepository`가 `getProblems()` → `observeProblems(): Flow<>` 로 교체됐으나 `Page1ViewModelTest.kt`가 미갱신  
**해결:** 테스트 파일의 mock 패턴 교체:
```kotlin
// 변경 전 (오류)
coEvery { mockRepo.getProblems() } returns Result.success(listOf(...))

// 변경 후 (정상)
every { mockRepo.observeProblems() } returns flowOf(Result.success(listOf(...)))
// import kotlinx.coroutines.flow.flowOf 추가 필요
```