# 인증/인가 설계 문서

## 요구사항

### 기능 요구사항
- 카카오 OAuth2 로그인 전용 (자체 회원가입 없음)
- 초대코드가 있어야만 회원가입 가능
- 최초 씨앗 유저는 관리자가 직접 생성
- Access Token(15분) + Refresh Token(14일) 이중 토큰 구조
- 로그아웃 시 토큰 즉시 무효화

### 비즈니스 규칙
- 초대코드는 1회만 사용 가능
- **내 초대코드가 소진되면(이미 누군가 써서 미사용 코드가 없으면) 다음 조회 시점에 새 코드를
  자동 발급한다.** 초대코드는 가입 시 딱 1개만 만들어지고 그 뒤로는 재발급 로직이 없었던 게
  버그였다 — 지인을 한 명 초대하고 나면 "내 초대코드"가 영영 안 뜨는 상태였다. 이제
  `GET /api/v1/users/me`(마이페이지/초대 다이얼로그가 호출)가 미사용 코드를 찾지 못하면 그 자리에서
  새 코드를 만들어 저장하고 내려준다 — 즉, 지인을 몇 명이든 계속 초대할 수 있다.
- Refresh Token은 사용할 때마다 새 토큰으로 교체 (RTR 전략)
- 탈퇴한 유저는 소프트 딜리트 (deleted_at)
- **초대코드로 가입하면 초대한 사람과 자동으로 맞팔로우(상호 follow)된다.** 초대코드를 썼다는 것
  자체가 서로 아는 사이라는 뜻이므로, 가입 직후 촌수 피드가 텅 비지 않도록 가입 트랜잭션 안에서
  `follows` row를 양방향으로 생성한다 ([follow.md](follow.md) 참고). 팔로우 실패(이미 팔로우 중인
  경우 등)는 발생하지 않는다 — 신규 유저이므로 항상 최초 팔로우.

---

## ERD

```mermaid
erDiagram
    USER {
        bigint id PK
        varchar kakao_id UK
        varchar email
        varchar nickname UK
        varchar profile_image_url
        timestamp deleted_at
        timestamp created_at
        timestamp updated_at
    }

    INVITE_CODE {
        bigint id PK
        varchar code UK
        bigint created_by FK
        bigint used_by FK
        timestamp used_at
        timestamp created_at
        timestamp updated_at
    }

    USER ||--o{ INVITE_CODE : "creates"
    USER ||--o| INVITE_CODE : "uses"
```

---

## 시퀀스 다이어그램

### 1. 로그인

```mermaid
sequenceDiagram
    actor Client
    participant Server
    participant KakaoAPI
    participant DB
    participant Redis

    Client->>Server: POST /api/v1/users/login { kakaoAccessToken }
    Server->>KakaoAPI: GET /v2/user/me (Bearer kakaoAccessToken)
    KakaoAPI-->>Server: { id, email, nickname }
    Server->>DB: SELECT * FROM user WHERE kakao_id = ?
    
    alt 기존 유저
        DB-->>Server: User 존재
        Server->>Redis: SET RT:{userId} refreshToken (TTL 14일)
        Server-->>Client: 200 { accessToken, refreshToken }
    else 신규 유저
        DB-->>Server: null
        Server-->>Client: 201 { status: "NEED_SIGNUP", kakaoId, email, nickname }
    end
```

### 2. 회원가입

```mermaid
sequenceDiagram
    actor Client
    participant Server
    participant DB
    participant Redis

    Client->>Server: POST /api/v1/users/signup { kakaoAccessToken, inviteCode, nickname }
    Server->>DB: SELECT * FROM invite_code WHERE code = ? AND used_by IS NULL
    
    alt 유효하지 않은 초대코드
        DB-->>Server: null
        Server-->>Client: 403 { message: "유효하지 않은 초대코드입니다." }
    else 유효한 초대코드
        Server->>DB: SELECT * FROM user WHERE nickname = ?
        alt 닉네임 중복
            DB-->>Server: User 존재
            Server-->>Client: 409 { message: "이미 사용 중인 닉네임입니다." }
        else 닉네임 사용 가능
            Server->>DB: INSERT user
            Server->>DB: UPDATE invite_code SET used_by = ?, used_at = NOW()
            Server->>DB: INSERT follows (inviter -> newUser), (newUser -> inviter) 상호 팔로우
            Server->>Redis: DEL NW:{inviter}:*, NW:{newUser}:* (촌수 캐시 무효화)
            Server->>Redis: SET RT:{userId} refreshToken (TTL 14일)
            Server-->>Client: 200 { accessToken, refreshToken }
        end
    end
```

### 3. 토큰 재발급 (RTR)

```mermaid
sequenceDiagram
    actor Client
    participant Server
    participant Redis

    Client->>Server: POST /api/v1/auth/refresh { refreshToken }
    Server->>Redis: GET RT:{userId}
    
    alt Redis에 저장된 RT와 불일치 (탈취 의심)
        Redis-->>Server: 다른 토큰 or null
        Server->>Redis: DEL RT:{userId} (모든 토큰 무효화)
        Server-->>Client: 401 { message: "인증이 필요합니다." }
    else 일치
        Server->>Redis: DEL RT:{userId} (기존 RT 삭제)
        Server->>Redis: SET RT:{userId} newRefreshToken (새 RT 저장)
        Server-->>Client: 200 { accessToken, refreshToken }
    end
```

### 4. 로그아웃

```mermaid
sequenceDiagram
    actor Client
    participant Server
    participant Redis

    Client->> Server: POST /api/v1/auth/logout (Bearer accessToken)
    Server->>Redis: DEL RT:{userId}
    Server->>Redis: SET BL:{accessToken} "logout" (TTL: AT 남은 만료시간)
    Server-->>Client: 200 { message: "로그아웃되었습니다." }
```

---

## API 명세

| Method | URL | Auth | 설명 |
|---|---|---|---|
| POST | /api/v1/users/login | X | 카카오 로그인 |
| POST | /api/v1/users/signup | X | 회원가입 |
| POST | /api/v1/auth/refresh | X | 토큰 재발급 |
| POST | /api/v1/auth/logout | O | 로그아웃 |
| POST | /internal/admin/seed-user | Admin Key | 씨앗 유저 생성 |