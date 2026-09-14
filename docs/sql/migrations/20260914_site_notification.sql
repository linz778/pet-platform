CREATE TABLE IF NOT EXISTS t_site_notification (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    title       VARCHAR(100)  NOT NULL,
    content     VARCHAR(500)  NOT NULL,
    type        VARCHAR(30)   NOT NULL,
    business_id BIGINT                 DEFAULT NULL,
    read_status TINYINT       NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    create_time DATETIME               DEFAULT NULL,
    update_time DATETIME               DEFAULT NULL,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_read_time (user_id, read_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';
