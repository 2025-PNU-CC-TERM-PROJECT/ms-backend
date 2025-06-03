# ms-backend
Spring Boot 기반의 백엔드 서버로, AI 모델 서빙 플랫폼의 인증 및 대시보드 기능을 담당
FastAPI 기반의 AI 서비스들과 연동되며, 사용자 인증, 활동 이력 조회 등 주요 기능을 제공

## 프로젝트 개요

- **백엔드 프레임워크:** Spring Boot 3.4.5
- **데이터베이스:** PostgreSQL
- **보안:** Spring Security + JWT
- **AI 연동:** KServe 기반 InferenceServer (이미지 분류 / 텍스트 요약)
- **배포:** Docker / Kubernetes 

## 주요 기능

| 기능            | 설명                                      |
| ------------- | --------------------------------------- |
| 회원가입 / 로그인 | JWT 기반 사용자 인증 및 토큰 발급                  |
| 마이페이지 조회   | 로그인한 사용자 정보 반환 (`/user`)                |
| 활동 이력 조회   | 사용자별 모델 사용 이력 반환 (`/dashboard/history`) |
| AI 모델 서빙   | KServe 기반 InferenceServer와 연동하여 예측 결과 수신 |

###  사용자 인증

- `POST /api/auth/login`: 이메일+비밀번호 로그인 → JWT 토큰 발급
- `POST /api/auth/register`: 회원가입

###  마이페이지

- `GET /user`: 현재 로그인한 사용자 정보 반환  
  ※ `SecurityContextHolder`에서 인증 정보 추출

###  대시보드

- `GET /dashboard/history`: 사용자가 요청한 AI 모델 이력 반환  
  → AI 예측 결과(JSON 포함) 및 입력 파일명, 예측 시간 정보 포함

###  AI 서비스 연동

- `POST /api/image-class`: 이미지 분류 요청 (KServe InferenceServer에 `POST`)
- `POST /api/text-summary`: 텍스트 요약 요청 (KServe InferenceServer에 `POST`)
- 결과를 받아 JSON 응답으로 가공 후 프론트로 반환
- 결과는 DB에 사용 이력으로 저장됨

## API 명세

/api/auth
POST /login: 이메일, 비밀번호로 로그인

POST /signup: 회원가입

/user
GET /user: 로그인한 사용자 정보 조회
GET /usage-history: 활동 이력 리스트 반환
GET /usage-history/iamge-meta/{id}: 이미지의 활동 이력 상세 내역 반환
GET /usage-history/iamge/{id}: 이미지 사진 반환


/dashboard
GET /usage-stats: 활동 횟수 반환 

POST /iamge-class: KServe Inference - Image 추론 결과값 반환
POST /text-summary: KServe Inference - Text Summary 추론 결과값 반환


## 연동되는 서비스

PostgreSQL DB

React + Next.js 프론트엔드

KServe 기반 InferenceServer

## 인증 구조

- JWT 토큰 발급 → 모든 요청에 헤더로 Authorization: Bearer <token> 전송
- 사용자 인증 정보는 UserDetailsImpl로 관리되며, SecurityContextHolder에서 조회

# 전체 실행 과정 

## 주요 화면 예시

### 로그인 & 회원가입

<p align="center">
  <img width="300" alt="login" src="https://github.com/user-attachments/assets/8eee6b5e-35e3-4fd8-accc-86ef4b9205f8" />  
  <img width="300" alt="sign_up" src="https://github.com/user-attachments/assets/13492a1d-1c70-4825-868d-c3dbbfdba108" />
</p>

---

### 대시 보드 

<p align="center">
  <img width="1104" alt="dashboard" src="https://github.com/user-attachments/assets/8b369db3-5228-4a7e-9669-295e9a37073d" />
</p>

### 이미지 분류 요청

<p align="center">
  <img width="300" alt="image_class_1" src="https://github.com/user-attachments/assets/b77a7350-0ed3-4c46-a486-5a79b3cbd845" />
  <img width="300" alt="image_class_2" src="https://github.com/user-attachments/assets/984b110d-68c9-4b33-85dc-de44cae70111" />
</p>

---

### 텍스트 요약 요청

<p align="center">
  <img width="300" alt="text_summarize_1" src="https://github.com/user-attachments/assets/f7e4f139-c3c2-47af-b57f-388897311212" />
  <img width="300" alt="text_summarize_2" src="https://github.com/user-attachments/assets/ada8346e-54a8-49ef-814c-a757b3bd3eae" />
</p>

---

### 사용자 활동 이력


<p align="center">
<img width="300" alt="user_history" src="https://github.com/user-attachments/assets/0a30292b-1281-4e3a-88fe-f3fbbb9910a9" />
<img width="300" alt="user_history_img" src="https://github.com/user-attachments/assets/1f27490b-adc1-48a3-9f6d-0eb7707ed256" />
<img width="300" alt="user_history_text" src="https://github.com/user-attachments/assets/3da0186e-c746-42b3-b296-867f4bfd0766" />
</p>

---

