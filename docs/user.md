# User 도메인

## API 목록

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | /api/v1/users/me | 내 프로필 조회 | 필요 |
| GET | /api/v1/users/{id} | 타인 프로필 조회 | 필요 |
| PUT | /api/v1/users/me | 닉네임 수정 | 필요 |
| GET | /api/v1/users/search?keyword= | 유저 검색 | 필요 |
| DELETE | /api/v1/users/me | 회원탈퇴 | 필요 |

## 시퀀스 다이어그램

### 내 프로필 조회
```mermaid
sequenceDiagram
    actor Client
    participant Controller
    participant UserService
    participant UserRepository

    Client->>Controller: GET /api/v1/users/me
    Controller->>UserService: getMyProfile(userId)
    UserService->>UserRepository: findById(userId)
    UserRepository-->>UserService: User
    UserService-->>Controller: UserProfileResponse
    Controller-->>Client: 200 OK