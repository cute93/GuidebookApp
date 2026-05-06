# GuidebookApp 온보딩 가이드

## 1. 프로젝트 개요

**함께 만드는 지도서**는 교사와 학생이 안드로이드 태블릿에서 함께 수학·과학 문제를 풀어가는 협업 앱입니다.
교사가 문제 이미지를 업로드하면, 교사와 학생(최대 3명)이 각각 스타일러스 펜으로 풀이를 작성하고,
네 사람의 메모가 2×2 그리드에 실시간으로 공유됩니다.

- 플랫폼: Android (Kotlin), 최소 SDK 26 / 대상 SDK 35
- 백엔드: Firebase Auth + Firestore(DB), Cloudinary(이미지 저장소)

---

## 2. 디렉토리 구조

```
GuidebookApp/
├── app/src/main/java/com/example/guidebook/
│   ├── activities/          # 화면 진입점 (Activity 4개)
│   ├── viewmodels/          # UI 상태 관리 (ViewModel 3개)
│   ├── repository/          # 데이터 계층 — Firebase·Cloudinary 호출
│   │   ├── GuidebookRepository.kt
│   │   └── CloudinaryHelper.kt
│   ├── models/              # 데이터 클래스 (AppUser, Problem, UserNote)
│   ├── adapters/            # RecyclerView 어댑터 (2×2 패널)
│   └── views/               # 커스텀 뷰 (스타일러스 드로잉 캔버스)
│
├── app/src/test/            # 단위 테스트 (JUnit + MockK)
├── app/src/androidTest/     # 계측 테스트 (Espresso)
└── app/build.gradle         # 의존성 및 BuildConfig(Cloudinary 키)
```

| 폴더 | 역할 |
|---|---|
| `activities/` | 각 화면 UI 바인딩·이벤트 처리, ViewModel 관찰 |
| `viewmodels/` | LiveData로 UI 상태 노출, Repository 호출 (코루틴) |
| `repository/` | Firebase·Cloudinary 단일 진입점, suspend 함수로 제공 |
| `models/` | Firestore 직렬화용 data class (기본값 필수) |
| `adapters/` | 2×2 그리드의 각 사용자 패널 렌더링 |
| `views/` | 오프스크린 Bitmap에 경로를 누적하는 드로잉 캔버스 |

---

## 3. 핵심 파일 5개

### ① `repository/GuidebookRepository.kt`
모든 Firebase·Cloudinary 호출을 담당하는 단일 데이터 계층입니다.
`login()`, `getProblems()`, `uploadProblem()`, `getUserNotes()`, `uploadNoteDrawing()` 등
suspend 함수로 제공하며 결과는 `Result<T>`로 래핑합니다.

### ② `repository/CloudinaryHelper.kt`
Cloudinary REST API를 직접 호출하는 object 싱글톤입니다.
`Bitmap` 또는 `ByteArray`를 받아 SHA-1 HMAC 서명 후 멀티파트 업로드하고 `secure_url`을 반환합니다.
API 자격증명은 `local.properties`에서 `BuildConfig`로 주입됩니다(소스에 하드코딩 없음).

### ③ `activities/Page1Activity.kt`
앱의 메인 화면입니다. 문제 목록 페이지네이션(이전/다음), 문제 이미지 표시,
2×2 RecyclerView 패널 렌더링을 담당합니다.
역할에 따라 조건부 UI를 적용합니다(교사만 문제 업로드 버튼 노출).

### ④ `views/DrawingView.kt`
커스텀 View로, `Path` + `Paint` 리스트를 오프스크린 `Bitmap`에 누적합니다.
스타일러스 입력(`SOURCE_STYLUS`) 감지 시 압력값(`event.pressure`)을 선 굵기에 반영합니다.
`getBitmap()`으로 최종 이미지를 추출해 업로드에 사용합니다.

### ⑤ `models/AppUser.kt`
Firebase Auth UID와 사용자 프로필을 묶는 data class입니다.
`Serializable`을 구현해 Activity 간 Intent extras로 전달됩니다.
`role` 필드(`"teacher"` | `"student"`)가 앱 전체의 권한 분기 기준입니다.

---

## 4. 주요 코드 흐름

### 로그인 흐름
```
LoginActivity
  → GuidebookRepository.login(email, pw)
      → Firebase Auth signIn → Firestore users/{uid} 조회
      → AppUser 반환
  → Intent("user" = appUser) → Page1Activity
```

### 문제 목록 로드 및 메모 표시 (Page1)
```
Page1Activity.onCreate()
  → Page1ViewModel.init(user) → repo.getProblems()
  → problems LiveData 업데이트 → 문제 이미지·제목 표시
  → viewModel.refreshNotes() → repo.getUserNotes(problemId)
  → notes LiveData → UserPanelAdapter → 2×2 그리드 렌더링
```

### 드로잉 저장 흐름 (Page2)
```
Page2Activity (저장 버튼 클릭)
  → DrawingView.getBitmap()          // 오프스크린 Bitmap 추출
  → Page2ViewModel.uploadDrawing()
      → GuidebookRepository.uploadNoteDrawing()
          → CloudinaryHelper.uploadBitmap()  // Cloudinary REST
          → Firestore notes/{problemId}_{userId} 저장
  → SaveState.Uploaded → Toast + finish()
```

### 문제 업로드 흐름 (Page3 — 교사 전용)
```
Page3Activity (업로드 버튼 클릭)
  → 갤러리에서 이미지 Uri 선택
  → Page3ViewModel.uploadProblem(title, subject, uri)
      → GuidebookRepository.uploadProblem()
          → CloudinaryHelper.uploadBytes()   // Cloudinary REST
          → Firestore problems/{problemId} 저장
  → 완료 시 finish() → Page1 onResume() 자동 갱신
```

---

## 5. 개발 시작하기

### 사전 준비
- Android Studio Hedgehog 이상
- JDK 17
- Firebase 프로젝트의 `google-services.json` → `app/` 디렉토리에 배치
- Cloudinary 계정의 API Key·Secret 발급

### local.properties 설정
```properties
# Cloudinary (필수)
CLOUDINARY_CLOUD_NAME=dulqmyj05
CLOUDINARY_API_KEY=<your_key>
CLOUDINARY_API_SECRET=<your_secret>

# 릴리스 서명 (선택 — 디버그 빌드 시 불필요)
SIGNING_STORE_FILE=path/to/keystore.jks
SIGNING_STORE_PASSWORD=...
SIGNING_KEY_ALIAS=...
SIGNING_KEY_PASSWORD=...
```

> `local.properties`는 `.gitignore`에 포함되어 있습니다. 절대 커밋하지 마세요.

### 빌드 및 실행
```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 연결된 디바이스/에뮬레이터에 설치 후 실행
./gradlew installDebug

# 전체 빌드 (모든 변형)
./gradlew build
```

### 테스트
```bash
# 단위 테스트 (JVM, 에뮬레이터 불필요)
./gradlew test

# 계측 테스트 (연결된 디바이스 또는 에뮬레이터 필요)
./gradlew connectedAndroidTest

# 린트 검사
./gradlew lint
```

### Firebase 테스트 계정 (개발용)
Firestore `users` 컬렉션에 직접 문서를 추가하거나, Firebase Console에서 Auth 계정을 생성합니다.
`role` 필드를 `"teacher"` 또는 `"student"`로 설정해야 앱이 올바르게 동작합니다.

---

## 참고 문서

- [`CLAUDE.md`](CLAUDE.md) — 아키텍처 상세 및 Firestore 스키마
- [`README_SETUP.md`](README_SETUP.md) — Firebase·Cloudinary 초기 세팅 절차
