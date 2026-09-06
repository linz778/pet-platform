USE pet_platform;

CREATE TABLE IF NOT EXISTS t_sitter_address (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    sitter_id        BIGINT       NOT NULL COMMENT '接单员 user_id',
    label             VARCHAR(20)  NOT NULL COMMENT '家/学校/公司/其他',
    province          VARCHAR(50)  NOT NULL,
    city              VARCHAR(50)  NOT NULL,
    district          VARCHAR(50)  NOT NULL,
    detail_address    VARCHAR(255) NOT NULL COMMENT '地图候选地点及详细地址',
    lng               DECIMAL(10,7) NOT NULL,
    lat               DECIMAL(10,7) NOT NULL,
    default_address   TINYINT      NOT NULL DEFAULT 0 COMMENT '0=普通 1=默认搜索地址',
    create_time       DATETIME              DEFAULT NULL,
    update_time       DATETIME              DEFAULT NULL,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sitter_default (sitter_id, default_address),
    KEY idx_geo (lat, lng)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接单员搜索地址簿';
