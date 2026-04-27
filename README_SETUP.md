# 함께 만드는 지도서 - Android 앱 설정 가이드

## Firebase 설정 (필수)

1. [Firebase Console](https://console.firebase.google.com/)에서 새 프로젝트 생성
2. Android 앱 추가 (`com.example.guidebook`)
3. `google-services.json` 다운로드 → `app/` 폴더에 복사
4. **Authentication** 활성화 → Email/Password 방식 켜기
5. **Firestore** 활성화 → 테스트 모드로 시작
6. **Storage** 활성화 → 테스트 모드로 시작

## Firestore 컬렉션 구조

```
users/{uid}
  - uid: String
  - name: String
  - email: String
  - role: "teacher" | "student"

problems/{problemId}
  - id: String
  - title: String
  - subject: String
  - imageUrl: String
  - createdAt: Long
  - teacherId: String

notes/{problemId}_{userId}
  - id: String
  - problemId: String
  - userId: String
  - userName: String
  - role: "teacher" | "student"
  - noteImageUrl: String
  - updatedAt: Long
```

## 사용자 등록

Firebase Console > Authentication에서 수동으로 사용자 추가 후,  
Firestore `users` 컬렉션에 해당 uid로 문서 생성 (role 포함).

## 앱 화면 구조

| 화면 | 설명 |
|------|------|
| **LoginActivity** | 이메일/비밀번호 로그인 |
| **Page1Activity** | 문제 뷰 + 교사/학생 패널 (썸네일) + 해결포인트작성 버튼 |
| **Page2Activity** | 태블릿 펜 필기 화면 + 저장/업로드 버튼 |
| **Page3Activity** | 교사 문제 이미지 업로드 화면 |
