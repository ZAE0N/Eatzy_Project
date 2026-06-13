# 🍜 Eatzy — 점심 메뉴 룰렛 & 맛집 맵

> 매일 점심 메뉴 고르기가 힘든 2030 세대를 위한 안드로이드 앱.
> 기피 메뉴를 걸러주는 **룰렛**으로 메뉴를 추천하고, 당첨 메뉴를 파는 **주변 식당을 지도**에 보여줍니다.

대학 프로젝트(인하공전)로 개발한 Java 기반 네이티브 안드로이드 앱입니다.

---

## ✨ 주요 기능

- **메뉴 룰렛** — 6칸 룰렛을 돌려 점심 메뉴를 무작위 추천. 사용자가 설정한 기피 메뉴·알레르기 메뉴는 후보에서 제외.
- **맛집 지도** — 당첨 메뉴를 기준으로 현재 위치 주변 식당을 Google Maps 마커로 표시.
- **개인화** — 회원가입/니즈 설정에서 선호·기피·알레르기 정보를 저장하고 룰렛에 반영.
- **커뮤니티** — 게시글/댓글 작성, 좋아요, 이미지·위치 첨부, 일반/베스트 탭.
- **봇 방지** — 로그인·회원가입·글쓰기 시 reCAPTCHA Enterprise 검증(fail-closed).
- **반응형 UI** — 태블릿(sw600dp)·가로 방향·다크 모드 대응, 시스템 네비게이션 바 인셋 처리.

---

## 🛠 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어 / UI | Java 11, XML 레이아웃 |
| 빌드 | Gradle (AGP 9.2.1), Version Catalog (`libs.versions.toml`) |
| SDK | compileSdk 36 / minSdk 35 / targetSdk 36 |
| 데이터베이스 | **Firebase Realtime Database** (외부 서버 없음, `FirebaseHelper` 싱글톤) |
| 지도 / 위치 | Google Maps SDK, FusedLocationProvider, **Places SDK**(실제 식당 검색) |
| 봇 방지 | Google reCAPTCHA Enterprise |
| UI 컴포넌트 | Material Components, ConstraintLayout |

> **참고:** 기획안에는 SQLite로 표기되어 있으나, 실제 구현은 **Firebase Realtime Database**로 확정되었습니다.

---

## 📁 프로젝트 구조

```
Eatzy_Project/
├─ app/
│  ├─ src/main/
│  │  ├─ java/com/inhatc/eatzy_project/   # Activity + POJO + 헬퍼 (28개)
│  │  ├─ res/
│  │  │  ├─ layout/                       # 화면 XML 18개
│  │  │  ├─ values/                       # colors, styles, dimens, strings, arrays, themes
│  │  │  ├─ values-sw600dp / -land / -night
│  │  │  ├─ drawable/ (31)  font/  mipmap-*  xml/
│  │  └─ AndroidManifest.xml
│  ├─ build.gradle
│  └─ google-services.json   # ⚠️ gitignore — 직접 발급 필요
├─ gradle/libs.versions.toml  # 의존성 버전 카탈로그
├─ eatzy_ui_sky.html          # 디자인 시안(10개 화면)
├─ !DOCTYPE.html              # 룰렛 화면 시안
└─ CLAUDE.md                  # 상세 설계/진행 노트
```

### 화면 ↔ Activity ↔ 레이아웃

| Activity | 레이아웃 | 화면 |
|---|---|---|
| `LoginActivity` (런처) | `login.xml` | 로그인 |
| `SignupActivity` | `signup.xml` | 회원가입 |
| `FindPasswordActivity` / `ResetPasswordActivity` | `find_password.xml` / `reset_password.xml` | 비밀번호 찾기·재설정 |
| `MainActivity` | `activity_main.xml` | 메인 홈 |
| `RouletteActivity` | `roulette.xml` | 메뉴 룰렛 |
| `RestaurantMapActivity` | `restaurant_map.xml` | 맛집 지도+리스트 |
| `NeedsSettingsActivity` | `needs_settings.xml` | 니즈(선호/알레르기) 설정 |
| `SidebarActivity` | `sidebar.xml` | 메뉴 사이드바 |
| `CommunityListActivity` / `CommunityDetailActivity` / `CommunityWriteActivity` | `community_*.xml` | 커뮤니티 목록·상세·작성 |
| `LocationPickerActivity` / `PostMapActivity` | `location_picker.xml` / `post_map.xml` | 위치 선택·게시글 지도 |
| `WishlistActivity` | `wishlist.xml` | 위시리스트 |

**POJO/헬퍼:** `User`, `Menu`, `Restaurant`, `Post`, `Comment`, `RouletteItem`, `MapMarker` ·
`FirebaseHelper`, `SessionManager`, `RecaptchaHelper`, `SampleData`, `TimeUtil`, `UiInsets`

---

## 🏗 아키텍처

