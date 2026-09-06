CREATE TABLE IF NOT EXISTS t_user_address (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL COMMENT '下单用户 user_id',
    label            VARCHAR(20)   NOT NULL COMMENT '家/学校/公司/其他',
    province         VARCHAR(50)   NOT NULL,
    city             VARCHAR(50)   NOT NULL,
    district         VARCHAR(50)   NOT NULL,
    detail_address   VARCHAR(255)  NOT NULL COMMENT '地图候选地点及详细地址',
    lng              DECIMAL(10,7) NOT NULL,
    lat              DECIMAL(10,7) NOT NULL,
    default_address  TINYINT       NOT NULL DEFAULT 0 COMMENT '0=普通 1=默认服务地址',
    create_time      DATETIME               DEFAULT NULL,
    update_time      DATETIME               DEFAULT NULL,
    deleted          TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_default (user_id, default_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户服务地址簿';
