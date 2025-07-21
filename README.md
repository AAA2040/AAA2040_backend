YouTube Audio & Lyrics Separation 

> YouTube URL 하나로 MR(무반주)·보컬 트랙 분리와 타임싱크 자막(VTT)을 제공하는 풀스택 싱글 페이지 애플리케이션

---

## 🚀 프로젝트 개요

이번 프로젝트는 사용자가 YouTube 영상 URL만 입력하면, 백엔드가 자동으로 MR(무반주)과 보컬 트랙을 분리하고, 동시에 타임싱크된 가사를 제공하는 풀스택 애플리케이션을 목표로 합니다. 프론트엔드는 Next.js로 개발해 정적 파일로 export(`npm run build`)한 뒤, Spring Boot의 `src/main/resources/static` 디렉터리에 포함시켜 배포합니다. 이 덕분에 별도의 CDN이나 정적 서버 없이도 하나의 JAR 파일만으로 완전한 SPA(싱글 페이지 애플리케이션)를 서빙할 수 있습니다.

백엔드 핵심 로직은 `AudioProcessingService`라는 단일 서비스 클래스로 집중되었습니다. 이 서비스는 전달받은 YouTube URL의 유효성을 즉시 검증하고, `RestTemplate`을 통해 외부 음원 분리 API에 POST 요청을 보냅니다. 외부 서비스는 JSON 형태의 `CallbackResponse`를 반환하며, 이 응답에는 태스크 식별자(`uriId`), MR URL, 보컬 URL, 그리고 원본 가사(`lyrics`)가 담겨 있습니다. 서비스는 이 값을 `ProcessingStatus`라는 DTO로 매핑해, 클라이언트가 필요로 하는 핵심 정보만 깔끔하게 묶어 반환합니다.

컨트롤러에서는 복잡한 매핑 로직을 모두 생략하고, `ProcessingStatus` 객체를 그대로 JSON으로 반환하도록 구현했습니다. 이를 통해 매핑 코드가 대폭 간소화되었고, 잘못된 키 이름이나 누락 위험이 사라졌습니다. 또한, DTO 필드 중 실제로 사용하지 않는 `progress`, `error`, `videoTitle`, `subtitlePath`는 모두 제거해, 응답 페이로드를 최소화하고 코드의 가독성을 높였습니다.

SPA 라우팅은 Spring MVC 설정 한 줄로 해결했습니다. 모든 “점(dot)”이 포함되지 않은 경로 요청을 `index.html`로 포워딩하도록 `@RequestMapping({"/", "/{path:[^.]+}", "/**/{path:[^.]+}"})` 어노테이션을 사용했으며, `/api/**` 경로만 예외로 처리해 API 호출과 클라이언트 라우팅이 충돌 없이 공존합니다.

---

## 🧩 전체 아키텍처

```
[클라이언트(UI)]
   │ 1. YouTube URL 입력
   ▼
[AudioController (Spring Boot)]
   │ 2. AudioProcessingService 호출
   ▼
[AudioProcessingService]
   │ 3. RestTemplate → 외부 API 요청
   │ 4. CallbackResponse → ProcessingStatus DTO 매핑
   ▼
[AudioController]
   │ 5. JSON 응답: taskId, no_vocals_url, vocals_url, lyrics
   ▼
[Next.js SPA]
   │ 6. MR·보컬 재생기 + 동기화 자막 렌더링
```

---

## 📂 디렉터리 구조

```
/
├─ frontend/                          # Next.js 프로젝트
│   ├─ pages/
│   └─ public/
├─ src/
│   ├─ main/
│   │   ├─ java/com/example/miniproj/
│   │   │   ├─ controller/            # AudioController.java
│   │   │   ├─ service/               # AudioProcessingService.java
│   │   │   ├─ model/                 # ProcessRequest, CallbackResponse, ProcessingStatus
│   │   │   └─ config/                # RestTemplateConfig.java
│   │   └─ resources/
│   │       ├─ static/                # Next.js 빌드 결과물
│   │       └─ application.yml
├─ data/
│   └─ subtitles/                     # 생성된 .vtt 자막 파일
├─ Dockerfile
├─ pom.xml / build.gradle
└─ README.md
```

---



## 🌐 API 명세

| Method | Path                              | 설명                             |
| ------ | --------------------------------- | ------------------------------ |
| POST   | `/api/process`                    | YouTube URL 수신 → 음원 분리 요청 → 응답 |
| GET    | `/api/status/{taskId}`            | 처리 상태 조회                       |
| GET    | `/api/result/{taskId}/lyrics`     | 원본 가사(raw lyrics) 반환           |
| GET    | `/api/download/{taskId}/subtitle` | 타임싱크 자막(VTT) 다운로드              |

### 예시: `POST /api/process`

**Request**

```json
{ "url": "https://www.youtube.com/watch?v=ABC123xyz" }
```

**Response**

```json
{
  "taskId": "ABC123xyz",
  "mrPath": "https://external-service.com/no_vocals/ABC123xyz.mp3",
  "vocalsPath": "https://external-service.com/vocals/ABC123xyz.mp3",
  "lyrics": "[0.00 ~ 10.00] 가사 예시..."
}
```

---

## ⚙️ 설정

```yaml
# src/main/resources/application.yml
external:
  service:
    url: https://api.example.com/process
server:
  port: 9003
```

* `external.service.url`: 외부 음원 분리 API 엔드포인트
* `server.port`: 애플리케이션 실행 포트

---

## 🚀 향후 개선 및 확장

* VTT 다운로드 전용 엔드포인트 강화
* 진행률(progress)·에러(error) 상태 조회 API 추가
* Spring WebFlux 기반 논블로킹 전환
* Hybrid 처리 옵션: 내부 모델(Local)과 외부 API 동시 또는 선택 처리
* CI/CD 자동화: 프론트엔드 빌드 → 백엔드 패키징 → Docker 배포 파이프라인

---

