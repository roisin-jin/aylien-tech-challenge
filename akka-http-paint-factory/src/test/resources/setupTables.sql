DROP TABLE IF EXISTS `api_user`;
CREATE TABLE `api_user` (
  id         BIGINT(20) NOT NULL AUTO_INCREMENT,
  app_id     VARCHAR(64)  NOT NULL,
  app_key    VARCHAR(64)  NOT NULL,
  email      VARCHAR(128) NOT NULL,
  has_expired  BOOLEAN NOT NULL DEFAULT 0,
  has_v1_access  BOOLEAN   NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT current_timestamp,
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_creds_UNIQUE` (`app_id`,`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;