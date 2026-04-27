const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  ImageRun, HeadingLevel, AlignmentType, BorderStyle, WidthType,
  ShadingType, PageBreak, Header, Footer, PageNumber, ExternalHyperlink
} = require('docx');
const fs = require('fs');

const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const borders = { top: border, bottom: border, left: border, right: border };

function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 300, after: 120 },
    children: [new TextRun({ text, bold: true, size: 32, color: "2E75B6" })]
  });
}
function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 200, after: 80 },
    children: [new TextRun({ text, bold: true, size: 26, color: "2E75B6" })]
  });
}
function p(text, options = {}) {
  return new Paragraph({
    spacing: { before: 60, after: 60 },
    children: [new TextRun({ text, size: 22, ...options })]
  });
}
function bullet(text) {
  return new Paragraph({
    numbering: { reference: "bullets", level: 0 },
    spacing: { before: 40, after: 40 },
    children: [new TextRun({ text, size: 22 })]
  });
}
function code(text) {
  return new Paragraph({
    spacing: { before: 40, after: 40 },
    shading: { fill: "F4F4F4", type: ShadingType.CLEAR },
    children: [new TextRun({ text, font: "Courier New", size: 18, color: "333333" })]
  });
}
function divider() {
  return new Paragraph({
    border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: "CCCCCC", space: 1 } },
    children: []
  });
}
function imgParagraph(imgPath, w, h) {
  if (!fs.existsSync(imgPath)) return p("[이미지 없음]", { color: "999999" });
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 120, after: 120 },
    children: [new ImageRun({
      type: "png",
      data: fs.readFileSync(imgPath),
      transformation: { width: w, height: h },
      altText: { title: "screenshot", description: "screenshot", name: "screenshot" }
    })]
  });
}

function chatBubble(role, text) {
  const isUser = role === "user";
  return new Table({
    width: { size: 9000, type: WidthType.DXA },
    columnWidths: isUser ? [1000, 8000] : [8000, 1000],
    rows: [new TableRow({
      children: [
        ...(isUser ? [] : [new TableCell({
          borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
          width: { size: 8000, type: WidthType.DXA },
          shading: { fill: "EEF4FB", type: ShadingType.CLEAR },
          margins: { top: 80, bottom: 80, left: 160, right: 160 },
          children: [
            new Paragraph({ children: [new TextRun({ text: "🤖 Claude", bold: true, size: 18, color: "2E75B6" })] }),
            new Paragraph({ children: [new TextRun({ text, size: 20 })] })
          ]
        })]),
        ...(isUser ? [new TableCell({
          borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
          width: { size: 8000, type: WidthType.DXA },
          shading: { fill: "FFF8E1", type: ShadingType.CLEAR },
          margins: { top: 80, bottom: 80, left: 160, right: 160 },
          children: [
            new Paragraph({ alignment: AlignmentType.RIGHT, children: [new TextRun({ text: "👤 사용자", bold: true, size: 18, color: "E65100" })] }),
            new Paragraph({ alignment: AlignmentType.RIGHT, children: [new TextRun({ text, size: 20 })] })
          ]
        })] : []),
        new TableCell({
          borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
          width: { size: 1000, type: WidthType.DXA },
          children: [new Paragraph({ children: [] })]
        })
      ]
    })]
  });
}

const screenshot = 'C:/Users/User/Documents/claude/GuidebookApp/screenshots/current.png';

