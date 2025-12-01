-- 현재 세션 및 데이터베이스 타임존 설정
SET timezone = 'Asia/Seoul';
ALTER DATABASE postgres SET timezone = 'Asia/Seoul';

-- 테이블 조건부 초기화
CREATE SCHEMA IF NOT EXISTS public;

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
                                    last_modified_by BIGINT,

                                    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
                                    FOREIGN KEY (parent_id) REFERENCES task(id) ON DELETE CASCADE,
                                    FOREIGN KEY (last_modified_by) REFERENCES "user"(id) ON DELETE SET NULL,

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
-- Comment 테이블
CREATE TABLE IF NOT EXISTS comment (
                                       id BIGSERIAL PRIMARY KEY,
                                       task_id BIGINT NOT NULL,
                                       user_id BIGINT NOT NULL,
                                       content VARCHAR(2000) NOT NULL,
    file_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                             FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
    );

-- Comment 인덱스
CREATE INDEX IF NOT EXISTS idx_comment_task_id ON comment(task_id);
CREATE INDEX IF NOT EXISTS idx_comment_user_id ON comment(user_id);
CREATE INDEX IF NOT EXISTS idx_comment_created_at ON comment(created_at);

-- EventAudit 테이블
CREATE TABLE IF NOT EXISTS event_audit (
                                           id BIGSERIAL PRIMARY KEY,
                                           event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT status_check CHECK (status IN (
                                   'PENDING',
                                   'PROCESSING', -- Kafka 전송을 시도하기 직전에 PROCESSING으로 업데이트 (여러 Poller가 동시에 같은 이벤트를 가져가는 것을 방지 (낙관적 멱등 처리))
                                   'PUBLISHED',
                                   'FAILED_PUBLISH'
                                             )),
    retry_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- EventAudit 인덱스
CREATE INDEX IF NOT EXISTS idx_event_audit_event_id ON event_audit(event_id);
CREATE INDEX IF NOT EXISTS idx_event_audit_event_type ON event_audit(event_type);
CREATE INDEX IF NOT EXISTS idx_event_audit_status ON event_audit(status);
CREATE INDEX IF NOT EXISTS idx_event_audit_created_at ON event_audit(created_at);

-- 실패한 이벤트 조회를 위한 복합 인덱스
CREATE INDEX IF NOT EXISTS idx_event_audit_failed_events ON event_audit(status, created_at)
    WHERE status = 'FAILED';

-- Notification 테이블
CREATE TABLE IF NOT EXISTS notification (
                                            id BIGSERIAL PRIMARY KEY,
                                            recipient_id BIGINT NOT NULL,
                                            sender_id BIGINT NOT NULL,
                                            is_read BOOLEAN NOT NULL DEFAULT FALSE,
                                            event_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    CONSTRAINT notification_type_check CHECK (type IN (
                                              'TASK_ASSIGNED',
                                              'TASK_STATUS_CHANGED',
                                              'TASK_FIELDS_CHANGED',
                                              'TASK_MENTION_IN_DESCRIPTION',
                                              'COMMENT_MENTION',
                                              'SUBTASK_CREATED'
                                                      )),
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,

                          FOREIGN KEY (recipient_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES "user"(id) ON DELETE CASCADE
    );

-- Notification 인덱스
CREATE INDEX IF NOT EXISTS idx_notification_recipient_id ON notification(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notification_sender_id ON notification(sender_id);
CREATE INDEX IF NOT EXISTS idx_notification_event_id ON notification(event_id);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notification(type);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notification(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notification(is_read);

-- 읽지 않은 알림 조회를 위한 복합 인덱스
CREATE INDEX IF NOT EXISTS idx_notification_unread ON notification(recipient_id, is_read, created_at)
    WHERE is_read = FALSE;

-- 특정 사용자의 알림 목록 조회를 위한 복합 인덱스
CREATE INDEX IF NOT EXISTS idx_notification_recipient_created_desc ON notification(recipient_id, created_at DESC);
