-- -----------------------------------------------------
-- Table `api_user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `api_user` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`email` VARCHAR(128) NOT NULL,
`app_id` VARCHAR(64) NOT NULL,
`app_key` VARCHAR(64) NOT NULL,
`has_expired` TINYINT(1) NOT NULL DEFAULT 0,
`has_v1_access` TINYINT(1) NOT NULL DEFAULT 0,
`created_at` TIMESTAMP NOT NULL DEFAULT current_timestamp,
PRIMARY KEY (`id`),
UNIQUE INDEX `app_creds_UNIQUE` (`app_id` ASC, `app_key` ASC))
ENGINE = InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------
-- Table `api_user_request_record`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `api_user_request_record` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`api_user_id` BIGINT(20) NOT NULL,
`http_method` VARCHAR(10) NOT NULL,
`http_uri` VARCHAR(1024) NOT NULL,
`post_body` VARCHAR(2048),
`response_code` VARCHAR(10) NOT NULL,
`response_message` VARCHAR(1024) NOT NULL,
`created_at` TIMESTAMP NOT NULL DEFAULT current_timestamp,
PRIMARY KEY (`id`),
FOREIGN KEY (`api_user_id`) REFERENCES `api_user`(`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `api_user` (`email`, `app_id`, `app_key`, `has_v1_access`) VALUES ("roisin@test.com", "testUserAppId", "testUserAppKey", true);