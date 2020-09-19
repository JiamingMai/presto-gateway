SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for gateway_backend
-- ----------------------------
DROP TABLE IF EXISTS `gateway_backend`;
CREATE TABLE `gateway_backend` (
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `routing_group` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `backend_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for query_history
-- ----------------------------
DROP TABLE IF EXISTS `query_history`;
CREATE TABLE `query_history` (
  `query_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `query_text` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created` bigint(20) DEFAULT NULL,
  `backend_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`query_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for queued_query
-- ----------------------------
DROP TABLE IF EXISTS `queued_query`;
CREATE TABLE `queued_query` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `query_id` varchar(100) DEFAULT NULL,
  `user_name` varchar(30) DEFAULT NULL,
  `body` mediumtext,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `source` varchar(100) DEFAULT NULL,
  `catalog` varchar(100) DEFAULT NULL,
  `client_tags` text,
  `status` int(10) unsigned NOT NULL,
  `session_properties` text,
  `client_info` text,
  `priority` int(4) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `query_id_idx` (`query_id`)
) ENGINE=InnoDB AUTO_INCREMENT=166042 DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
