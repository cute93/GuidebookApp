# GuidebookApp 검증 과정 기록

작성일: 2026-04-29 | 대상 브랜치: `master`

---

## 배경

GuidebookApp은 교사·학생이 함께 교육 문제를 풀고 주석을 공유하는 Android 태블릿 앱입니다.
이번 작업 시작 전 `app/src/test`, `app/src/androidTest` 디렉터리가 완전히 비어 있었고,
Cloudinary API 시크릿이 소스코드에 하드코딩된 상태였습니다.

**목표:** 빌드 가능 여부 확인 → 단위 테스트·계측 테스트 추가 → 자격증명 보안 처리

---

## 단계 0 — 검증 플랜 검토 및 보완

실행 전 `lovely-tinkering-tide.md` 플랜을 검토하여 아래 4가지를 보완했습니다.

| 항목 | 문제 | 조치 |
|---|---|---|
| 파일 목록 | `ProblemTest.kt`, `UserNoteTest.kt` 표에는 있으나 파일 목록에 누락 | 목록에 추가 |
| 코루틴 설정 | `Dispatchers.setMain(UnconfinedTestDispatcher())` 패턴 미포함 | 예시 코드 추가 |
| `testOptions` | `unitTests.returnDefaultValues = true` 설정 미언급 | 지침에 추가 |
| 보안 권고 | `BuildConfig` APK 리버싱 노출 한계 미언급 | 장기 권고 포함하여 보강 |

---

## 단계 1 — 빌드 환경 점검

### 1-1. Gradle 래퍼 누락

`gradlew` / `gradlew.bat`이 저장소에 없어 빌드 명령 자체가 불가능한 상태였습니다.

```
The term '.\gradlew' is not recognized as the name of a cmdlet...
```

**원인:** Gradle Wrapper 파일이 `.gitignore`되거나 커밋되지 않은 채로 저장소에 올라옴.  
**조치:** 캐시된 Gradle 8.11.1 배포본(`~/.gradle/wrapper/dists/`)을 사용해 래퍼를 재생성.

```bash
gradle wrapper --gradle-version 8.11.1
# → gradlew, gradlew.bat 생성
```

### 1-2. 런처 아이콘 리소스 누락

래퍼 생성 후 첫 빌드에서 새로운 오류 발생.

```
ERROR: resource mipmap/ic_launcher not found
ERROR: resource mipmap/ic_launcher_round not found
```

**원인:** `AndroidManifest.xml`이 `@mipmap/ic_launcher`를 참조하지만 `res/mipmap*` 폴더가 모두 없음.  
**조치:** `minSdk 26`이므로 `mipmap-anydpi-v26` Adaptive Icon XML만으로 충분.

생성 파일:
- `res/mipmap-anydpi-v26/ic_launcher.xml`
- `res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `res/drawable/ic_launcher_foreground.xml`
- `res/values/colors.xml` — `ic_launcher_background` 색상 추가

**빌드 결과:**
```
BUILD SUCCESSFUL in 58s
```

---

## 단계 2 — 테스트 의존성 추가 (`app/build.gradle`)

### 추가 내용

```gradle
android {
    buildFeatures {
        buildConfig true          // BuildConfig 생성 활성화
    }
    testOptions {
        unitTests.returnDefaultValues = true  // Android 프레임워크 Log 등 호출 시 RuntimeException 방지
    }
}

