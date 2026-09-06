-- 接单员信誉分：满分 100，取消惩罚时最低扣到 0。
ALTER TABLE t_sitter_profile
    ADD COLUMN credit_score TINYINT NOT NULL DEFAULT 100 COMMENT '信誉分 0-100' AFTER credit_level;
