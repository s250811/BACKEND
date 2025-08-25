-- User 테이블
CREATE TABLE IF NOT EXISTS "user" (
                                      id BIGSERIAL PRIMARY KEY,
                                      email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    profile_image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_user_email ON "user"(email);
CREATE INDEX IF NOT EXISTS idx_user_created_at ON "user"(created_at);

-- Magic Link Token 테이블
CREATE TABLE IF NOT EXISTS magic_link_token (
                                                id BIGSERIAL PRIMARY KEY,
                                                email VARCHAR(255) NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_magic_link_token_email ON magic_link_token(email);
CREATE INDEX IF NOT EXISTS idx_magic_link_token_token ON magic_link_token(token);
CREATE INDEX IF NOT EXISTS idx_magic_link_token_expires_at ON magic_link_token(expires_at);