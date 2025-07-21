 MiniProj: YouTube Audio & Lyrics Separation SPA

> YouTube URL 하나로 MR(무반주)·보컬 트랙 분리와 타임싱크 자막(VTT)을 제공하는 풀스택 싱글페이지 애플리케이션

---

## 🚀 프로젝트 개요

MiniProj는 사용자가 YouTube 영상 URL을 입력하면, 백엔드가 해당 URL을 외부 음원 분리 & 가사 추출 API로 전달하고, 반환된 보컬·반주 트랙 URL과 원본 가사를 JSON으로 응답하며, 동시에 타임싱크 자막(.vtt) 파일을 생성하여 프론트엔드에 서빙하는 애플리케이션입니다.

- **프론트엔드**: Next.js → `npm run build` → Spring Boot `src/main/resources/static`  
- **백엔드**: Spring Boot + RestTemplate  
- **배포**: `mvn package`/`gradle bootJar` → 실행용 JAR → Docker 이미지  

---

## 🧩 전체 아키텍처

```text
[클라이언트(UI)]
   │ 1. YouTube URL 입력
   ▼
[AudioController (Spring Boot)]
   │ 2. AudioProcessingService 호출
   ▼
[AudioProcessingService]
   │ 3. RestTemplate → 외부 음원 분리 API 요청
   │ 4. CallbackResponse → ProcessingStatus DTO 매핑
   ▼
[AudioController]
   │ 5. JSON 응답: taskId, no_vocals_url, vocals_url, lyrics
   ▼
[Next.js SPA]
   │ 6. MR·보컬 재생기 + 동기화 자막 렌더링
📂 디렉터리 구조
csharp
복사
편집
/
├─ frontend/                          # Next.js 프로젝트
│   ├─ pages/…
│   └─ public/…
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
🔧 빠른 시작
1. 프론트엔드 빌드
bash
복사
편집
cd frontend
npm install
npm run build
# → 빌드 결과물이 ../src/main/resources/static/ 아래로 복사되어야 합니다.
2. 백엔드 빌드 & 실행
bash
복사
편집
# 프로젝트 루트
mvn clean package
# 또는
gradle clean bootJar

# 생성된 JAR 실행
java -jar target/miniproj.jar
3. (옵션) Docker로 실행
bash
복사
편집
docker build -t miniproj .
docker run -p 9003:9003 miniproj
🌐 API 명세
Method	Path	설명
POST	/api/process	YouTube URL 수신 → 음원 분리 요청 → 응답
GET	/api/status/{taskId}	처리 상태 조회
GET	/api/result/{taskId}/lyrics	원본 가사(raw lyrics) 반환
GET	/api/download/{taskId}/subtitle	타임싱크 자막(VTT) 다운로드

예시: POST /api/process
Request

json
복사
편집
{ "url": "https://www.youtube.com/watch?v=ABC123xyz" }
Response

json
복사
편집
{
  "taskId": "ABC123xyz",
  "mrPath": "https://external-service.com/no_vocals/ABC123xyz.mp3",
  "vocalsPath": "https://external-service.com/vocals/ABC123xyz.mp3",
  "lyrics": "[0.00 ~ 10.00] 가사 예시..."
}
⚙️ 설정
yaml
복사
편집
# src/main/resources/application.yml
external:
  service:
    url: https://api.example.com/process
server:
  port: 9003
external.service.url: 외부 음원 분리 API 엔드포인트

server.port: 애플리케이션 실행 포트

🚀 주요 개선 및 확장 포인트
VTT 다운로드 전용 엔드포인트 강화

진행률(progress)·에러(error) 상태 조회 API 추가

Spring WebFlux 기반 논블로킹 전환

Hybrid 처리 옵션: 내부 모델(Local)과 외부 API 동시 또는 선택 처리

CI/CD 자동화: 프론트엔드 빌드 → 백엔드 패키징 → Docker 배포 파이프라인

🤝 기여 방법
이 저장소를 Fork

새 브랜치 생성:

bash
복사
편집
git checkout -b feature/your-feature
변경사항 커밋:

bash
복사
편집
git commit -m "feat: 설명"
원본 저장소에 Pull Request 생성

