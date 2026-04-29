# 빌드 및 Lint 검증 과정

작성일: 2026-04-29 | 대상 브랜치: `master`

---

## 1. 빌드 검증

### 1-1. Gradle 래퍼 누락

첫 빌드 시도에서 `gradlew` / `gradlew.bat`이 저장소에 없어 명령 자체가 실행되지 않았습니다.

```
The term '.\gradlew' is not recognized as the name of a cmdlet...
```

캐시된 Gradle 8.11.1 배포본으로 래퍼를 재생성했습니다.

```bash
gradle wrapper --gradle-version 8.11.1
# → gradlew, gradlew.bat 생성
```

### 1-2. 런처 아이콘 리소스 누락

래퍼 생성 후 빌드에서 새로운 오류 발생.

```
ERROR: resource mipmap/ic_launcher not found
ERROR: resource mipmap/ic_launcher_round not found
```

`AndroidManifest.xml`이 `@mipmap/ic_launcher`를 참조하지만 `res/mipmap*` 폴더가 없었습니다.
`minSdk 26`이므로 `mipmap-anydpi-v26` Adaptive Icon XML로 해결했습니다.

**생성 파일:**

| 파일 | 내용 |
|---|---|
| `res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive Icon 정의 |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | 원형 Adaptive Icon 정의 |
| `res/drawable/ic_launcher_foreground.xml` | 포그라운드 벡터 드로어블 |
| `res/values/colors.xml` | `ic_launcher_background` 색상 추가 |

### 1-3. 빌드 결과

```
./gradlew assembleDebug

BUILD SUCCESSFUL in 58s
40 actionable tasks: 30 executed, 10 up-to-date
```

---

## 2. Lint 검증

### 2-1. 1차 실행 결과: ERROR 2개

```
./gradlew lint

Lint found 2 errors, 57 warnings.
```

---

**오류 1 — `DrawingView.kt:102` `[NewApi]`**

```
paths.removeLast()
Error: Call requires API level 35 (current min is 26)
```

`removeLast()`는 API 35 미만에서는 Kotlin stdlib 확장함수로 동작하지만,
API 35부터는 `java.util.List#removeLast`로 해석됩니다.
lint가 `minSdk 26`과 충돌하는 API 호환성 오류로 탐지했습니다.

```kotlin
// 수정 전
paths.removeLast()

// 수정 후
paths.removeAt(paths.size - 1)
```

---

**오류 2 — `Page2ViewModel.kt:15` `[NullSafeMutableLiveData]`**

```
_saveState.value = null
Error: Cannot set non-nullable LiveData value to null
```

`MutableLiveData<SaveState>`로 선언된 non-nullable LiveData에 `null`을 대입하고 있었습니다.

```kotlin
// 수정 전
private val _saveState = MutableLiveData<SaveState>()
val saveState: LiveData<SaveState> = _saveState

// 수정 후
private val _saveState = MutableLiveData<SaveState?>()
val saveState: LiveData<SaveState?> = _saveState
```

---

### 2-2. 2차 실행 결과: ERROR 0개

```
./gradlew lint

BUILD SUCCESSFUL in 31s
Lint: 0 errors, 57 warnings
```

HTML 리포트: `app/build/reports/lint-results-debug.html`

---

### 2-3. 잔여 경고 (57개, 기능 영향 없음)

| 경고 유형 | 내용 |
|---|---|
| `[GradleDependency]` | 의존성 신규 버전 권고 (androidx.core 1.12→1.18 등) |
| `[SelectedPhotoAccess]` | Android 14 부분 사진 접근 권한 미처리 |
| `[Deprecated]` | `getSerializableExtra` — API 33 이전 방식 사용 |
