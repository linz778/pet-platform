-- 已初始化数据库的增量升级脚本：接单员宠物照护手记
CREATE TABLE IF NOT EXISTS t_sitter_pet_note (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    sitter_id     BIGINT        NOT NULL COMMENT '接单员 user_id',
    pet_id        BIGINT        NOT NULL COMMENT '服务过的宠物 id',
    pet_name      VARCHAR(50)   NOT NULL COMMENT '宠物名称快照',
    pet_species   VARCHAR(30)            DEFAULT NULL COMMENT '宠物物种快照',
    pet_avatar    VARCHAR(500)           DEFAULT NULL COMMENT '宠物头像快照',
    habits        VARCHAR(500)           DEFAULT NULL COMMENT '性格与习性',
    feeding_notes VARCHAR(500)           DEFAULT NULL COMMENT '饮食与禁忌',
    care_notes    VARCHAR(1000)          DEFAULT NULL COMMENT '服务心得与注意事项',
    create_time   DATETIME               DEFAULT NULL,
    update_time   DATETIME               DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sitter_update (sitter_id, update_time),
    KEY idx_sitter_pet (sitter_id, pet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接单员宠物照护手记';
