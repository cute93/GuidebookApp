# Firestore 실시간 갱신 — 문제 목록 자동 동기화

**날짜:** 2026-04-30
**프로젝트:** GuidebookApp
**소요시간:** 약 1시간

## 배경

교사가 문제를 업로드해도 학생 화면에서는 앱을 재시작하거나 Page1을 나갔다 들어와야 새 문제가 보이는 문제가 있었음. 실시간 협업 앱에서 치명적인 UX 결함.

## 상세 내용

**기존 구조 (문제)**
- `GuidebookRepository.getProblems()`: Firestore 일회성 `.get()` 쿼리
- `Page1ViewModel.init()` 호출 시 한 번만 로드, 이후 갱신 없음
- `onResume()`에서는 노트(메모)만 갱신, 문제 목록은 갱신 안 됨

**변경 내용**
- `getProblems()` → `observeProblems()`: `callbackFlow` + `addSnapshotListener`로 교체
- ViewModel에서 `collect`로 실시간 스트림 구독
- 새 문제 목록 수신 시, 현재 선택된 문제가 변경된 경우에만 노트 재로드 (불필요한 요청 방지)
- ViewModel 소멸 시 `awaitClose { listener.remove() }`로 리스너 자동 해제

변경 파일 2개: `GuidebookRepository.kt`, `Page1ViewModel.kt`

## 결과 및 성과

- 교사 문제 업로드 → 학생 화면 즉시 자동 반영 (앱 재시작 불필요)
- 리스너 메모리 누수 없음 (ViewModel 생명주기에 바인딩)

## 기술적 의사결정

- `StateFlow` 대신 `callbackFlow` 선택: Firestore 콜백 기반 API를 Flow로 래핑하는 표준 패턴. ViewModel에서 `viewModelScope`로 수집하므로 생명주기 관리 자동화.
- 노트 재로드 조건: 현재 문제 ID 변경 시에만 → 다른 사용자의 문제 추가 시 내가 보던 문제 노트가 불필요하게 재요청되는 것 방지.

## 회고

초기 설계 시 `addSnapshotListener`를 고려했어야 했음. 실시간 협업 앱에서는 기본적으로 스냅샷 리스너를 사용하는 것이 맞음.
