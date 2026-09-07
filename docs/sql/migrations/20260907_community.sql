-- 已初始化数据库的增量升级脚本：宠物社区
CREATE TABLE IF NOT EXISTS t_community_post (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    author_id     BIGINT        NOT NULL COMMENT '发布用户',
    type          TINYINT       NOT NULL COMMENT '1=晒宠分享 2=养宠问答',
    pet_id        BIGINT                 DEFAULT NULL COMMENT '关联自己的宠物档案',
    title         VARCHAR(100)  NOT NULL,
    content       VARCHAR(2000) NOT NULL,
    images        VARCHAR(1500)          DEFAULT NULL COMMENT '图片 URL，逗号分隔，最多5张',
    like_count    INT           NOT NULL DEFAULT 0,
    comment_count INT           NOT NULL DEFAULT 0,
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '0=隐藏 1=公开',
    create_time   DATETIME               DEFAULT NULL,
    update_time   DATETIME               DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_type_time (type, create_time),
    KEY idx_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物社区帖子';

CREATE TABLE IF NOT EXISTS t_community_comment (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    post_id     BIGINT        NOT NULL,
    author_id   BIGINT        NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '0=隐藏 1=公开',
    create_time DATETIME               DEFAULT NULL,
    update_time DATETIME               DEFAULT NULL,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_post_time (post_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物社区评论与回答';

CREATE TABLE IF NOT EXISTS t_community_like (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    post_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    create_time DATETIME          DEFAULT NULL,
    update_time DATETIME          DEFAULT NULL,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_user (post_id, user_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物社区爪印点赞';