dependencies {
    // 단위 테스트
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'io.mockk:mockk:1.13.10'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'  // 프로젝트와 버전 일치
    testImplementation 'androidx.arch.core:core-testing:2.2.0'               // InstantTaskExecutorRule

    // 계측 테스트
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test:rules:1.5.0'
}
```

---

## 단계 3 — 단위 테스트 작성

### 3-1. 모델 테스트 (Firebase 의존 없음)

| 파일 | 검증 항목 |
|---|---|
| `AppUserTest.kt` | 기본 `role="student"`, 전체 필드 기본값, Serializable 직렬화 무결성, `copy()` 후 불변성 |
| `ProblemTest.kt` | `createdAt=0L`, 전체 필드 기본값, 필드 설정 정확성, `copy()` 후 불변성 |
| `UserNoteTest.kt` | `id = "{problemId}_{userId}"` 키 형식, 기본 `role="student"`, `updatedAt=0L`, 빈 필드 기본값 |

### 3-2. ViewModel 테스트 — 첫 번째 시도 (실패)

처음에는 `mockkConstructor(GuidebookRepository::class)` + `mockkStatic(FirebaseAuth::class)`를 사용했지만, 모든 ViewModel 테스트가 `AssertionError`로 실패했습니다.

```
Page1ViewModelTest > currentProblem returns first problem initially FAILED
Page2ViewModelTest > uploadDrawing sets Uploaded state on success FAILED
...
```

**원인 분석:**  
`GuidebookRepository`가 ViewModel 내부에서 직접 생성(`private val repo = GuidebookRepository()`)되므로, `mockkConstructor`가 인터셉트하더라도 `FirebaseAuth.getInstance()` 등의 초기화 코드가 실행되어 Mock 체인이 불안정했습니다.

### 3-3. ViewModel 테스트 — 해결책: Repository 생성자 주입

ViewModel 3개에 Repository를 생성자 파라미터로 주입하도록 수정했습니다.  
기존 Activity(`by viewModels()`)는 기본값이 있어 변경 불필요합니다.

```kotlin
// 변경 전
class Page1ViewModel : ViewModel() {
    private val repo = GuidebookRepository()

// 변경 후
class Page1ViewModel(
    private val repo: GuidebookRepository = GuidebookRepository()
) : ViewModel() {
```

`Page2ViewModel`, `Page3ViewModel`도 동일하게 변경.

테스트에서는 Firebase 관련 설정 없이 순수하게 Mock 주입:

```kotlin
private val mockRepo = mockk<GuidebookRepository>(relaxed = true)
private lateinit var vm: Page1ViewModel

@Before fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    vm = Page1ViewModel(repo = mockRepo)   // 직접 주입
}
```

### 3-4. 작성된 ViewModel 테스트 케이스

| 파일 | 테스트 케이스 |
|---|---|
| `Page1ViewModelTest.kt` | `currentProblem` null(빈 목록) / 비null(첫 항목), `goToNext` 경계값(마지막에서 이동 안함), `goToPrev` 경계값(0에서 이동 안함), 앞뒤 이동 후 복귀, 에러 LiveData 전파 |
| `Page2ViewModelTest.kt` | 초기 `saveState=null`, 업로드 성공 → `Uploaded`, 업로드 실패 → `Error(message)` |
| `Page3ViewModelTest.kt` | 초기 `uploadState=null`, 빈 제목 → 즉시 `Error`(repo 호출 없음 검증), 공백 제목 → `Error`, 성공 → `Success`, 실패 → `Error(message)` |

**위치:** `app/src/test/java/com/example/guidebook/`

---

## 단계 4 — 계측(Instrumented) 테스트 작성

에뮬레이터/실기기 연결 후 `./gradlew connectedCheck`로 실행합니다.

| 파일 | 검증 항목 |
|---|---|
| `DrawingViewTest.kt` | `setColor` 호출 후 `getBitmap()` 반환, `clear()` 후 흰 배경 픽셀 검증, 빈 상태 `undo()` 크래시 없음, `setStrokeWidth`·`setEraserMode` 정상 동작 |
| `LoginActivityTest.kt` | 버튼·입력란 표시, 빈 입력 시 버튼 여전히 활성, 이메일·비밀번호 입력 후 버튼 활성 |
| `Page1ActivityRoleTest.kt` | `role="student"` → `btnUploadProblem` GONE, `role="teacher"` → VISIBLE, 이전/다음 버튼 표시 확인 |

**위치:** `app/src/androidTest/java/com/example/guidebook/`

---

## 단계 5 — 보안: Cloudinary 자격증명 분리

### 문제

`CloudinaryHelper.kt`에 API 시크릿이 하드코딩되어 있었고, `local.properties`가 `.gitignore` 없이 git에 추적되고 있었습니다.

```kotlin
// 변경 전 — 소스코드에 직접 노출
private const val API_SECRET = "ycPjrxl78C6cM7IJL-bU2f82IXY"
```

### 조치 순서

**① `local.properties`에 자격증명 이동**
```properties
CLOUDINARY_CLOUD_NAME=dulqmyj05
CLOUDINARY_API_KEY=135822862878268
CLOUDINARY_API_SECRET=ycPjrxl78C6cM7IJL-bU2f82IXY
```

**② `app/build.gradle`에서 `BuildConfig` 필드로 주입**
```gradle
def props = new Properties()
def localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) props.load(localPropsFile.newDataInputStream())
buildConfigField "String", "CLOUDINARY_CLOUD_NAME",  "\"${props.getProperty('CLOUDINARY_CLOUD_NAME',  'dulqmyj05')}\""
buildConfigField "String", "CLOUDINARY_API_KEY",    "\"${props.getProperty('CLOUDINARY_API_KEY',    '')}\""
buildConfigField "String", "CLOUDINARY_API_SECRET", "\"${props.getProperty('CLOUDINARY_API_SECRET', '')}\""
```

**③ `CloudinaryHelper.kt` 참조 변경**
```kotlin
// 변경 후 — BuildConfig 참조
private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
private val API_KEY    = BuildConfig.CLOUDINARY_API_KEY
private val API_SECRET = BuildConfig.CLOUDINARY_API_SECRET
```

**④ `.gitignore` 생성 및 추적 해제**
```bash
git rm --cached local.properties
# .gitignore에 local.properties 추가
```

### 한계 및 장기 권고

| 구분 | 내용 |
|---|---|
| 단기 효과 | git 저장소에서 키 제거 → 소스코드 노출 방지 |
| 남은 위험 | `BuildConfig` 필드는 APK 리버싱(jadx 등)으로 추출 가능 |
| 장기 권고 | 서버 측 서명 생성 프록시로 전환 — 클라이언트가 `API_SECRET`을 직접 보유하지 않도록 |

---

## 단계 6 — lint 실행 및 버그 수정

### 1차 lint 결과: ERROR 2개

**오류 1 — `DrawingView.kt:102` `[NewApi]`**

```
paths.removeLast()
// API 35에서 java.util.List#removeLast로 해석되어 minSdk 26과 충돌
```

`removeLast()`는 API 35 이전에는 Kotlin stdlib 확장함수로 동작하지만, API 35부터는 Java 메서드로 해석됩니다. lint가 이를 API 호환성 오류로 탐지했습니다.

```kotlin
// 수정
paths.removeAt(paths.size - 1)
```

**오류 2 — `Page2ViewModel.kt:15` `[NullSafeMutableLiveData]`**

```kotlin
// 문제: non-nullable LiveData에 null 대입
private val _saveState = MutableLiveData<SaveState>()
...
_saveState.value = null  // 타입 불일치
```

```kotlin
// 수정: nullable 타입으로 명시
private val _saveState = MutableLiveData<SaveState?>()
val saveState: LiveData<SaveState?> = _saveState
```

### 2차 lint 결과: ERROR 0개

```
BUILD SUCCESSFUL in 31s — Lint: 0 errors, 57 warnings
```

잔여 경고 57개 (기능 영향 없음):

| 경고 유형 | 내용 |
|---|---|
| `[GradleDependency]` | 의존성 신규 버전 존재 (androidx.core 1.12→1.18 등) |
| `[SelectedPhotoAccess]` | Android 14 부분 사진 접근 권한 미처리 권고 |
| `[Deprecated]` | `getSerializableExtra` API 33 이전 방식 사용 |

---

## 최종 결과

| 항목 | 결과 |
|---|---|
| `./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL |
| `./gradlew lint` (ERROR) | ✅ 0개 |
| `./gradlew test` (단위 테스트 28개) | ✅ 전체 통과 |
| `./gradlew connectedCheck` (계측 테스트) | 🔲 기기 연결 후 실행 필요 |
| Cloudinary 자격증명 git 추적 해제 | ✅ 완료 |

