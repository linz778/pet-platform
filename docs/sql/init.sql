-- =============================================================
-- 宠物日常上门服务系统平台 - 数据库初始化脚本
-- MySQL 8.x / 9.x  字符集 utf8mb4
-- 首次运行：mysql -uroot -p < docs/sql/init.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS pet_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE pet_platform;

-- -------------------------------------------------------------
-- 用户（宠物主人 / 接单员 / 管理员，用 role 区分）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    phone       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    nickname    VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    avatar      VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/SITTER/ADMIN',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删 1=已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -------------------------------------------------------------
-- 宠物档案
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_pet;
CREATE TABLE t_pet (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL COMMENT '所属主人',
    name           VARCHAR(50) NOT NULL COMMENT '宠物昵称',
    species        VARCHAR(20)          DEFAULT NULL COMMENT '物种:狗/猫/其他',
    breed          VARCHAR(50)          DEFAULT NULL COMMENT '品种',
    gender         TINYINT              DEFAULT 0 COMMENT '0=未知 1=公 2=母',
    age_months     INT                  DEFAULT NULL COMMENT '年龄(月)',
    weight_kg      DECIMAL(6,2)         DEFAULT NULL COMMENT '体重kg',
    avatar         VARCHAR(255)         DEFAULT NULL COMMENT '宠物照片',
    vaccine_cert   VARCHAR(500)         DEFAULT NULL COMMENT '疫苗免疫证明图片URL(多个逗号分隔)',
    personality    VARCHAR(255)         DEFAULT NULL COMMENT '性格习性',
    feeding_taboo  VARCHAR(500)         DEFAULT NULL COMMENT '喂养禁忌',
    create_time    DATETIME             DEFAULT NULL,
    update_time    DATETIME             DEFAULT NULL,
    deleted        TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';

-- -------------------------------------------------------------
-- 服务类别与规则配置（单价/节假日溢价/平台抽成）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_service_category;
CREATE TABLE t_service_category (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    name               VARCHAR(50) NOT NULL COMMENT '服务名称:上门喂养/洗护/散步',
    code               VARCHAR(30) NOT NULL COMMENT '服务编码',
    base_price         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '基础单价',
    unit               VARCHAR(20)          DEFAULT '次' COMMENT '计价单位:次/小时',
    holiday_rate       DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '节假日溢价倍数',
    commission_rate    DECIMAL(4,3) NOT NULL DEFAULT 0.100 COMMENT '平台抽成比例',
    checklist_template VARCHAR(1000)        DEFAULT NULL COMMENT '标准作业清单模板(JSON/逗号分隔)',
    status             TINYINT     NOT NULL DEFAULT 1 COMMENT '0=下架 1=上架',
    create_time        DATETIME             DEFAULT NULL,
    update_time        DATETIME             DEFAULT NULL,
    deleted            TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务类别与规则表';

-- -------------------------------------------------------------
-- 接单员资质与信誉
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_sitter_profile;
CREATE TABLE t_sitter_profile (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL COMMENT '关联 t_user',
    real_name        VARCHAR(50)          DEFAULT NULL COMMENT '真实姓名',
    id_card          VARCHAR(50)          DEFAULT NULL COMMENT '身份证号(加密存储)',
    id_card_img      VARCHAR(500)         DEFAULT NULL COMMENT '身份证照片',
    health_cert      VARCHAR(500)         DEFAULT NULL COMMENT '健康证明',
    qualification    VARCHAR(500)         DEFAULT NULL COMMENT '训犬/美容等资质证书',
    experience_years INT                  DEFAULT 0 COMMENT '养宠/护理经验年限',
    audit_status     TINYINT     NOT NULL DEFAULT 0 COMMENT '0=待审 1=通过 2=驳回',
    audit_remark     VARCHAR(255)         DEFAULT NULL COMMENT '审核备注',
    credit_level     TINYINT     NOT NULL DEFAULT 3 COMMENT '信誉等级 1-5',
    current_lat      DECIMAL(10,7)        DEFAULT NULL COMMENT '当前纬度',
    current_lng      DECIMAL(10,7)        DEFAULT NULL COMMENT '当前经度',
    available        TINYINT     NOT NULL DEFAULT 1 COMMENT '是否可接单',
    create_time      DATETIME             DEFAULT NULL,
    update_time      DATETIME             DEFAULT NULL,
    deleted          TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id),
    KEY idx_geo (current_lat, current_lng),
    KEY idx_audit (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接单员资质表';

-- -------------------------------------------------------------
-- 接单员常用搜索地址簿
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_sitter_address;
CREATE TABLE t_sitter_address (
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

-- -------------------------------------------------------------
-- 订单
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    order_no         VARCHAR(40) NOT NULL COMMENT '订单号',
    user_id          BIGINT      NOT NULL COMMENT '下单用户',
    pet_id           BIGINT      NOT NULL COMMENT '服务宠物',
    category_id      BIGINT      NOT NULL COMMENT '服务类别',
    sitter_id        BIGINT               DEFAULT NULL COMMENT '接单员 user_id',
    service_address  VARCHAR(255) NOT NULL COMMENT '服务地址',
    address_lat      DECIMAL(10,7) NOT NULL COMMENT '服务地址纬度',
    address_lng      DECIMAL(10,7) NOT NULL COMMENT '服务地址经度',
    service_start    DATETIME    NOT NULL COMMENT '预约开始时间',
    service_end      DATETIME             DEFAULT NULL COMMENT '预约结束时间',
    amount           DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单金额',
    commission       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '平台抽成',
    sitter_income    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '接单员佣金',
    status           TINYINT     NOT NULL DEFAULT 0 COMMENT '0=待支付 1=待接单 2=已接单 3=服务中 4=待验收 5=已完成 6=已取消 7=仲裁中(本期不触发)',
    pay_status       TINYINT     NOT NULL DEFAULT 0 COMMENT '0=未支付 1=已支付(平台担保) 2=已结算 3=已退款',
    pay_time         DATETIME             DEFAULT NULL COMMENT '支付时间',
    taken_time       DATETIME             DEFAULT NULL COMMENT '被接单时间',
    checkin_time     DATETIME             DEFAULT NULL COMMENT '到达打卡时间',
    finish_time      DATETIME             DEFAULT NULL COMMENT '完成时间',
    accept_time      DATETIME             DEFAULT NULL COMMENT '用户验收时间',
    cancel_time      DATETIME             DEFAULT NULL COMMENT '取消时间',
    cancel_reason    VARCHAR(255)         DEFAULT NULL COMMENT '取消原因',
    remark           VARCHAR(500)         DEFAULT NULL COMMENT '备注',
    create_time      DATETIME             DEFAULT NULL,
    update_time      DATETIME             DEFAULT NULL,
    deleted          TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_sitter (sitter_id),
    KEY idx_status (status),
    KEY idx_geo_time (address_lat, address_lng, service_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- -------------------------------------------------------------
-- 履约存证（打卡 / 作业清单拍照）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_order_evidence;
CREATE TABLE t_order_evidence (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    order_id     BIGINT      NOT NULL,
    sitter_id    BIGINT      NOT NULL COMMENT '上传接单员',
    type         TINYINT     NOT NULL COMMENT '1=进门定位打卡 2=作业清单存证 3=散步轨迹',
    check_item   VARCHAR(50)          DEFAULT NULL COMMENT '清单项:换粮/添水/铲砂/梳毛等',
    image_url    VARCHAR(500)         DEFAULT NULL COMMENT '存证照片',
    lat          DECIMAL(10,7)        DEFAULT NULL COMMENT '上传纬度',
    lng          DECIMAL(10,7)        DEFAULT NULL COMMENT '上传经度',
    track_json   TEXT                 DEFAULT NULL COMMENT '散步轨迹点(JSON)',
    remark       VARCHAR(255)         DEFAULT NULL,
    create_time  DATETIME             DEFAULT NULL,
    update_time  DATETIME             DEFAULT NULL,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='履约存证表';

-- -------------------------------------------------------------
-- 钱包与流水
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_wallet;
CREATE TABLE t_wallet (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL COMMENT '所属用户；约定 0 = 平台佣金账户',
    balance     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '可用余额',
    frozen      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '冻结金额(担保中)',
    total_income DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计收入',
    create_time DATETIME          DEFAULT NULL,
    update_time DATETIME          DEFAULT NULL,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包表';

DROP TABLE IF EXISTS t_wallet_transaction;
CREATE TABLE t_wallet_transaction (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    wallet_id    BIGINT   NOT NULL,
    user_id      BIGINT   NOT NULL,
    type         TINYINT  NOT NULL COMMENT '1=充值 2=支付 3=佣金入账 4=提现 5=退款 6=平台佣金',
    amount       DECIMAL(12,2) NOT NULL COMMENT '金额(正负表示收支)',
    order_id     BIGINT            DEFAULT NULL COMMENT '关联订单',
    balance_after DECIMAL(12,2)    DEFAULT NULL COMMENT '变动后余额',
    remark       VARCHAR(255)      DEFAULT NULL,
    create_time  DATETIME          DEFAULT NULL,
    update_time  DATETIME          DEFAULT NULL,
    deleted      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_wallet (wallet_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水表';

-- -------------------------------------------------------------
-- 评价（双向）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_review;
CREATE TABLE t_review (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    order_id      BIGINT   NOT NULL,
    from_user_id  BIGINT   NOT NULL COMMENT '评价人',
    to_user_id    BIGINT   NOT NULL COMMENT '被评价人',
    rating        TINYINT  NOT NULL DEFAULT 5 COMMENT '星级 1-5',
    content       VARCHAR(500)      DEFAULT NULL,
    anonymous     TINYINT  NOT NULL DEFAULT 0,
    create_time   DATETIME          DEFAULT NULL,
    update_time   DATETIME          DEFAULT NULL,
    deleted       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_from (order_id, from_user_id),
    KEY idx_to_user (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- -------------------------------------------------------------
-- 纠纷仲裁
-- -------------------------------------------------------------
DROP TABLE IF EXISTS t_arbitration;
CREATE TABLE t_arbitration (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    order_id       BIGINT   NOT NULL,
    complainant_id BIGINT   NOT NULL COMMENT '申诉人',
    reason         VARCHAR(500) NOT NULL COMMENT '申诉理由',
    evidence       VARCHAR(1000)     DEFAULT NULL COMMENT '补充证据图片',
    status         TINYINT  NOT NULL DEFAULT 0 COMMENT '0=待处理 1=处理中 2=已裁定',
    result         VARCHAR(500)      DEFAULT NULL COMMENT '责任认定结果',
    refund_amount  DECIMAL(10,2)     DEFAULT 0 COMMENT '退款金额',
    handler_id     BIGINT            DEFAULT NULL COMMENT '处理管理员',
    create_time    DATETIME          DEFAULT NULL,
    update_time    DATETIME          DEFAULT NULL,
    deleted        TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纠纷仲裁表';

-- =============================================================
-- 种子数据
-- =============================================================
-- 管理员账号：admin / admin123
INSERT INTO t_user (username, password, phone, nickname, role, status, create_time, update_time)
VALUES ('admin', '$2a$10$2hHlopz2Bdg7RZHNVVJuSOBao0YoltCcv1j5wSPGHQ/jktWbwbjH6', '13800000000', '平台管理员', 'ADMIN', 1, NOW(), NOW());

-- 演示账号：user / 123456（宠物主人），sitter / 123456（接单员）
INSERT INTO t_user (username, password, phone, nickname, role, status, create_time, update_time)
VALUES ('user', '$2a$10$ug6hUJcrjL/3zWdSZ03Uoeo/qI001SsvnErjYT6NaSazDlLr4MiTG', '13800000001', '演示主人', 'USER', 1, NOW(), NOW());
INSERT INTO t_user (username, password, phone, nickname, role, status, create_time, update_time)
VALUES ('sitter', '$2a$10$ug6hUJcrjL/3zWdSZ03Uoeo/qI001SsvnErjYT6NaSazDlLr4MiTG', '13800000002', '演示接单员', 'SITTER', 1, NOW(), NOW());

-- 服务类别
INSERT INTO t_service_category (name, code, base_price, unit, holiday_rate, commission_rate, checklist_template, status, create_time, update_time) VALUES
('上门喂养', 'FEEDING', 40.00, '次', 1.50, 0.100, '换粮,添水,铲砂,梳毛,陪玩', 1, NOW(), NOW()),
('上门洗护', 'GROOMING', 80.00, '次', 1.50, 0.120, '梳毛,洗澡,吹干,剪指甲,清洁耳道', 1, NOW(), NOW()),
('户外散步', 'WALKING', 30.00, '次', 1.30, 0.100, '出门,牵引,散步轨迹,返程,喂水', 1, NOW(), NOW()),
('陪伴互动', 'COMPANION', 35.00, '小时', 1.30, 0.100, '玩具互动,喂食零食,状态观察,陪玩时长记录', 1, NOW(), NOW());

-- 为演示账号建钱包
-- 注意：user_id = 0 是平台佣金账户（约定），验收结算时平台抽成入账到此账户
INSERT INTO t_wallet (user_id, balance, frozen, total_income, create_time, update_time)
VALUES (0, 0.00, 0.00, 0.00, NOW(), NOW()),
       (2, 0.00, 0.00, 0.00, NOW(), NOW()),
       (3, 0.00, 0.00, 0.00, NOW(), NOW());

-- 接单员资质（演示，已通过审核）
INSERT INTO t_sitter_profile (user_id, real_name, experience_years, audit_status, credit_level, current_lat, current_lng, available, create_time, update_time)
VALUES (3, '演示接单员', 3, 1, 5, 31.2304000, 121.4737000, 1, NOW(), NOW());

-- 演示主人的宠物档案（下单流程开箱可演示）
INSERT INTO t_pet (user_id, name, species, breed, gender, age_months, weight_kg, personality, feeding_taboo, create_time, update_time) VALUES
(2, '豆豆', '狗', '柯基', 1, 26, 11.50, '亲人活泼，喜欢户外散步，见到陌生狗会兴奋吠叫', '禁食巧克力、葡萄、洋葱；每日狗粮定量 150g，分两餐', NOW(), NOW()),
(2, '奶糖', '猫', '英国短毛猫', 2, 14, 4.20, '性格安静认生，喜欢躲在猫爬架顶层，不喜被抱', '乳糖不耐受，禁止喂牛奶；猫粮需选低敏配方', NOW(), NOW());
