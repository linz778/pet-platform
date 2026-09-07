ALTER TABLE t_order
    ADD COLUMN order_type TINYINT NOT NULL DEFAULT 0 COMMENT '0=标准服务 1=悬赏任务' AFTER category_id,
    ADD COLUMN task_title VARCHAR(100) DEFAULT NULL COMMENT '悬赏任务标题' AFTER order_type,
    ADD COLUMN task_description VARCHAR(1000) DEFAULT NULL COMMENT '悬赏任务说明' AFTER task_title,
    ADD COLUMN task_review_remark VARCHAR(500) DEFAULT NULL COMMENT '管理员审核说明' AFTER task_description,
    ADD KEY idx_order_type_status (order_type, status);

INSERT INTO t_service_category
    (name, code, base_price, unit, holiday_rate, commission_rate, checklist_template, status, create_time, update_time)
SELECT '任务悬赏', 'BOUNTY', 0.01, '单', 1.00, 0.100, '', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_service_category WHERE code = 'BOUNTY' AND deleted = 0);