**단위 테스트 상세 (총 28개):**

| 파일 | 통과 |
|---|---|
| `AppUserTest` | 4 |
| `ProblemTest` | 4 |
| `UserNoteTest` | 5 |
| `Page1ViewModelTest` | 7 |
| `Page2ViewModelTest` | 3 |
| `Page3ViewModelTest` | 5 |

---

## 남은 수동 E2E 체크리스트

실기기 또는 에뮬레이터에서 직접 확인이 필요합니다.

### 교사 플로우
- [ ] 교사 계정 로그인 → Page1 진입
- [ ] "문제 업로드" 버튼 표시 확인
- [ ] Page3 → 이미지 선택 → 제목 입력 → 업로드 성공
- [ ] Page1에서 업로드한 문제 목록 반영 확인
- [ ] 교사 패널 → Page2 진입 → 드로잉 → 업로드 성공
- [ ] Page1 교사 패널에 드로잉 미리보기 반영 확인

### 학생 플로우
- [ ] 학생 계정 로그인 → Page1 진입
- [ ] "문제 업로드" 버튼 없음 확인
- [ ] 자신 패널 클릭 → Page2 진입 가능
- [ ] 다른 학생 패널 클릭 → Toast "자신의 해결포인트만 작성할 수 있습니다."
- [ ] 메모 작성 후 업로드 → Page1 패널 미리보기 반영

