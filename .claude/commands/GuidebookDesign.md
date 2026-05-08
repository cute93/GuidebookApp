---
name: GuidebookDesign
description: GuidebookApp 디자인 시스템으로 HTML 페이지를 생성합니다. Apple 스타일 다크 테마, 연두색 액센트(#BDFF43), blob 배경 효과를 적용합니다.
user-invocable: true
allowed-tools:
  - Read
  - Write
  - Glob
---

# /GuidebookDesign — GuidebookApp 디자인 시스템 HTML 생성

인수: `$ARGUMENTS`

`$ARGUMENTS`에 담긴 요청을 바탕으로 GuidebookApp 디자인 시스템을 따르는 HTML 파일을 생성합니다.

---

## 인수 파싱

`$ARGUMENTS` 형식:
```
/GuidebookDesign <파일명> [--title <제목>] [--type <타입>] [--link <연결파일>]
```

- `<파일명>` — 생성할 HTML 파일명 (예: `page.html`). 생략 시 `guidebook_page.html`
- `--title <제목>` — 페이지 제목. 생략 시 파일명 기반으로 자동 결정
- `--type` — `overview`(기본) | `detail` | `dashboard` | `report`
- `--link <연결파일>` — 다른 페이지로의 링크 (nav CTA 버튼에 사용)

`$ARGUMENTS`가 비어있으면 사용법을 출력하고 종료합니다.

---

## 디자인 시스템 규칙

아래 규칙을 **반드시** 준수하여 HTML을 생성합니다.

### CSS 변수 (모든 파일 공통 적용)
```css
:root {
  --bg: #000;
  --surface: rgba(255,255,255,0.04);
  --surface-hover: rgba(255,255,255,0.07);
  --border: rgba(255,255,255,0.08);
  --text: #f5f5f7;
  --text-secondary: rgba(245,245,247,0.55);
  --accent: #BDFF43;
  --accent-dim: rgba(189,255,67,0.12);
  --indigo: #818cf8;
  --indigo-dim: rgba(129,140,248,0.12);
  --radius: 20px;
  --font: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI", sans-serif;
  --mono: "SF Mono", "Fira Code", "Cascadia Code", monospace;
}
```

### 배경 효과 (필수 포함)
다음 레이어를 `z-index: 0`, `pointer-events: none`으로 고정 배치합니다:

1. **Animated blobs** — 3–4개의 `.blob` 요소
   - `filter: blur(120px)`, `opacity: 0.18–0.22`
   - `animation: drift 18–20s ease-in-out infinite alternate`
   - 기본 색상: `#3F51B5`(인디고), `#BDFF43`(연두), `#a855f7`(보라), `#06b6d4`(시안)
   - 각 blob을 화면 모서리/중앙에 분산 배치

2. **Grid lines** — CSS `background-image` 격자
   - 56–60px 간격, `rgba(255,255,255,0.02–0.025)` 투명도

3. **Floating particles** — JS로 20–30개 동적 생성
   ```js
   // particle: position absolute, 2px 원, animation float-up
   // left: random vw, duration: 12–30s, drift: -50~50px
   ```

### 내비게이션
```css
nav {
  position: sticky; top: 0; z-index: 100;
  backdrop-filter: blur(24px) saturate(180%);
  border-bottom: 1px solid var(--border);
  background: rgba(0,0,0,0.65);
  height: 56px;
}
```
- 좌: 로고 (`Guidebook<span style="color:var(--accent)">App</span>`)
- 우: CTA 버튼 (`background: var(--accent); color: #000; border-radius: 20px`)

### 카드 컴포넌트
```css
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: background 0.2s, border-color 0.2s;
}
.card:hover {
  background: var(--surface-hover);
  border-color: rgba(255,255,255,0.12);
}
```

### 버튼 스타일
- Primary: `background: var(--accent); color: #000; border-radius: 30px; padding: 12px 28px`
- Ghost: `border: 1px solid var(--border); color: var(--text); border-radius: 30px`
- 위험/삭제: `background: rgba(239,68,68,0.1); color: #f87171`

### 상태 배지
- 완료: `background: rgba(74,222,128,0.1); color: #4ade80`
- 대기: `background: rgba(251,191,36,0.1); color: #fbbf24`
- 미완: `background: rgba(255,255,255,0.07); color: var(--text-secondary)`

### 코드 블록
macOS 창 스타일로 감쌉니다:
```html
<div class="code-wrap">
  <div class="code-top">
    <div class="code-dots">
      <div class="code-dot" style="background:#ff5f57"></div>
      <div class="code-dot" style="background:#febc2e"></div>
      <div class="code-dot" style="background:#28c840"></div>
    </div>
    <span class="code-lang">언어명</span>
  </div>
  <pre>코드 내용</pre>
</div>
```

### 타이포그래피
- 헤딩 h1: `font-size: clamp(40px,6vw,72px); font-weight:700; letter-spacing:-2px`
  - 그라디언트 텍스트: `background: linear-gradient(135deg,#fff,rgba(255,255,255,0.6)); -webkit-background-clip:text`
- 헤딩 h2: `font-size: 22px; font-weight: 700; letter-spacing: -0.5px`
- 본문: `font-size: 14px; color: var(--text-secondary); line-height: 1.6`
- 섹션 레이블: `font-size: 12px; font-weight:500; letter-spacing:1px; text-transform:uppercase; color:var(--text-secondary)`

### 레이아웃 최대 너비
- overview: `max-width: 1100px`
- detail (사이드바 포함): `grid-template-columns: 220px 1fr`

---

## 타입별 생성 구조

### type=overview (기본)
```
nav
wrap (max-width 1100px)
  ├─ .hero (eyebrow badge + h1 + subtitle + meta chips)
  ├─ .status-bar (현재 상태 + 진행률 바)
  ├─ section (카드 그리드 — CSS grid 3열)
  ├─ section (타임라인 또는 목록)
  └─ .cta-row (btn-primary + btn-ghost)
footer
```

### type=detail
```
nav (← 뒤로가기 + step dots)
.layout (grid: 220px sidebar + 1fr main)
  ├─ aside (sticky, IntersectionObserver 연동)
  └─ main
      ├─ 섹션별 .step-section (id 기반 앵커)
      └─ 각 섹션: step-badge + card + code-wrap
footer
```

### type=dashboard
```
nav
wrap
  ├─ 상단 KPI 카드 행 (4개, CSS grid)
  ├─ 중단 큰 카드 (주요 정보)
  └─ 하단 2열 (부가 정보)
footer
```

### type=report
```
nav
wrap
  ├─ .hero (제목 + 날짜 + 요약)
  ├─ 섹션 목록 (카드 스택)
  └─ .cta-row
footer
```

---

## 실행 절차

1. `$ARGUMENTS`를 파싱하여 파일명, 제목, 타입, 링크를 결정합니다.
2. 콘텐츠 내용은 `$ARGUMENTS`의 나머지 텍스트 또는 파일명에서 유추합니다.
   - 내용이 명확하지 않으면 현재 프로젝트 컨텍스트(GuidebookApp Figma 반영 작업)를 기반으로 채웁니다.
3. 위 디자인 시스템 규칙을 모두 적용하여 완성된 HTML을 작성합니다.
4. 프로젝트 루트(`C:\Users\FairyTail\StudioProjects\GuidebookApp\`)에 파일을 저장합니다.
5. 생성된 파일 경로와 주요 섹션 목록을 사용자에게 보고합니다.

---

## 사용 예시

```
/GuidebookDesign verify.html --title "검증 체크리스트" --type report
/GuidebookDesign step2.html --title "Page1 레이아웃 재설계" --type detail --link history1.html
/GuidebookDesign dashboard.html --title "빌드 현황 대시보드" --type dashboard
```