- **화면 전환:** Activity + Intent 기반. 별도 프레임워크/아키텍처 라이브러리 미사용(순수 안드로이드 네이티브).
- **데이터:** `FirebaseHelper` 싱글톤이 Firebase Realtime DB 읽기/쓰기 담당. POJO 4종(User/Menu/Restaurant/Post)으로 직렬화.
- **세션:** `SessionManager`가 로그인 사용자 정보 보관, 니즈 설정을 Firebase에 저장/복원.
- **룰렛 → 지도 연동:** 룰렛 당첨 메뉴를 Intent extra `"menu"`로 `RestaurantMapActivity`에 전달 → 해당 메뉴 식당 필터링/마커 표시.
- **봇 방지:** `RecaptchaHelper`가 reCAPTCHA Enterprise 토큰 검증을 게이트로 수행(검증 실패 시 동작 차단).
- **반응형:** `UiInsets` 헬퍼로 전 화면 시스템 바 겹침 처리, `dimens` 리소스 한정자로 크기/여백 자동 조정.

---

## ✅ 구현 완료

| 항목 | 비고 | 날짜 |
|---|---|---|
| UI 레이아웃 + Activity + 화면 전환 | 시안 11개 화면 → XML/Java 변환 | 2026-06-02 |
| 룰렛 디자인/로직 | 6칸 라벨 회전(ObjectAnimator), 당첨 계산, 맛집찾기 연동 | 2026-06-02 |
| 메인 재배치 / 불필요 기능 제거 | ☰ 좌상단·로고 중앙, 알림/즐겨찾기/블랙리스트 메뉴 삭제 | 2026-06-02 |
| 반응형 UI / 인셋 처리 | sw600dp·land·night + `UiInsets`, 룰렛판 화면폭 비례 스케일 | — |
| Firebase Realtime DB 연동 | `FirebaseHelper` 싱글톤, POJO 4종, 회원가입 연동 | — |
| Google Maps 실연동 | 실지도 교체, 현재 위치/인하공전 폴백, API 키 인증 | 2026-06-09 |
| 개인화 흐름 | 세션 + 니즈 설정 저장/복원 + 룰렛 선호/알레르기 필터 | — |
| 식당 데이터 연동 | 하드코딩 제거 + Firebase 식당 시딩/필터/마커 | — |
| 커뮤니티 | 게시글/댓글 Firebase 저장, 글쓰기 화면, 일반/베스트 탭, 좋아요 | — |
| reCAPTCHA 봇 방지 | 로그인/회원가입/글쓰기 3곳 게이트(fail-closed) | — |
| Places SDK 코드 | 실제 식당 검색 코드 완료 | — |

> 빌드(`compileDebugJavaWithJavac` / `processDebugResources`)는 BUILD SUCCESSFUL.
> 단, 아래 사유로 **상당수 기능은 에뮬레이터 런타임 미검증** 상태입니다.

---

## 🚧 미구현 / 진행 중

- **설정 화면(기획 1.2)** — 팀원 코드 수령 후 통합 예정. (대기)
- **Places API 런타임 설정** — 코드는 완료, Cloud Console에서 Places API(New) + 결제 + 할당량 캡 설정 필요. 미설정 시 샘플 데이터로 폴백.
- **런타임 검증** — 개발 에뮬레이터의 WiFi 고장으로 인터넷 끊김 → 추천창 ANR 발생. 어댑터 끄기·wipe-data 모두 실패. **실기기 또는 새 AVD에서 검증 권장.**

---

## 🚀 빌드 / 실행

1. **저장소 클론** 후 Android Studio로 열기.
2. **`app/google-services.json` 직접 추가** — Firebase 콘솔에서 패키지 `com.inhatc.eatzy_project`로 발급. (gitignore 처리됨)
3. **API 키 설정** — Google Maps / reCAPTCHA 키가 필요합니다(아래 보안 안내 참고).
4. Gradle Sync 후 `LoginActivity`로 실행.

요구 환경: Android Studio (AGP 9.2.1 호환), JDK 11, 실기기 또는 minSdk 35 이상 AVD.

---

## 🔐 보안 안내

- `google-services.json`, 키스토어, `*.properties` 비밀값은 `.gitignore`로 제외됩니다.
- **Google Maps API 키와 reCAPTCHA 키는 현재 `AndroidManifest.xml`/소스에 하드코딩**되어 있습니다.
  공개 저장소에 노출되므로, 반드시 **API 키 제한(패키지명 + SHA-1 지문, API별 제한)**을 걸어 보호하세요.
  운영 시에는 키를 소스에서 분리(`secrets.properties` 등)하는 것을 권장합니다.

---

## 📄 기타

- 디자인 시안: `eatzy_ui_sky.html`, `!DOCTYPE.html`
- 상세 설계·진행 노트: `CLAUDE.md`
- 패키지: `com.inhatc.eatzy_project` · 런처: `LoginActivity`