### 드로잉 도구 (Page2)
- [ ] 색상 전환 (검정/파랑/빨강)
- [ ] 스트로크 슬라이더 굵기 변화
- [ ] 지우개 모드 전환 후 삭제
- [ ] Undo — 마지막 획 제거
- [ ] Clear — 전체 초기화
- [ ] 스타일러스 압력에 따른 굵기 변화 (실물 기기 필요)

### 예외 처리
- [ ] 비밀번호 오타 → "로그인 실패" Toast
- [ ] Page3 이미지 없이 업로드 시도 → Toast
- [ ] Page3 제목 없이 업로드 시도 → "제목을 입력해주세요." Toast

---

## 변경 파일 목록

| 파일 | 변경 |
|---|---|
| `gradlew`, `gradlew.bat` | 신규 생성 (래퍼 복원) |
| `.gitignore` | 신규 생성 |
| `app/build.gradle` | `buildConfig true`, `testOptions`, BuildConfig 필드, 테스트 의존성 추가 |
| `local.properties` | Cloudinary 자격증명 추가 (git 미추적) |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | 신규 생성 |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | 신규 생성 |
| `res/drawable/ic_launcher_foreground.xml` | 신규 생성 |
| `res/values/colors.xml` | `ic_launcher_background` 색상 추가 |
| `repository/CloudinaryHelper.kt` | 하드코딩 → `BuildConfig` 참조 |
| `viewmodels/Page1ViewModel.kt` | `repo` 생성자 파라미터 주입 |
| `viewmodels/Page2ViewModel.kt` | `repo` 생성자 파라미터 주입, `LiveData<SaveState?>` 타입 수정 |
| `viewmodels/Page3ViewModel.kt` | `repo` 생성자 파라미터 주입 |
| `views/DrawingView.kt` | `removeLast()` → `removeAt(size-1)` |
| `test/.../AppUserTest.kt` | 신규 생성 |
| `test/.../ProblemTest.kt` | 신규 생성 |
| `test/.../UserNoteTest.kt` | 신규 생성 |
| `test/.../Page1ViewModelTest.kt` | 신규 생성 |
| `test/.../Page2ViewModelTest.kt` | 신규 생성 |
| `test/.../Page3ViewModelTest.kt` | 신규 생성 |
| `androidTest/.../DrawingViewTest.kt` | 신규 생성 |
| `androidTest/.../LoginActivityTest.kt` | 신규 생성 |
| `androidTest/.../Page1ActivityRoleTest.kt` | 신규 생성 |
