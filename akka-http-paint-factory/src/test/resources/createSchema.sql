-- -----------------------------------------------------
-- Table `api_user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `api_user` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
`email` VARCHAR(128) NOT NULL,
`app_id` VARCHAR(64) NOT NULL,
`app_key` VARCHAR(64) NOT NULL,
`has_expired` INT(1) NOT NULL DEFAULT(0),
`has_v1_access` INT(1) NOT NULL DEFAULT(0),
PRIMARY KEY (`id`),
UNIQUE INDEX `app_creds_UNIQUE` (`app_id` ASC, `app_key` ASC))
ENGINE = InnoDB DEFAULT CHARSET=utf8;