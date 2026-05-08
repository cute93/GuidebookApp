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
-DrawingView는 오프스크린 Bitmap에 렌더링 후 PNG로 업로드 (GuidebookRepository.uploadNoteDrawing()). MotionEvent.TOOL_TYPE_STYLUS의 압력 데이터를 활용해 선 굵기를 조절합니다.
-Page1Activity는 교사 패널(위, 크게) + 학생 3개 패널(아래, 작게)을 고정 레이아웃으로 직접 바인딩합니다. bindPanel() 헬퍼로 각 슬롯의 이름·이미지·버튼을 처리합니다.
-Page3Activity는 조건부 접근 가능 — 툴바 버튼은 appUser.role == "teacher"일 때만 나타납니다.
-모든 비동기 작업은 Kotlin 코루틴(viewModelScope.launch)을 사용합니다. Firebase 작업은 kotlinx-coroutines-play-services의 await()로 처리됩니다.
-Page2Activity는 드래프트를 filesDir/drafts/{problemId}_{userId}.png에 JPEG 75%로 로컬 저장하고, 재진입 시 DrawingView.loadBitmap()으로 복원합니다.

## 빌드 오류 및 해결방법

### 1. gradle-wrapper.jar 없음
**증상:** `./gradlew` 실행 시 `gradle/wrapper/gradle-wrapper.jar` 없음 오류  
**원인:** `.gitignore`의 `*.jar` 규칙으로 인해 wrapper jar가 git에서 제외됨  
**해결:**
```bash
# 방법 A — git에서 jar 추적 허용 (.gitignore에 예외 추가)
echo '!gradle/wrapper/gradle-wrapper.jar' >> .gitignore
git add -f gradle/wrapper/gradle-wrapper.jar

# 방법 B — gradle wrapper 재생성 (Gradle 설치된 환경)
gradle wrapper --gradle-version 8.11.1
```

### 2. local.properties 없음으로 빌드 실패
**증상:** `storeFile file('')` 오류 또는 `sdk.dir` 미설정으로 빌드 실패  
**원인:** `local.properties`는 gitignore 대상 — 클론 후 항상 수동 생성 필요  
**해결:** 프로젝트 루트에 `local.properties` 생성
```properties
sdk.dir=C\:/Users/<사용자명>/AppData/Local/Android/Sdk
# 릴리스 서명이 불필요한 경우 아래 더미값으로 디버그 빌드 가능
SIGNING_STORE_FILE=placeholder.jks
SIGNING_STORE_PASSWORD=placeholder
SIGNING_KEY_ALIAS=placeholder
SIGNING_KEY_PASSWORD=placeholder
```

### 3. 단위테스트 컴파일 오류 — 삭제된 Repository 메서드 참조
**증상:** `./gradlew test` 실패, `Unresolved reference: getProblems`  
**원인:** Repository API가 `getProblems(): Result<List<Problem>>` → `observeProblems(): Flow<Result<List<Problem>>>` 로 변경됐으나 테스트 미갱신  
**해결:** `Page1ViewModelTest.kt`에서 MockK 방식 변경
```kotlin
// 수정 전 (구 API)
coEvery { mockRepo.getProblems() } returns Result.success(listOf(...))

// 수정 후 (Flow 기반)
import kotlinx.coroutines.flow.flowOf
every { mockRepo.observeProblems() } returns flowOf(Result.success(listOf(...)))
```

### 4. 레이아웃·Activity 불일치로 빌드 실패
**증상:** XML에서 뷰를 제거했는데 Activity에서 해당 binding 참조 시 컴파일 오류  
**원인:** `activity_page1.xml`에서 RecyclerView(`rvPanels`) 제거 후 `Page1Activity.kt`가 `binding.rvPanels` 여전히 참조  
**해결:** 레이아웃과 Activity를 항상 함께 수정. 뷰 ID를 삭제할 경우 해당 ID를 참조하는 모든 kt 파일 동시 업데이트