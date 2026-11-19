-- V1__init_schema.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email       VARCHAR(255) NOT NULL,
                       password    VARCHAR(255) NOT NULL,
                       nickname    VARCHAR(50)  NOT NULL,
                       role        VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
                       created_at  TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
                       CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE categories (
                            id          BIGSERIAL    PRIMARY KEY,
                            name        VARCHAR(50)  NOT NULL,
                            slug        VARCHAR(50)  NOT NULL,
                            description TEXT,
                            created_at  TIMESTAMP    NOT NULL DEFAULT now(),
                            CONSTRAINT uq_categories_name UNIQUE (name),
                            CONSTRAINT uq_categories_slug UNIQUE (slug)
);

-- public_id = 외부 URL 전용, id = 내부 전용
CREATE TABLE posts (
                       id           BIGSERIAL    PRIMARY KEY,
                       public_id    UUID         NOT NULL ,
                       user_id      UUID         NOT NULL,
                       category_id  BIGINT,
                       title        VARCHAR(300) NOT NULL,
                       content      TEXT         NOT NULL,
                       summary      TEXT,
                       thumbnail    VARCHAR(500),
                       status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
                       view_count   BIGINT       NOT NULL DEFAULT 0,
                       published_at TIMESTAMP,
                       created_at   TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
                       CONSTRAINT uq_posts_public_id UNIQUE (public_id),
                       CONSTRAINT fk_posts_user      FOREIGN KEY (user_id)     REFERENCES users(id),
                       CONSTRAINT fk_posts_category  FOREIGN KEY (category_id) REFERENCES categories(id),
                       CONSTRAINT chk_posts_status   CHECK (status IN ('DRAFT', 'PUBLISHED'))
);
CREATE INDEX idx_posts_public_id        ON posts(public_id);
CREATE INDEX idx_posts_status_published ON posts(status, published_at DESC);
CREATE INDEX idx_posts_user_id          ON posts(user_id);

CREATE TABLE tags (
                      id         BIGSERIAL   PRIMARY KEY,
                      name       VARCHAR(50) NOT NULL,
                      slug       VARCHAR(50) NOT NULL,
                      post_count INT         NOT NULL DEFAULT 0,
                      CONSTRAINT uq_tags_name UNIQUE (name),
                      CONSTRAINT uq_tags_slug UNIQUE (slug)
);

CREATE TABLE post_tags (
                           post_id BIGINT NOT NULL,
                           tag_id  BIGINT NOT NULL,
                           PRIMARY KEY (post_id, tag_id),
                           CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                           CONSTRAINT fk_post_tags_tag  FOREIGN KEY (tag_id)  REFERENCES tags(id)  ON DELETE CASCADE
);

CREATE TABLE comments (
                          id          BIGSERIAL    PRIMARY KEY,
                          public_id   UUID         NOT NULL DEFAULT gen_random_uuid(),
                          post_id     BIGINT       NOT NULL,
                          parent_id   BIGINT,
                          author_name VARCHAR(50)  NOT NULL,
                          password    VARCHAR(255) NOT NULL,
                          content     TEXT         NOT NULL,
                          is_deleted  BOOLEAN      NOT NULL DEFAULT false,
                          created_at  TIMESTAMP    NOT NULL DEFAULT now(),
                          CONSTRAINT uq_comments_public_id UNIQUE (public_id),
                          CONSTRAINT fk_comments_post      FOREIGN KEY (post_id)   REFERENCES posts(id) ON DELETE CASCADE,
                          CONSTRAINT fk_comments_parent    FOREIGN KEY (parent_id) REFERENCES comments(id)
);
CREATE INDEX idx_comments_post_id ON comments(post_id);

CREATE TABLE post_views (
                            id         BIGSERIAL PRIMARY KEY,
                            post_id    BIGINT    NOT NULL,
                            view_date  DATE      NOT NULL,
                            view_count BIGINT    NOT NULL DEFAULT 0,
                            CONSTRAINT uq_post_views UNIQUE (post_id, view_date),
                            CONSTRAINT fk_post_views_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE daily_visitors (
                                visit_date   DATE   PRIMARY KEY,
                                total_count  BIGINT NOT NULL DEFAULT 0,
                                unique_count BIGINT NOT NULL DEFAULT 0
);