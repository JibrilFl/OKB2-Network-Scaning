Для БД:
Таблица:

CREATE TABLE `devices` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`mac` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`ip` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`last_seen` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `mac_address` (`mac`) USING BTREE
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=3
;


Процедура:


DELIMITER //

CREATE PROCEDURE update_device_info(IN in_name VARCHAR(255), IN in_mac VARCHAR(255), IN in_ip VARCHAR(255))
BEGIN
    DECLARE existing_ip VARCHAR(255);

    SELECT ip INTO existing_ip FROM devices WHERE mac = in_mac;

    IF existing_ip IS NOT NULL THEN
        IF existing_ip <> in_ip THEN
            UPDATE devices SET ip = in_ip, last_seen = CURRENT_TIMESTAMP, name = in_name WHERE mac = in_mac;
        END IF;
    ELSE
        INSERT INTO devices (name, mac, ip, last_seen) VALUES (in_name, in_mac, in_ip, CURRENT_TIMESTAMP);
    END IF;
END //

DELIMITER ;
____________________________________________________________________________________________________________________

Для компиляции используем команду:
./gradlew buildFatJar  
после к исполняемому файлу добавляем файл config.json
описание настройки файла в файле .txt



