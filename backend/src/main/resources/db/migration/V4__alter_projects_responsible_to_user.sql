ALTER TABLE projects DROP COLUMN responsible;

ALTER TABLE projects
    ADD COLUMN responsible_user_id BIGINT NOT NULL REFERENCES users(id);