const doc = new Document({
  numbering: {
    config: [{
      reference: "bullets",
      levels: [{ level: 0, format: "bullet", text: "•", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } } }]
    }]
  },
  styles: {
    default: { document: { run: { font: "맑은 고딕", size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, font: "맑은 고딕" },
        paragraph: { spacing: { before: 300, after: 120 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: "맑은 고딕" },
        paragraph: { spacing: { before: 200, after: 80 }, outlineLevel: 1 } },
    ]
  },
  sections: [{
    properties: {
      page: { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } }
    },
    headers: {
      default: new Header({ children: [new Paragraph({
        border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: "2E75B6", space: 1 } },
        children: [new TextRun({ text: "함께 만드는 지도서 — 앱 개발 세션 기록 (2026-04-27)", size: 18, color: "666666" })]
      })] })
    },
    footers: {
      default: new Footer({ children: [new Paragraph({
        alignment: AlignmentType.RIGHT,
        children: [new TextRun({ text: "Page ", size: 18 }), new TextRun({ children: [PageNumber.CURRENT], size: 18 })]
      })] })
    },
    children: [

      // ── 타이틀 ──────────────────────────────────────────────────
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 480, after: 120 },
        children: [new TextRun({ text: "함께 만드는 지도서", bold: true, size: 56, color: "2E75B6" })]
      }),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 80 },
        children: [new TextRun({ text: "Android 앱 개발 세션 전체 기록", size: 32, color: "555555" })]
      }),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 0, after: 480 },
        children: [new TextRun({ text: "2026년 4월 27일  |  Claude & 현준", size: 22, color: "888888" })]
      }),
      divider(),

      // ── 1. 프로젝트 개요 ─────────────────────────────────────────
      new Paragraph({ children: [new PageBreak()] }),
      h1("1. 프로젝트 개요"),
      p("Figma 디자인을 바탕으로 교사와 학생이 함께 문제 풀이 포인트를 작성하고 공유하는 Android 태블릿 앱을 개발했습니다."),
      new Paragraph({ spacing: { before: 100, after: 100 }, children: [] }),

      h2("Figma 원본 디자인"),
      bullet("파일명: Figma 함께만드는지도서"),
      bullet("URL: https://www.figma.com/design/AWxxaehyNVv25A1TjRxCBg/..."),
      new Paragraph({ spacing: { before: 80, after: 80 }, children: [] }),

      h2("앱 화면 구조 (3 Pages)"),
      new Table({
        width: { size: 9360, type: WidthType.DXA },
        columnWidths: [1500, 3000, 4860],
        rows: [
          new TableRow({ children: [
            new TableCell({ borders, width: { size: 1500, type: WidthType.DXA }, shading: { fill: "2E75B6", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "화면", bold: true, color: "FFFFFF", size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3000, type: WidthType.DXA }, shading: { fill: "2E75B6", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "역할", bold: true, color: "FFFFFF", size: 20 })] })] }),
            new TableCell({ borders, width: { size: 4860, type: WidthType.DXA }, shading: { fill: "2E75B6", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "주요 기능", bold: true, color: "FFFFFF", size: 20 })] })] }),
          ]}),
          ...[
            ["Page 1", "문제 뷰", "문제 이미지 + 교사/학생 필기 썸네일. 해결포인트작성 버튼 → Page2. 이전/다음 문제 네비게이션"],
            ["Page 2", "태블릿 필기", "문제 참조 + 펜 필기 캔버스. 저장하기(임시) / 업로드하기(Page1 반영)"],
            ["Page 3", "교사 업로드", "문제 이미지 업로드. 업로드 완료 시 Page1에 문제 생성"],
          ].map(([page, role, feat], i) => new TableRow({ children: [
            new TableCell({ borders, width: { size: 1500, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "EEF4FB" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: page, bold: true, size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3000, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "EEF4FB" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: role, size: 20 })] })] }),
            new TableCell({ borders, width: { size: 4860, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "EEF4FB" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: feat, size: 20 })] })] }),
          ]}))
        ]
      }),

      // ── 2. 대화 흐름 ─────────────────────────────────────────────
      new Paragraph({ children: [new PageBreak()] }),
      h1("2. 세션 대화 흐름"),

      // 대화 1
      h2("Step 1 — Figma 연결 확인"),
      chatBubble("user", "피그마와 연결확인 부탁해"),
      new Paragraph({ spacing: { before: 80, after: 80 }, children: [] }),
      chatBubble("claude", "Figma Web MCP 연결 확인 완료!\n• 계정: ddongtilee@daum.net\n• Figma Dev Mode MCP는 데스크탑 앱에서 별도 활성화 필요\n• Web MCP는 정상 연결됨"),

      new Paragraph({ spacing: { before: 160, after: 80 }, children: [] }),
      h2("Step 2 — 앱 구조 요청 및 설계"),
      chatBubble("user", "Figma 함께 만드는 지도서를 참조하여\nPage1: 문제 내용 화면 (교사/학생 내용 작게 표시)\nPage2: 해결포인트 작성 화면 (태블릿 펜 입력)\nPage3: 교사가 문제 이미지 업로드하는 화면\n→ 안드로이드 앱을 만들어줘"),
      new Paragraph({ spacing: { before: 80, after: 80 }, children: [] }),
      chatBubble("claude", "Figma 파일 구조 분석 후 전체 Android 앱 생성 완료!\n\n생성 파일:\n• LoginActivity, Page1/2/3Activity\n• DrawingView (태블릿 펜 압력 감지)\n• Firebase Repository\n• 모든 Layout XML 파일"),

      new Paragraph({ spacing: { before: 160, after: 80 }, children: [] }),
      h2("Step 3 — Firebase 설정"),
      chatBubble("user", "이 프로젝트에 Firestore를 사용 설정할 수 없습니다. 알 수 없는 오류가 발생했습니다."),
      new Paragraph({ spacing: { before: 80, after: 80 }, children: [] }),
      chatBubble("claude", "화면 확인 결과: 3단계 구성에서 오류 발생\n→ 해결 방법 제시: \"만들기\" 버튼 재시도\n→ 성공! Firestore 데이터베이스 생성 완료"),

      new Paragraph({ spacing: { before: 160, after: 80 }, children: [] }),
      h2("Step 4 — Firebase Storage 문제 & Cloudinary 대체"),
      chatBubble("user", "유료결재를 안해서 Storage 시작하기가 안되는데.. 다른 방법은?"),
      new Paragraph({ spacing: { before: 80, after: 80 }, children: [] }),
      chatBubble("claude", "Firebase Storage → 최근 유료 전용으로 변경됨\n\n대안: Cloudinary (무료 25GB)\n• 가입 후 Cloud Name / API Key / Secret 발급\n• CloudinaryHelper.kt 생성\n• GuidebookRepository.kt를 Cloudinary 버전으로 교체"),

      new Paragraph({ spacing: { before: 160, after: 80 }, children: [] }),
      h2("Step 5 — Authentication & 사용자 등록"),
      chatBubble("user", "사용자 추가까지 했어"),
      new Paragraph({ spacing: { before: 80, after: 80 }, children: [] }),
      chatBubble("claude", "Firebase Authentication 확인:\n• teacher@school.com (role: teacher)\n• student1@school.com (role: student)\n• student2@school.com (role: student)\n\nFirestore users 컬렉션에 uid/name/email/role 문서 생성 완료"),

      new Paragraph({ spacing: { before: 160, after: 80 }, children: [] }),
      h2("Step 6 — google-services.json & Android Studio"),
      chatBubble("user", "json파일 다운로드하자!"),
      new Paragraph({ spacing: { before: 80, after: 80 }, children: [] }),
      chatBubble("claude", "Firebase 프로젝트 설정에서 google-services.json 다운로드\n→ GuidebookApp/app/ 폴더에 복사 완료\n\nAndroid Studio 설치 여부 확인 → 미설치\n→ developer.android.com/studio 에서 Panda 4 다운로드 중"),

      // ── 3. 생성된 파일 구조 ──────────────────────────────────────
      new Paragraph({ children: [new PageBreak()] }),
      h1("3. 생성된 파일 구조"),
      code("GuidebookApp/"),
      code("├── build.gradle"),
      code("├── settings.gradle"),
      code("├── README_SETUP.md"),
      code("├── 세션정리.md"),
      code("└── app/"),
      code("    ├── build.gradle          ← Firebase, Cloudinary, Glide 의존성"),
      code("    ├── google-services.json  ← Firebase 연동 (다운로드 완료)"),
      code("    └── src/main/"),
      code("        ├── AndroidManifest.xml"),
      code("        ├── java/com/example/guidebook/"),
      code("        │   ├── activities/"),
      code("        │   │   ├── LoginActivity.kt"),
      code("        │   │   ├── Page1Activity.kt"),
      code("        │   │   ├── Page2Activity.kt"),
      code("        │   │   └── Page3Activity.kt"),
      code("        │   ├── views/DrawingView.kt   ← 태블릿 펜 (압력감지)"),
      code("        │   ├── models/ (AppUser, Problem, UserNote)"),
      code("        │   ├── viewmodels/ (Page1~3ViewModel)"),
      code("        │   ├── adapters/ (UserPanelAdapter)"),
      code("        │   └── repository/"),
      code("        │       ├── GuidebookRepository.kt"),
      code("        │       └── CloudinaryHelper.kt"),
      code("        └── res/layout/ (5개 화면 레이아웃)"),

      // ── 4. 기술 스택 ─────────────────────────────────────────────
      new Paragraph({ children: [new PageBreak()] }),
      h1("4. 기술 스택"),
      new Table({
        width: { size: 9360, type: WidthType.DXA },
        columnWidths: [2500, 3000, 3860],
        rows: [
          new TableRow({ children: [
            new TableCell({ borders, width: { size: 2500, type: WidthType.DXA }, shading: { fill: "2E75B6", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "항목", bold: true, color: "FFFFFF", size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3000, type: WidthType.DXA }, shading: { fill: "2E75B6", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "선택", bold: true, color: "FFFFFF", size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3860, type: WidthType.DXA }, shading: { fill: "2E75B6", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "비고", bold: true, color: "FFFFFF", size: 20 })] })] }),
          ]}),
          ...[
            ["언어", "Kotlin", "Android 표준"],
            ["아키텍처", "MVVM", "ViewModel + LiveData"],
            ["데이터베이스", "Firebase Firestore", "실시간 동기화"],
            ["인증", "Firebase Auth", "이메일/비밀번호"],
            ["이미지 저장", "Cloudinary (무료)", "Firebase Storage 유료 전환으로 대체"],
            ["이미지 로딩", "Glide 4.16", "빠른 캐싱"],
            ["비동기", "Kotlin Coroutines", "viewModelScope"],
          ].map(([item, sel, note], i) => new TableRow({ children: [
            new TableCell({ borders, width: { size: 2500, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "EEF4FB" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: item, bold: true, size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3000, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "EEF4FB" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: sel, size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3860, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "EEF4FB" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: note, size: 20 })] })] }),
          ]}))
        ]
      }),

      // ── 5. 트러블슈팅 ────────────────────────────────────────────
      new Paragraph({ children: [new PageBreak()] }),
      h1("5. 트러블슈팅 기록"),
      new Table({
        width: { size: 9360, type: WidthType.DXA },
        columnWidths: [3000, 2500, 3860],
        rows: [
          new TableRow({ children: [
            new TableCell({ borders, width: { size: 3000, type: WidthType.DXA }, shading: { fill: "C0392B", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "문제", bold: true, color: "FFFFFF", size: 20 })] })] }),
            new TableCell({ borders, width: { size: 2500, type: WidthType.DXA }, shading: { fill: "C0392B", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "원인", bold: true, color: "FFFFFF", size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3860, type: WidthType.DXA }, shading: { fill: "C0392B", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: "해결", bold: true, color: "FFFFFF", size: 20 })] })] }),
          ]}),
          ...[
            ["Firestore 생성 오류", "Google 일시적 서버 오류", "만들기 버튼 재시도 → 성공"],
            ["Firebase Storage 사용 불가", "유료 플랜(Blaze) 전용으로 변경됨", "Cloudinary 무료 플랜으로 대체"],
            ["Android Studio 다운로드 실패", "Google CDN 직접 다운로드 차단", "공식 사이트에서 브라우저로 직접 다운로드"],
          ].map(([prob, cause, sol], i) => new TableRow({ children: [
            new TableCell({ borders, width: { size: 3000, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "FDEDEC" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: prob, size: 20 })] })] }),
            new TableCell({ borders, width: { size: 2500, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "FDEDEC" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: cause, size: 20 })] })] }),
            new TableCell({ borders, width: { size: 3860, type: WidthType.DXA }, shading: { fill: i % 2 === 0 ? "FDEDEC" : "FFFFFF", type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              children: [new Paragraph({ children: [new TextRun({ text: sol, size: 20 })] })] }),
          ]}))
        ]
      }),

      // ── 6. 현재 상태 & 다음 단계 ────────────────────────────────
      new Paragraph({ children: [new PageBreak()] }),
      h1("6. 현재 상태 및 다음 단계"),
      h2("완료 항목 ✅"),
      bullet("Android 앱 전체 코드 (Kotlin, MVVM)"),
      bullet("Firebase Firestore 설정 완료"),
      bullet("Firebase Authentication + 사용자 3명 등록"),
      bullet("Cloudinary 이미지 스토리지 연동"),
      bullet("google-services.json 앱 폴더 복사 완료"),
      new Paragraph({ spacing: { before: 120, after: 80 }, children: [] }),

      h2("남은 작업 ⏳"),
      bullet("Android Studio Panda 4 설치 (다운로드 중)"),
      bullet("Android Studio에서 GuidebookApp 열기"),
      bullet("Gradle Sync 후 빌드"),
      bullet("태블릿 USB 연결 → 실기기 테스트"),
      new Paragraph({ spacing: { before: 120, after: 80 }, children: [] }),

      h2("Android Studio 설치 후 실행 순서"),
      bullet("Android Studio 실행"),
      bullet("Open → C:\\Users\\User\\Documents\\claude\\GuidebookApp 선택"),
      bullet("상단 Sync Now 클릭 (Gradle 동기화, 수분 소요)"),
      bullet("태블릿 USB 연결 → 개발자 옵션 > USB 디버깅 ON"),
      bullet("상단 ▶ 실행 버튼 → 태블릿 선택 → 앱 설치 및 실행"),

      // ── 7. 현재 화면 스크린샷 ────────────────────────────────────
      new Paragraph({ children: [new PageBreak()] }),
      h1("7. 세션 종료 시점 화면"),
      p("아래는 세션 종료 시점의 화면입니다. Android Studio Panda 4 다운로드 페이지와 GuidebookApp 프로젝트 파일 구조가 보입니다."),
      new Paragraph({ spacing: { before: 120, after: 120 }, children: [] }),
      imgParagraph(screenshot, 580, 360),

      new Paragraph({ spacing: { before: 120, after: 120 }, children: [] }),
      divider(),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 120, after: 0 },
        children: [new TextRun({ text: "— 세션 기록 끝 —", size: 20, color: "888888", italics: true })]
      }),
    ]
  }]
});

Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync('C:/Users/User/Documents/claude/GuidebookApp/세션기록.docx', buffer);
  console.log('✅ DOCX 생성 완료!');
}).catch(e => { console.error('❌ 오류:', e.message); });
