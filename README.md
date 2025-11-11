## 📋 목차
- [프로젝트 소개](#-프로젝트-소개)
- [사용 기술](#️-사용-기술)
- [빌드 및 실행](#-빌드-및-실행)
- [폴더 구조](#-폴더-구조)

---

# 프로젝트 소개

<img width="1920" height="1080" alt="프로젝트 대표 이미지" src="https://github.com/user-attachments/assets/bd6f809d-c0c4-4623-a4f2-62cc8eeaca2d" />

- 신한금융희망재단의 **공간 예약 서비스 백엔드 API 코드**입니다.  
- Spring Boot 기반 REST API 서버로, 사용자(`user`)와 관리자(`admin`) 기능을 제공합니다.  
- 각 도메인별로 **Controller**와 **Service** 계층이 구성되어 있습니다.

---

# 사용 기술

| 구분 | 기술 |
| ---- | ---- |
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5.4 |
| **Build Tool** | Gradle |
| **Database** | PostgreSQL |
| **ORM** | Spring Data JPA, QueryDSL 5 (jakarta) |
| **Cache / Session** | Redis |
| **Security** | Spring Security, JWT |
| **Cloud** | AWS S3 |
| **API Docs** | springdoc-openapi (Swagger UI) |
| **Mail** | Spring Mail |
| **Dev Tool** | Lombok, Spring Boot DevTools |
---

# 빌드 및 실행

- Gradle을 통해 프로젝트를 빌드하고 실행할 수 있습니다.  
- 실행 전, `application.properties` 파일에 데이터베이스 및 환경 설정을 입력하세요. 환경 설정 파일은 별도로 제공된 개발 문서를 참고해주세요.

### 빌드

```
./gradlew clean build
```

### 개발 서버 실행

```
./gradlew bootRun
```

---

# 폴더 구조

```
Team-Mute-back-end
├── build.gradle
├── src
│   ├── main
│   │   ├── java/Team_Mute/back_end
│   │   │   ├── global              # 전역 설정(Security, Exception, Util 등)
│   │   │   └── domain              # 도메인별 패키지
│   │   │       ├── member            # 사용자 및 관리자 계정 관리 도메인
│   │   │       ├── space_user        # 사용자 - 공간 조회 관련 도메인
│   │   │       ├── space_admin       # 관리자 - 공간 관리 도메인
│   │   │       ├── reservation       # 사용자 - 예약 생성 도메인
│   │   │       ├── reservation_admin # 관리자 - 예약 관리 도메인
│   │   │       ├── invitation        # 사용자 - 예약 초대장 도메인
│   │   │       └── dashboard_admin   # 관리자 - 대시보드 도메인
│   │   └── resources
│   │       └── application.properties # 환경 설정
└── README.md

```

### 폴더 컨벤션
- **domain/**
  - 도메인 단위로 패키지를 구성합니다.  
  - 각 도메인 내부에는 `Controller`, `Service`, `Repository`, `Entity`, `Dto` 계층이 포함됩니다.  
  - 기능별로 책임을 명확히 분리하여 유지보수성과 확장성을 높입니다.

- **global/**
  - 전역적으로 사용되는 설정 및 공통 로직을 관리합니다.  
  - `SecurityConfig`, `DataSeedRunner`, `GlobalExceptionHandler`, `SwaggerConfig`, `Enum` 등 모든 도메인에서 참조할 수 있는 코드를 포함합니다.

- **resources/**
  - 애플리케이션 실행에 필요한 설정 파일 및 리소스를 관리합니다.  
  - `application.properties`을 포함합니다.

---
