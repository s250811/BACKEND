-- 테이블 초기화
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;


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


-- Workspace 테이블
CREATE TABLE IF NOT EXISTS workspace (
                                         id BIGSERIAL PRIMARY KEY,
    workspace_name VARCHAR(255) NOT NULL,
                                         img_url TEXT,
    description VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE IF NOT EXISTS workspacemember (


                                       id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
                                       nickname VARCHAR(255) NOT NULL,
    description VARCHAR(500),
                                       role VARCHAR(255) NOT NULL,
                                       CONSTRAINT status_check CHECK (role IN ('OWNER', 'MEMBER')),

                                       is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (workspace_id) REFERENCES workspace(id),
                                       FOREIGN KEY (user_id) REFERENCES "user"(id)

);


CREATE TABLE IF NOT EXISTS workspaceinvite (
       id BIGSERIAL PRIMARY KEY,
       workspace_id BIGINT NOT NULL,
       from_user_id BIGINT NOT NULL,
       to_user_id BIGINT NOT NULL,
       status varchar(255) NOT NULL,
        CONSTRAINT status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    FOREIGN KEY (from_user_id) REFERENCES "user"(id),
    FOREIGN KEY (to_user_id) REFERENCES "user"(id),

    UNIQUE (workspace_id, to_user_id)
);

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_workspaceinvite_workspace_id ON workspaceinvite(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspaceinvite_from_user_id ON workspaceinvite(from_user_id);
CREATE INDEX IF NOT EXISTS idx_workspaceinvite_to_user_id ON workspaceinvite(to_user_id);

-- Folder 테이블 (상위 프로젝트)
CREATE TABLE IF NOT EXISTS folder (
      id BIGSERIAL PRIMARY KEY,
      workspace_id BIGINT NOT NULL,
      folder_name VARCHAR(255) NOT NULL,
      is_deleted BOOLEAN DEFAULT FALSE,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

      FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_folder_workspace_id ON folder(workspace_id);

-- Project 테이블 (하위 프로젝트)
CREATE TABLE IF NOT EXISTS project (
                                       id BIGSERIAL PRIMARY KEY,
                                       folder_id BIGINT NOT NULL,
                                       project_name VARCHAR(255) NOT NULL,
                                       description VARCHAR(255) NOT NULL,
                                       is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                       FOREIGN KEY (folder_id) REFERENCES folder(id)
);

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_project_folder_id ON project(folder_id);


-- Task 테이블
-- task status enum 타입 생성
CREATE TABLE IF NOT EXISTS task (
                                    id BIGSERIAL PRIMARY KEY,
                                    project_id BIGINT NOT NULL,
                                    parent_id BIGINT,
                                    task_name VARCHAR(255) NOT NULL,
                                    description VARCHAR(1000),
                                    task_status VARCHAR(255) NOT NULL,
                                    file_url TEXT,
                                    CONSTRAINT status_check CHECK (task_status IN ('BACKLOG', 'TO_DO', 'IN_PROGRESS', 'PENDING', 'DONE')),
                                    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                    start_date TIMESTAMP WITH TIME ZONE,
                                    end_date TIMESTAMP WITH TIME ZONE,
                                    created_at TIMESTAMP WITH TIME ZONE,
                                    updated_at TIMESTAMP WITH TIME ZONE,

                                    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
                                    FOREIGN KEY (parent_id) REFERENCES task(id) ON DELETE CASCADE,

                                    CONSTRAINT chk_parent_id_not_self CHECK (id <> parent_id)
);

CREATE INDEX IF NOT EXISTS idx_task_project_id ON task(project_id);
CREATE INDEX IF NOT EXISTS idx_task_parent_id ON task(parent_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON task(task_status);

-- Task 담당자 매핑 테이블
CREATE TABLE IF NOT EXISTS task_manager (
                                            id BIGSERIAL PRIMARY KEY,
                                            task_id BIGINT NOT NULL,
                                            user_id BIGINT NOT NULL,
                                            created_at TIMESTAMP WITH TIME ZONE,
                                            updated_at TIMESTAMP WITH TIME ZONE,

                                            FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
                                            FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

-- 휴지통 테이블
CREATE TABLE IF NOT EXISTS trash (
                                         id BIGSERIAL PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,

                                         item_id BIGINT NOT NULL,
                                        type VARCHAR(255) NOT NULL,
                                            CONSTRAINT type_check CHECK (type IN ('folder', 'project', 'task')),

                                         deleted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         deleted_by_user_id BIGINT,

                                         UNIQUE (item_id),

                                         FOREIGN KEY (deleted_by_user_id) REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_trash_bin_deleted_by_user_id ON trash(deleted_by_user_id);
CREATE INDEX IF NOT EXISTS idx_trash_bin_deleted_at ON trash(deleted_at);

-- 비밀번호는 "password"를 BCrypt로 해싱한 값입니다.
INSERT INTO "user" (email, password, nickname, created_at, updated_at)
VALUES (
           'user1@example.com',
           'password',
           'root',
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       ) ON CONFLICT (email) DO NOTHING;

INSERT INTO "user" (email, password, nickname, created_at, updated_at)
VALUES (
           'user2@example.com',
           'password',
           'root12',
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       ) ON CONFLICT (email) DO NOTHING;