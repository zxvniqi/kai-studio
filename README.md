# Kai Studio

NovelAI 이미지 생성용 웹 UI입니다. V5까지 지원하고, 폰에서도 쓰기 편하게 만들었습니다.

**바로 쓰기** → https://zxvniqi.github.io/kai-studio/

**안드로이드 앱** → [kai-studio.apk 받기](https://github.com/zxvniqi/kai-studio/releases/download/app/kai-studio.apk)

## 주요 기능

- 프롬프트 · 캐릭터 프롬프트 · UC, 가중치 색 표시, 태그 자동완성
- 레퍼런스 이미지(i2i · 인페인트 · 바이브 · 캐릭터 레퍼런스)
- 단부루 태그 검색 · 북마크, 프롬프트 청크
- 이미지 메타데이터에서 설정 불러오기, 기록 ZIP 저장, 백업/복원

## 안내

- NovelAI 토큰은 내 브라우저(기기)에만 저장되며 다른 곳으로 보내지 않습니다.
- 앱은 위 사이트를 그대로 불러오는 방식이라, 사이트가 업데이트되면 앱도 자동으로 반영됩니다.

## 구성

| 경로 | 내용 |
|---|---|
| `index.html` | 사이트 전체 (단일 파일) |
| `manifest.json`, `sw.js`, `icon-*.png` | 홈 화면 추가(PWA)용 |
| `android/` | 안드로이드 앱 (WebView) |
| `.github/workflows/apk.yml` | `android/` 변경 시 APK 빌드 후 릴리스에 올림 |
