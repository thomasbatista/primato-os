-- A Checklist Diário is now filled either by a Worker (the usual case) or directly by a
-- Manager visiting the site, so the worker link becomes optional and gains a counterpart.
ALTER TABLE daily_reports ALTER COLUMN filled_by_worker_id DROP NOT NULL;

ALTER TABLE daily_reports ADD COLUMN filled_by_user_id BIGINT REFERENCES users(id);

-- num_nonnulls is clearer than the equivalent "(a IS NULL) <> (b IS NULL)" XOR trick and
-- still extends cleanly if a third filler type is ever added. Existing rows all have a
-- worker and no user, so they already satisfy it — no backfill needed.
ALTER TABLE daily_reports ADD CONSTRAINT chk_daily_reports_filled_by_exactly_one
    CHECK (num_nonnulls(filled_by_worker_id, filled_by_user_id) = 1);

CREATE INDEX idx_daily_reports_filled_by_user_id ON daily_reports(filled_by_user_id);
