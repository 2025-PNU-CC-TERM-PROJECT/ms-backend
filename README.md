# ms-backend
Spring Boot 기반의 백엔드 서버로, AI 모델 서빙 플랫폼의 인증 및 대시보드 기능을 담당
FastAPI 기반의 AI 서비스들과 연동되며, 사용자 인증, 활동 이력 조회 등 주요 기능을 제공

## 프로젝트 개요

- **백엔드 프레임워크:** Spring Boot 3.4.5
- **데이터베이스:** PostgreSQL
- **보안:** Spring Security + JWT
- **AI 연동:** FastAPI 기반 모델 서버 (이미지 분류 / 텍스트 요약)
- **배포:** Docker / Kubernetes (로컬 개발 시 Kind 사용)

## 주요 기능

| 기능            | 설명                                      |
| ------------- | --------------------------------------- |
| 회원가입 / 로그인 | JWT 기반 사용자 인증 및 토큰 발급                   |
| 마이페이지 조회   | 로그인한 사용자 정보 반환 (`/user`)                |
| 활동 이력 조회   | 사용자별 모델 사용 이력 반환 (`/dashboard/history`) |
| AI 모델 연동   | FastAPI 기반 모델 서버와 연동하여 예측 결과 수신         |

###  사용자 인증

- `POST /api/auth/login`: 이메일+비밀번호 로그인 → JWT 토큰 발급
- `POST /api/auth/register/student`: 학생 회원가입

###  마이페이지

- `GET /user`: 현재 로그인한 사용자 정보 반환  
  ※ `SecurityContextHolder`에서 인증 정보 추출

###  대시보드

- `GET /dashboard/history`: 사용자가 요청한 AI 모델 이력 반환  
  → AI 예측 결과(JSON 포함) 및 입력 파일명, 예측 시간 정보 포함

###  AI 예측 연동 (FastAPI)

- `POST /api/image-class`: 이미지 분류 요청 (FastAPI 서버에 `POST`)
- `POST /api/text-summary`: 텍스트 요약 요청 (FastAPI 서버에 `POST`)
- 결과를 받아 JSON 응답으로 가공 후 프론트로 반환
- 결과는 DB에 사용 이력으로 저장됨


## API 명세

 /api/auth
POST /login: 이메일, 비밀번호로 로그인

POST /register/student: 일반 학생 회원가입

 /user
GET /user: 로그인한 사용자 정보 조회

 /dashboard
GET /dashboard/history: 활동 이력 리스트 반환

## 연동되는 서비스

FastAPI 기반 AI 모델 서빙 (이미지 분류 / 텍스트 요약)

PostgreSQL DB

React + Next.js 프론트엔드

## 인증 구조

- JWT 토큰 발급 → 모든 요청에 헤더로 Authorization: Bearer <token> 전송
- 사용자 인증 정보는 UserDetailsImpl로 관리되며, SecurityContextHolder에서 조회



