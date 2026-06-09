-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: atwrpg
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `account_non_expired` tinyint(1) NOT NULL DEFAULT '1',
  `account_non_locked` tinyint(1) NOT NULL DEFAULT '1',
  `credentials_non_expired` tinyint(1) NOT NULL DEFAULT '1',
  `failed_login_attempts` int NOT NULL DEFAULT '0',
  `locked_until` timestamp NULL DEFAULT NULL,
  `last_login` timestamp NULL DEFAULT NULL,
  `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `theme_preference` varchar(50) DEFAULT NULL,
  `active_wrestler_id` bigint DEFAULT NULL,
  `legacy_score` bigint NOT NULL DEFAULT '0',
  `prestige` bigint NOT NULL DEFAULT '0',
  `shows_booked` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_account_username` (`username`),
  KEY `idx_account_email` (`email`),
  KEY `idx_account_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account`
--

LOCK TABLES `account` WRITE;
/*!40000 ALTER TABLE `account` DISABLE KEYS */;
INSERT INTO `account` VALUES (1,'admin','$2a$10$wKGJ2IuP7HwMP66VaqSdYuqo3S1lcXpl9oqQkTGuLaDYHfbH57hD6','admin@atwrpg.local',1,1,1,1,0,NULL,NULL,'2026-06-09 01:51:06','2026-06-09 01:51:06',NULL,NULL,0,0,0),(2,'booker','$2a$10$OrFNvKFkH5s/DvDzd301Me4v9bpIulbPNasymqmaxCqaUM.kVXHEi','booker@atwrpg.local',1,1,1,1,0,NULL,NULL,'2026-06-09 01:51:06','2026-06-09 01:51:06',NULL,NULL,0,0,0),(3,'player','$2a$10$oHciydemMfshOLiGK7g4KO.Epu07svrzinu7PFvdJws5PYK3pIKx.','player@atwrpg.local',1,1,1,1,0,NULL,NULL,'2026-06-09 01:51:06','2026-06-09 01:51:06',NULL,NULL,0,0,0),(4,'viewer','$2a$10$no8XHshPMFd14eBxIs9e2uYW8bXm/pT6MOZsXnw.RHyhmRWgvok06','viewer@atwrpg.local',1,1,1,1,0,NULL,NULL,'2026-06-09 01:51:06','2026-06-09 01:51:06',NULL,NULL,0,0,0);
/*!40000 ALTER TABLE `account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `account_achievement`
--

DROP TABLE IF EXISTS `account_achievement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_achievement` (
  `account_id` bigint NOT NULL,
  `achievement_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`,`achievement_id`),
  KEY `fk_account_achievement_achievement` (`achievement_id`),
  CONSTRAINT `fk_account_achievement_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_account_achievement_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `achievement` (`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_achievement`
--

LOCK TABLES `account_achievement` WRITE;
/*!40000 ALTER TABLE `account_achievement` DISABLE KEYS */;
/*!40000 ALTER TABLE `account_achievement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `account_roles`
--

DROP TABLE IF EXISTS `account_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_roles` (
  `account_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`account_id`,`role_id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `account_roles_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `account_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_roles`
--

LOCK TABLES `account_roles` WRITE;
/*!40000 ALTER TABLE `account_roles` DISABLE KEYS */;
INSERT INTO `account_roles` VALUES (1,1),(2,2),(3,3),(4,4);
/*!40000 ALTER TABLE `account_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `achievement`
--

DROP TABLE IF EXISTS `achievement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `achievement` (
  `achievement_id` bigint NOT NULL AUTO_INCREMENT,
  `achievement_key` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `xp_value` int NOT NULL,
  `category` varchar(50) NOT NULL,
  `icon_url` varchar(512) DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`achievement_id`),
  UNIQUE KEY `uc_achievement_key` (`achievement_key`),
  UNIQUE KEY `uc_achievement_external_id` (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `achievement`
--

LOCK TABLES `achievement` WRITE;
/*!40000 ALTER TABLE `achievement` DISABLE KEYS */;
/*!40000 ALTER TABLE `achievement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `arena`
--

DROP TABLE IF EXISTS `arena`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `arena` (
  `arena_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `location_id` bigint NOT NULL,
  `capacity` int NOT NULL,
  `alignment_bias` varchar(255) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` datetime(6) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`arena_id`),
  UNIQUE KEY `uc_arena_name` (`name`),
  UNIQUE KEY `uc_arena_external_id` (`external_id`),
  KEY `fk_arena_on_location` (`location_id`),
  CONSTRAINT `fk_arena_on_location` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `arena`
--

LOCK TABLES `arena` WRITE;
/*!40000 ALTER TABLE `arena` DISABLE KEYS */;
/*!40000 ALTER TABLE `arena` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `arena_environmental_trait`
--

DROP TABLE IF EXISTS `arena_environmental_trait`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `arena_environmental_trait` (
  `arena_id` bigint NOT NULL,
  `environmental_trait` varchar(255) DEFAULT NULL,
  KEY `fk_arena_env_trait_on_arena` (`arena_id`),
  CONSTRAINT `fk_arena_env_trait_on_arena` FOREIGN KEY (`arena_id`) REFERENCES `arena` (`arena_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `arena_environmental_trait`
--

LOCK TABLES `arena_environmental_trait` WRITE;
/*!40000 ALTER TABLE `arena_environmental_trait` DISABLE KEYS */;
/*!40000 ALTER TABLE `arena_environmental_trait` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `backstage_action_history`
--

DROP TABLE IF EXISTS `backstage_action_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backstage_action_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `action_type` varchar(50) NOT NULL,
  `action_date` datetime NOT NULL,
  `dice_rolled` int NOT NULL,
  `successes` int NOT NULL,
  `outcome_description` text,
  PRIMARY KEY (`id`),
  KEY `idx_backstage_action_history_campaign` (`campaign_id`),
  CONSTRAINT `backstage_action_history_ibfk_1` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backstage_action_history`
--

LOCK TABLES `backstage_action_history` WRITE;
/*!40000 ALTER TABLE `backstage_action_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `backstage_action_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign`
--

DROP TABLE IF EXISTS `campaign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `status` varchar(50) NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `ended_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_campaign_wrestler` (`wrestler_id`),
  KEY `fk_campaign_universe` (`universe_id`),
  CONSTRAINT `campaign_ibfk_1` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`),
  CONSTRAINT `fk_campaign_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign`
--

LOCK TABLES `campaign` WRITE;
/*!40000 ALTER TABLE `campaign` DISABLE KEYS */;
INSERT INTO `campaign` VALUES (1,1,'ACTIVE','2026-06-09 01:51:13',NULL,'2026-06-09 01:51:13',1);
/*!40000 ALTER TABLE `campaign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_ability_card`
--

DROP TABLE IF EXISTS `campaign_ability_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_ability_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  `alignment_type` varchar(50) NOT NULL,
  `level` int NOT NULL,
  `effect_script` varchar(255) DEFAULT NULL,
  `one_time_use` tinyint(1) NOT NULL DEFAULT '1',
  `timing` varchar(50) DEFAULT NULL,
  `secondary_effect_script` varchar(255) DEFAULT NULL,
  `secondary_one_time_use` tinyint(1) NOT NULL DEFAULT '0',
  `secondary_timing` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_alignment_level` (`name`,`alignment_type`,`level`),
  KEY `idx_campaign_ability_card_alignment` (`alignment_type`,`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_ability_card`
--

LOCK TABLES `campaign_ability_card` WRITE;
/*!40000 ALTER TABLE `campaign_ability_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_ability_card` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_completed_chapters`
--

DROP TABLE IF EXISTS `campaign_completed_chapters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_completed_chapters` (
  `campaign_state_id` bigint NOT NULL,
  `chapter_id` varchar(255) NOT NULL,
  KEY `campaign_state_id` (`campaign_state_id`),
  CONSTRAINT `campaign_completed_chapters_ibfk_1` FOREIGN KEY (`campaign_state_id`) REFERENCES `campaign_state` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_completed_chapters`
--

LOCK TABLES `campaign_completed_chapters` WRITE;
/*!40000 ALTER TABLE `campaign_completed_chapters` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_completed_chapters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_encounter`
--

DROP TABLE IF EXISTS `campaign_encounter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_encounter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `chapter_id` varchar(255) NOT NULL,
  `narrative_text` text NOT NULL,
  `player_choice` text,
  `alignment_shift` int NOT NULL DEFAULT '0',
  `vp_reward` int NOT NULL DEFAULT '0',
  `encounter_date` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_campaign_encounter_campaign` (`campaign_id`),
  CONSTRAINT `campaign_encounter_ibfk_1` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_encounter`
--

LOCK TABLES `campaign_encounter` WRITE;
/*!40000 ALTER TABLE `campaign_encounter` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_encounter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_state`
--

DROP TABLE IF EXISTS `campaign_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_state` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `current_chapter_id` varchar(255) DEFAULT NULL,
  `victory_points` int NOT NULL DEFAULT '0',
  `skill_tokens` int NOT NULL DEFAULT '0',
  `health_penalty` int NOT NULL DEFAULT '0',
  `opponent_health_penalty` int NOT NULL DEFAULT '0',
  `hand_size_penalty` int NOT NULL DEFAULT '0',
  `stamina_penalty` int NOT NULL DEFAULT '0',
  `current_phase` varchar(50) NOT NULL DEFAULT 'BACKSTAGE',
  `actions_taken` int NOT NULL DEFAULT '0',
  `last_action_type` varchar(50) DEFAULT NULL,
  `last_action_success` tinyint(1) DEFAULT '1',
  `promo_unlocked` tinyint(1) NOT NULL DEFAULT '0',
  `attack_unlocked` tinyint(1) NOT NULL DEFAULT '0',
  `pending_l1_picks` int NOT NULL DEFAULT '0',
  `pending_l2_picks` int NOT NULL DEFAULT '0',
  `pending_l3_picks` int NOT NULL DEFAULT '0',
  `matches_played` int NOT NULL DEFAULT '0',
  `wins` int NOT NULL DEFAULT '0',
  `losses` int NOT NULL DEFAULT '0',
  `rival_id` bigint DEFAULT NULL,
  `current_match_id` bigint DEFAULT NULL,
  `momentum_bonus` int NOT NULL DEFAULT '0',
  `current_game_date` date DEFAULT NULL,
  `last_sync` datetime DEFAULT NULL,
  `feature_data` text,
  `active_storyline_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `rival_id` (`rival_id`),
  KEY `idx_campaign_state_campaign` (`campaign_id`),
  KEY `idx_campaign_state_current_match` (`current_match_id`),
  KEY `fk_campaign_state_storyline` (`active_storyline_id`),
  CONSTRAINT `campaign_state_ibfk_1` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`),
  CONSTRAINT `campaign_state_ibfk_2` FOREIGN KEY (`rival_id`) REFERENCES `wrestler` (`wrestler_id`),
  CONSTRAINT `campaign_state_ibfk_3` FOREIGN KEY (`current_match_id`) REFERENCES `segment` (`segment_id`),
  CONSTRAINT `fk_campaign_state_storyline` FOREIGN KEY (`active_storyline_id`) REFERENCES `campaign_storyline` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_state`
--

LOCK TABLES `campaign_state` WRITE;
/*!40000 ALTER TABLE `campaign_state` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_state_cards`
--

DROP TABLE IF EXISTS `campaign_state_cards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_state_cards` (
  `campaign_state_id` bigint NOT NULL,
  `card_id` bigint NOT NULL,
  KEY `campaign_state_id` (`campaign_state_id`),
  KEY `card_id` (`card_id`),
  CONSTRAINT `campaign_state_cards_ibfk_1` FOREIGN KEY (`campaign_state_id`) REFERENCES `campaign_state` (`id`),
  CONSTRAINT `campaign_state_cards_ibfk_2` FOREIGN KEY (`card_id`) REFERENCES `campaign_ability_card` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_state_cards`
--

LOCK TABLES `campaign_state_cards` WRITE;
/*!40000 ALTER TABLE `campaign_state_cards` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_state_cards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_state_upgrades`
--

DROP TABLE IF EXISTS `campaign_state_upgrades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_state_upgrades` (
  `campaign_state_id` bigint NOT NULL,
  `upgrade_id` bigint NOT NULL,
  PRIMARY KEY (`campaign_state_id`,`upgrade_id`),
  KEY `fk_csu_upgrade` (`upgrade_id`),
  CONSTRAINT `fk_csu_state` FOREIGN KEY (`campaign_state_id`) REFERENCES `campaign_state` (`id`),
  CONSTRAINT `fk_csu_upgrade` FOREIGN KEY (`upgrade_id`) REFERENCES `campaign_upgrade` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_state_upgrades`
--

LOCK TABLES `campaign_state_upgrades` WRITE;
/*!40000 ALTER TABLE `campaign_state_upgrades` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_state_upgrades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_storyline`
--

DROP TABLE IF EXISTS `campaign_storyline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_storyline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `current_milestone_id` bigint DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `ended_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_storyline_campaign` (`campaign_id`),
  KEY `fk_storyline_current_milestone` (`current_milestone_id`),
  CONSTRAINT `fk_storyline_campaign` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`),
  CONSTRAINT `fk_storyline_current_milestone` FOREIGN KEY (`current_milestone_id`) REFERENCES `storyline_milestone` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_storyline`
--

LOCK TABLES `campaign_storyline` WRITE;
/*!40000 ALTER TABLE `campaign_storyline` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_storyline` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campaign_upgrade`
--

DROP TABLE IF EXISTS `campaign_upgrade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campaign_upgrade` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `type` varchar(50) NOT NULL,
  `sub_type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campaign_upgrade`
--

LOCK TABLES `campaign_upgrade` WRITE;
/*!40000 ALTER TABLE `campaign_upgrade` DISABLE KEYS */;
/*!40000 ALTER TABLE `campaign_upgrade` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card`
--

DROP TABLE IF EXISTS `card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `card` (
  `card_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `type` varchar(100) NOT NULL,
  `damage` int NOT NULL DEFAULT '0',
  `stamina` int NOT NULL DEFAULT '0',
  `momentum` int NOT NULL DEFAULT '0',
  `target` int NOT NULL DEFAULT '0',
  `number` int NOT NULL,
  `finisher` tinyint(1) NOT NULL DEFAULT '0',
  `signature` tinyint(1) NOT NULL DEFAULT '0',
  `pin` tinyint(1) NOT NULL DEFAULT '0',
  `taunt` tinyint(1) NOT NULL DEFAULT '0',
  `recover` tinyint(1) NOT NULL DEFAULT '0',
  `creation_date` timestamp NOT NULL,
  `set_id` bigint NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`card_id`),
  UNIQUE KEY `number` (`number`,`set_id`),
  KEY `set_id` (`set_id`),
  CONSTRAINT `card_ibfk_1` FOREIGN KEY (`set_id`) REFERENCES `card_set` (`set_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card`
--

LOCK TABLES `card` WRITE;
/*!40000 ALTER TABLE `card` DISABLE KEYS */;
/*!40000 ALTER TABLE `card` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card_set`
--

DROP TABLE IF EXISTS `card_set`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `card_set` (
  `set_id` bigint NOT NULL AUTO_INCREMENT,
  `set_code` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `release_date` date DEFAULT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`set_id`),
  UNIQUE KEY `set_code` (`set_code`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card_set`
--

LOCK TABLES `card_set` WRITE;
/*!40000 ALTER TABLE `card_set` DISABLE KEYS */;
/*!40000 ALTER TABLE `card_set` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `commentary_team`
--

DROP TABLE IF EXISTS `commentary_team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commentary_team` (
  `team_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `external_id` (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `commentary_team`
--

LOCK TABLES `commentary_team` WRITE;
/*!40000 ALTER TABLE `commentary_team` DISABLE KEYS */;
/*!40000 ALTER TABLE `commentary_team` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `commentary_team_members`
--

DROP TABLE IF EXISTS `commentary_team_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commentary_team_members` (
  `team_id` bigint NOT NULL,
  `commentator_id` bigint NOT NULL,
  PRIMARY KEY (`team_id`,`commentator_id`),
  KEY `commentator_id` (`commentator_id`),
  CONSTRAINT `commentary_team_members_ibfk_1` FOREIGN KEY (`team_id`) REFERENCES `commentary_team` (`team_id`) ON DELETE CASCADE,
  CONSTRAINT `commentary_team_members_ibfk_2` FOREIGN KEY (`commentator_id`) REFERENCES `commentator` (`commentator_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `commentary_team_members`
--

LOCK TABLES `commentary_team_members` WRITE;
/*!40000 ALTER TABLE `commentary_team_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `commentary_team_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `commentator`
--

DROP TABLE IF EXISTS `commentator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commentator` (
  `commentator_id` bigint NOT NULL AUTO_INCREMENT,
  `npc_id` bigint NOT NULL,
  `style` varchar(255) DEFAULT NULL,
  `catchphrase` varchar(255) DEFAULT NULL,
  `persona_description` text,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`commentator_id`),
  UNIQUE KEY `npc_id` (`npc_id`),
  UNIQUE KEY `external_id` (`external_id`),
  CONSTRAINT `commentator_ibfk_1` FOREIGN KEY (`npc_id`) REFERENCES `npc` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `commentator`
--

LOCK TABLES `commentator` WRITE;
/*!40000 ALTER TABLE `commentator` DISABLE KEYS */;
/*!40000 ALTER TABLE `commentator` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `deck`
--

DROP TABLE IF EXISTS `deck`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deck` (
  `deck_id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`deck_id`),
  KEY `wrestler_id` (`wrestler_id`),
  CONSTRAINT `deck_ibfk_1` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `deck`
--

LOCK TABLES `deck` WRITE;
/*!40000 ALTER TABLE `deck` DISABLE KEYS */;
/*!40000 ALTER TABLE `deck` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `deck_card`
--

DROP TABLE IF EXISTS `deck_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deck_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `deck_id` bigint NOT NULL,
  `card_id` bigint NOT NULL,
  `set_id` bigint NOT NULL,
  `amount` int NOT NULL DEFAULT '1',
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `deck_id` (`deck_id`,`card_id`,`set_id`),
  KEY `card_id` (`card_id`),
  KEY `set_id` (`set_id`),
  CONSTRAINT `deck_card_ibfk_1` FOREIGN KEY (`deck_id`) REFERENCES `deck` (`deck_id`) ON DELETE CASCADE,
  CONSTRAINT `deck_card_ibfk_2` FOREIGN KEY (`card_id`) REFERENCES `card` (`card_id`) ON DELETE CASCADE,
  CONSTRAINT `deck_card_ibfk_3` FOREIGN KEY (`set_id`) REFERENCES `card_set` (`set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `deck_card`
--

LOCK TABLES `deck_card` WRITE;
/*!40000 ALTER TABLE `deck_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `deck_card` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `draft`
--

DROP TABLE IF EXISTS `draft`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `draft` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `league_id` bigint NOT NULL,
  `status` varchar(50) NOT NULL,
  `current_turn_user_id` bigint DEFAULT NULL,
  `current_round` int NOT NULL DEFAULT '1',
  `current_pick_number` int NOT NULL DEFAULT '1',
  `direction` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_draft_league` (`league_id`),
  KEY `fk_draft_turn_user` (`current_turn_user_id`),
  CONSTRAINT `fk_draft_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`),
  CONSTRAINT `fk_draft_turn_user` FOREIGN KEY (`current_turn_user_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `draft`
--

LOCK TABLES `draft` WRITE;
/*!40000 ALTER TABLE `draft` DISABLE KEYS */;
/*!40000 ALTER TABLE `draft` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `draft_pick`
--

DROP TABLE IF EXISTS `draft_pick`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `draft_pick` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `draft_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  `pick_number` int NOT NULL,
  `round` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_draft_wrestler` (`draft_id`,`wrestler_id`),
  UNIQUE KEY `uk_draft_pick_number` (`draft_id`,`pick_number`),
  KEY `fk_pick_user` (`user_id`),
  KEY `fk_pick_wrestler` (`wrestler_id`),
  CONSTRAINT `fk_pick_draft` FOREIGN KEY (`draft_id`) REFERENCES `draft` (`id`),
  CONSTRAINT `fk_pick_user` FOREIGN KEY (`user_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_pick_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `draft_pick`
--

LOCK TABLES `draft_pick` WRITE;
/*!40000 ALTER TABLE `draft_pick` DISABLE KEYS */;
/*!40000 ALTER TABLE `draft_pick` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drama_event`
--

DROP TABLE IF EXISTS `drama_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drama_event` (
  `drama_event_id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `event_type` varchar(255) NOT NULL,
  `severity` varchar(255) NOT NULL,
  `event_date` timestamp NOT NULL,
  `creation_date` timestamp NOT NULL,
  `heat_impact` int DEFAULT NULL,
  `fan_impact` int DEFAULT NULL,
  `injury_caused` tinyint(1) NOT NULL,
  `rivalry_created` tinyint(1) NOT NULL,
  `rivalry_ended` tinyint(1) NOT NULL,
  `is_processed` tinyint(1) NOT NULL,
  `processed_date` timestamp NULL DEFAULT NULL,
  `processing_notes` longtext,
  `primary_wrestler_id` bigint DEFAULT NULL,
  `secondary_wrestler_id` bigint DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`drama_event_id`),
  KEY `primary_wrestler_id` (`primary_wrestler_id`),
  KEY `secondary_wrestler_id` (`secondary_wrestler_id`),
  KEY `fk_drama_universe` (`universe_id`),
  KEY `idx_drama_event_processed_date` (`is_processed`,`event_date`),
  CONSTRAINT `drama_event_ibfk_1` FOREIGN KEY (`primary_wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE SET NULL,
  CONSTRAINT `drama_event_ibfk_2` FOREIGN KEY (`secondary_wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_drama_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drama_event`
--

LOCK TABLES `drama_event` WRITE;
/*!40000 ALTER TABLE `drama_event` DISABLE KEYS */;
INSERT INTO `drama_event` VALUES (1,'Reference Event','Test drama event','BACKSTAGE_INCIDENT','NEUTRAL','2026-06-09 01:51:13','2026-06-09 01:51:13',NULL,NULL,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,'2026-06-09 01:51:13',1);
/*!40000 ALTER TABLE `drama_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faction`
--

DROP TABLE IF EXISTS `faction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faction` (
  `faction_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `leader_id` bigint DEFAULT NULL,
  `formed_date` timestamp NULL DEFAULT NULL,
  `disbanded_date` timestamp NULL DEFAULT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `affinity` int NOT NULL DEFAULT '0',
  `alignment` varchar(50) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `image_url` varchar(512) DEFAULT NULL,
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`faction_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `manager_id` (`manager_id`),
  KEY `fk_faction_leader` (`leader_id`),
  KEY `fk_faction_universe` (`universe_id`),
  CONSTRAINT `faction_ibfk_1` FOREIGN KEY (`manager_id`) REFERENCES `npc` (`id`),
  CONSTRAINT `fk_faction_leader` FOREIGN KEY (`leader_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_faction_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faction`
--

LOCK TABLES `faction` WRITE;
/*!40000 ALTER TABLE `faction` DISABLE KEYS */;
INSERT INTO `faction` VALUES (1,'Reference Faction','Snapshot seed faction',1,NULL,NULL,NULL,'2026-06-09 01:51:13',NULL,NULL,NULL,0,NULL,'2026-06-09 01:51:13',NULL,1);
/*!40000 ALTER TABLE `faction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faction_heat_event`
--

DROP TABLE IF EXISTS `faction_heat_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faction_heat_event` (
  `faction_heat_event_id` bigint NOT NULL AUTO_INCREMENT,
  `faction_rivalry_id` bigint NOT NULL,
  `heat_change` int NOT NULL,
  `heat_after_event` int NOT NULL,
  `reason` varchar(255) NOT NULL,
  `event_date` timestamp NOT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`faction_heat_event_id`),
  KEY `faction_rivalry_id` (`faction_rivalry_id`),
  CONSTRAINT `faction_heat_event_ibfk_1` FOREIGN KEY (`faction_rivalry_id`) REFERENCES `faction_rivalry` (`faction_rivalry_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faction_heat_event`
--

LOCK TABLES `faction_heat_event` WRITE;
/*!40000 ALTER TABLE `faction_heat_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `faction_heat_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faction_rivalry`
--

DROP TABLE IF EXISTS `faction_rivalry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faction_rivalry` (
  `faction_rivalry_id` bigint NOT NULL AUTO_INCREMENT,
  `faction1_id` bigint NOT NULL,
  `faction2_id` bigint NOT NULL,
  `heat` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `started_date` timestamp NOT NULL,
  `ended_date` timestamp NULL DEFAULT NULL,
  `storyline_notes` longtext,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`faction_rivalry_id`),
  UNIQUE KEY `faction1_id` (`faction1_id`,`faction2_id`),
  KEY `faction2_id` (`faction2_id`),
  CONSTRAINT `faction_rivalry_ibfk_1` FOREIGN KEY (`faction1_id`) REFERENCES `faction` (`faction_id`) ON DELETE CASCADE,
  CONSTRAINT `faction_rivalry_ibfk_2` FOREIGN KEY (`faction2_id`) REFERENCES `faction` (`faction_id`) ON DELETE CASCADE,
  CONSTRAINT `faction_rivalry_chk_1` CHECK ((`faction1_id` <> `faction2_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faction_rivalry`
--

LOCK TABLES `faction_rivalry` WRITE;
/*!40000 ALTER TABLE `faction_rivalry` DISABLE KEYS */;
/*!40000 ALTER TABLE `faction_rivalry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `feud_heat_event`
--

DROP TABLE IF EXISTS `feud_heat_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feud_heat_event` (
  `feud_heat_event_id` bigint NOT NULL AUTO_INCREMENT,
  `feud_id` bigint NOT NULL,
  `heat_change` int NOT NULL,
  `heat_after_event` int NOT NULL,
  `reason` varchar(500) NOT NULL,
  `event_date` timestamp NOT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feud_heat_event_id`),
  KEY `feud_id` (`feud_id`),
  CONSTRAINT `feud_heat_event_ibfk_1` FOREIGN KEY (`feud_id`) REFERENCES `multi_wrestler_feud` (`multi_wrestler_feud_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feud_heat_event`
--

LOCK TABLES `feud_heat_event` WRITE;
/*!40000 ALTER TABLE `feud_heat_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `feud_heat_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `feud_participant`
--

DROP TABLE IF EXISTS `feud_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feud_participant` (
  `feud_participant_id` bigint NOT NULL AUTO_INCREMENT,
  `feud_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  `role` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `joined_date` timestamp NOT NULL,
  `left_date` timestamp NULL DEFAULT NULL,
  `left_reason` varchar(255) DEFAULT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feud_participant_id`),
  UNIQUE KEY `feud_id` (`feud_id`,`wrestler_id`),
  KEY `wrestler_id` (`wrestler_id`),
  CONSTRAINT `feud_participant_ibfk_1` FOREIGN KEY (`feud_id`) REFERENCES `multi_wrestler_feud` (`multi_wrestler_feud_id`) ON DELETE CASCADE,
  CONSTRAINT `feud_participant_ibfk_2` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feud_participant`
--

LOCK TABLES `feud_participant` WRITE;
/*!40000 ALTER TABLE `feud_participant` DISABLE KEYS */;
/*!40000 ALTER TABLE `feud_participant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','Initial Schema','SQL','V1__Initial_Schema.sql',-2075464759,'atwrpg','2026-06-09 01:51:06',631,1),(2,'2','Add Won At Segment To Title Reign','SQL','V2__Add_Won_At_Segment_To_Title_Reign.sql',-332785961,'atwrpg','2026-06-09 01:51:06',46,1),(3,'3','Rename Show Table','SQL','V3__Rename_Show_Table.sql',-1147003678,'atwrpg','2026-06-09 01:51:06',22,1),(4,'4','Add Unique Constraint To Wrestler Account','SQL','V4__Add_Unique_Constraint_To_Wrestler_Account.sql',0,'atwrpg','2026-06-09 01:51:06',3,1),(5,'5','Create Campaign System','SQL','V5__Create_Campaign_System.sql',-1212479758,'atwrpg','2026-06-09 01:51:07',500,1),(6,'6','Add Theme Preference And Default Setting','SQL','V6__Add_Theme_Preference_And_Default_Setting.sql',-913225845,'atwrpg','2026-06-09 01:51:07',23,1),(7,'7','Add League System','SQL','V7__Add_League_System.sql',100936296,'atwrpg','2026-06-09 01:51:07',226,1),(8,'9','Add Image Url To Npc','SQL','V9__Add_Image_Url_To_Npc.sql',-1391881153,'atwrpg','2026-06-09 01:51:07',25,1),(9,'10','Add Image Url To Show Template','SQL','V10__Add_Image_Url_To_Show_Template.sql',2091528606,'atwrpg','2026-06-09 01:51:07',34,1),(10,'11','Add Expected Matches And Promos To ShowTemplate','SQL','V11__Add_Expected_Matches_And_Promos_To_ShowTemplate.sql',-1363888031,'atwrpg','2026-06-09 01:51:07',39,1),(11,'12','Add Recurrence And Defense Frequency','SQL','V12__Add_Recurrence_And_Defense_Frequency.sql',-33215304,'atwrpg','2026-06-09 01:51:07',157,1),(12,'13','Remove LocalAI Settings','SQL','V13__Remove_LocalAI_Settings.sql',-2084740732,'atwrpg','2026-06-09 01:51:07',3,1),(13,'14','Create Commentary Tables','SQL','V14__Create_Commentary_Tables.sql',-1829352772,'atwrpg','2026-06-09 01:51:08',236,1),(14,'15','Create News Tables','SQL','V15__Create_News_Tables.sql',-2003982983,'atwrpg','2026-06-09 01:51:08',11,1),(15,'16','Add Gender Constraint To ShowTemplate','SQL','V16__Add_Gender_Constraint_To_ShowTemplate.sql',-1162501534,'atwrpg','2026-06-09 01:51:08',23,1),(16,'17','Add Legacy Fields To Account','SQL','V17__Add_Legacy_Fields_To_Account.sql',-508336936,'atwrpg','2026-06-09 01:51:08',98,1),(17,'18','Create Achievement Tables','SQL','V18__Create_Achievement_Tables.sql',1925586479,'atwrpg','2026-06-09 01:51:08',26,1),(18,'19','Add Affinity To Faction','SQL','V19__Add_Affinity_To_Faction.sql',1676214726,'atwrpg','2026-06-09 01:51:08',29,1),(19,'20','Remove Rivalry Unique Constraint','SQL','V20__Remove_Rivalry_Unique_Constraint.sql',-1313235155,'atwrpg','2026-06-09 01:51:08',24,1),(20,'21','Create Storyline Tables','SQL','V21__Create_Storyline_Tables.sql',-1288453859,'atwrpg','2026-06-09 01:51:08',113,1),(21,'22','Add Npc Attributes','SQL','V22__Add_Npc_Attributes.sql',1944315555,'atwrpg','2026-06-09 01:51:08',30,1),(22,'23','Add Referee To Segment','SQL','V23__Add_Referee_To_Segment.sql',1177591894,'atwrpg','2026-06-09 01:51:08',129,1),(23,'24','Add No Dq To Segment Rule','SQL','V24__Add_No_Dq_To_Segment_Rule.sql',621834710,'atwrpg','2026-06-09 01:51:08',16,1),(24,'25','Add Ringside Actions','SQL','V25__Add_Ringside_Actions.sql',-837106943,'atwrpg','2026-06-09 01:51:08',26,1),(25,'26','Add Physical Condition To Wrestler','SQL','V26__Add_Physical_Condition_To_Wrestler.sql',-2031017789,'atwrpg','2026-06-09 01:51:09',70,1),(26,'27','Create Location Arena Tables And Link Show','SQL','V27__Create_Location_Arena_Tables_And_Link_Show.sql',-282762121,'atwrpg','2026-06-09 01:51:09',281,1),(27,'28','Remove Wrestler Account Unique Constraint','SQL','V28__Remove_Wrestler_Account_Unique_Constraint.sql',-433816961,'atwrpg','2026-06-09 01:51:09',10,1),(28,'29','Add Faction Alignment And Team Fields','SQL','V29__Add_Faction_Alignment_And_Team_Fields.sql',-393460592,'atwrpg','2026-06-09 01:51:09',78,1),(29,'30','Add Updated At To Entities','SQL','V30__Add_Updated_At_To_Entities.sql',-1658631685,'atwrpg','2026-06-09 01:51:10',839,1),(30,'31','Initialize Set Enablement Settings','SQL','V31__Initialize_Set_Enablement_Settings.sql',727117897,'atwrpg','2026-06-09 01:51:10',6,1),(31,'32','Add Expansion Code To Wrestler','SQL','V32__Add_Expansion_Code_To_Wrestler.sql',-911082154,'atwrpg','2026-06-09 01:51:10',67,1),(32,'33','Add Expansion Code To Npc','SQL','V33__Add_Expansion_Code_To_Npc.sql',1736603957,'atwrpg','2026-06-09 01:51:10',29,1),(33,'34','Add Image Url To Various Entities','SQL','V34__Add_Image_Url_To_Various_Entities.sql',-517119444,'atwrpg','2026-06-09 01:51:10',73,1),(34,'35','Create Wrestler Relationship Table','SQL','V35__Create_Wrestler_Relationship_Table.sql',387058247,'atwrpg','2026-06-09 01:51:10',53,1),(35,'36','Add GM Mode Financials And Logistics','SQL','V36__Add_GM_Mode_Financials_And_Logistics.sql',896273371,'atwrpg','2026-06-09 01:51:10',342,1),(36,'37','Add Effect Script To Title','SQL','V37__Add_Effect_Script_To_Title.sql',1842801306,'atwrpg','2026-06-09 01:51:10',23,1),(37,'38','Add Show Attendance And Revenue','SQL','V38__Add_Show_Attendance_And_Revenue.sql',1941232678,'atwrpg','2026-06-09 01:51:11',46,1),(38,'39','Add Notes To Segment','SQL','V39__Add_Notes_To_Segment.sql',1957484762,'atwrpg','2026-06-09 01:51:11',33,1),(39,'40','Create Status Card Tables','SQL','V40__Create_Status_Card_Tables.sql',326802875,'atwrpg','2026-06-09 01:51:11',63,1),(40,'41','add campaign ability card constraint','SQL','V41__add_campaign_ability_card_constraint.sql',-1424910555,'atwrpg','2026-06-09 01:51:11',12,1),(41,'42','Add Team Number To Segment Participant','SQL','V42__Add_Team_Number_To_Segment_Participant.sql',-1799038452,'atwrpg','2026-06-09 01:51:11',14,1),(42,'43','Create Universe Table','SQL','V43__Create_Universe_Table.sql',-2040159274,'atwrpg','2026-06-09 01:51:11',9,1),(43,'44','Create Wrestler State Table','SQL','V44__Create_Wrestler_State_Table.sql',-1904189481,'atwrpg','2026-06-09 01:51:11',27,1),(44,'45','Add Universe Membership','SQL','V45__Add_Universe_Membership.sql',2092049629,'atwrpg','2026-06-09 01:51:11',18,1),(45,'46','Add Universe Settings Tables','SQL','V46__Add_Universe_Settings_Tables.sql',1564362811,'atwrpg','2026-06-09 01:51:11',25,1),(46,'47','Deactivate Same Faction Rivalries','SQL','V47__Deactivate_Same_Faction_Rivalries.sql',-351764884,'atwrpg','2026-06-09 01:51:11',5,1),(47,'48','Add RivalryId To Segment','SQL','V48__Add_RivalryId_To_Segment.sql',1611206126,'atwrpg','2026-06-09 01:51:11',46,1),(48,'49','add campaign ability card constraint idempotent','SQL','V49__add_campaign_ability_card_constraint_idempotent.sql',2027248105,'atwrpg','2026-06-09 01:51:11',23,1),(49,'50','Add Team Number To Segment Participant idempotent','SQL','V50__Add_Team_Number_To_Segment_Participant_idempotent.sql',1618984521,'atwrpg','2026-06-09 01:51:11',4,1),(50,'51','Decouple Wrestler State','SQL','V51__Decouple_Wrestler_State.sql',-369822378,'atwrpg','2026-06-09 01:51:11',368,1),(51,'52','Drop Deprecated Wrestler Columns','SQL','V52__Drop_Deprecated_Wrestler_Columns.sql',-2102293264,'atwrpg','2026-06-09 01:51:12',999,1),(52,'53','Backfill Universe Id','SQL','V53__Backfill_Universe_Id.sql',-1694769477,'atwrpg','2026-06-09 01:51:12',13,1),(53,'54','Add Universe Alignment','SQL','V54__Add_Universe_Alignment.sql',-1733031706,'atwrpg','2026-06-09 01:51:12',48,1),(54,'55','Seed Rivalry Lifecycle Settings','SQL','V55__Seed_Rivalry_Lifecycle_Settings.sql',-661848111,'atwrpg','2026-06-09 01:51:12',2,1),(55,'56','Replace Defense Frequency With Type','SQL','V56__Replace_Defense_Frequency_With_Type.sql',2101993557,'atwrpg','2026-06-09 01:51:13',54,1),(56,'57','Add Outcome Matrix','SQL','V57__Add_Outcome_Matrix.sql',-1072229307,'atwrpg','2026-06-09 01:51:13',19,1),(57,'58','Add DramaEvent Cleanup Index','SQL','V58__Add_DramaEvent_Cleanup_Index.sql',-1939705152,'atwrpg','2026-06-09 01:51:13',11,1),(58,'59','Link Injury To InjuryType','SQL','V59__Link_Injury_To_InjuryType.sql',-1300595049,'atwrpg','2026-06-09 01:51:13',45,1),(59,'60','Add Wrestler Season Snapshot','SQL','V60__Add_Wrestler_Season_Snapshot.sql',-840879091,'atwrpg','2026-06-09 01:51:13',14,1),(60,'61','Add Universe To Rivalry','SQL','V61__Add_Universe_To_Rivalry.sql',-1158894585,'atwrpg','2026-06-09 01:51:13',53,1),(61,'62','Add Universe Scoped Game Settings','SQL','V62__Add_Universe_Scoped_Game_Settings.sql',1150524815,'atwrpg','2026-06-09 01:51:13',54,1),(62,'63','Migrate Credentials To Default Universe','SQL','V63__Migrate_Credentials_To_Default_Universe.sql',-1857336965,'atwrpg','2026-06-09 01:51:13',4,1),(63,'64','Create Universe Invite And Join Request Tables','SQL','V64__Create_Universe_Invite_And_Join_Request_Tables.sql',-1932337377,'atwrpg','2026-06-09 01:51:13',77,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `game_setting`
--

DROP TABLE IF EXISTS `game_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(255) NOT NULL,
  `setting_value` varchar(255) NOT NULL,
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_game_setting_key_universe` (`setting_key`,`universe_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `game_setting`
--

LOCK TABLES `game_setting` WRITE;
/*!40000 ALTER TABLE `game_setting` DISABLE KEYS */;
INSERT INTO `game_setting` VALUES (1,'AI_CLAUDE_API_KEY','',1),(2,'AI_CLAUDE_API_URL','https://api.anthropic.com/v1/messages/',NULL),(3,'AI_CLAUDE_ENABLED','false',NULL),(4,'AI_CLAUDE_MODEL_NAME','claude-3-haiku-20240307',NULL),(5,'AI_GEMINI_API_KEY','',1),(6,'AI_GEMINI_API_URL','https://generativelanguage.googleapis.com/v1beta/models/',NULL),(7,'AI_GEMINI_ENABLED','false',NULL),(8,'AI_GEMINI_MODEL_NAME','gemini-2.5-flash',NULL),(9,'AI_OPENAI_API_KEY','',1),(10,'AI_OPENAI_API_URL','https://api.openai.com/v1/chat/completions',NULL),(11,'AI_OPENAI_DEFAULT_MODEL','gpt-3.5-turbo',NULL),(12,'AI_OPENAI_ENABLED','false',NULL),(13,'AI_OPENAI_MAX_TOKENS','1000',NULL),(14,'AI_OPENAI_PREMIUM_MODEL','gpt-4',NULL),(15,'AI_OPENAI_TEMPERATURE','0.7',NULL),(16,'AI_PROVIDER_AUTO','true',NULL),(17,'AI_TIMEOUT','300',NULL),(18,'default_theme','light',NULL),(19,'rivalry_heat_decay_enabled','false',NULL),(20,'rivalry_heat_decay_interval_days','7',NULL),(21,'rivalry_heat_decay_per_interval','1',NULL),(22,'rivalry_max_duration_days','0',NULL),(23,'rivalry_resolution_on_regular_shows','false',NULL),(24,'rivalry_resolution_threshold_ple','30',NULL),(25,'rivalry_resolution_threshold_regular','35',NULL),(26,'set_enabled_BASE_GAME','true',NULL),(27,'set_enabled_EDDIE','true',NULL),(28,'set_enabled_EXTREME','true',NULL),(29,'set_enabled_MATT_CARDONA','true',NULL),(30,'set_enabled_RUMBLE','true',NULL),(31,'set_enabled_TRAILBLAZERS','true',NULL);
/*!40000 ALTER TABLE `game_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `heat_event`
--

DROP TABLE IF EXISTS `heat_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `heat_event` (
  `heat_event_id` bigint NOT NULL AUTO_INCREMENT,
  `rivalry_id` bigint NOT NULL,
  `heat_change` int NOT NULL,
  `heat_after_event` int NOT NULL,
  `reason` varchar(500) NOT NULL,
  `event_date` timestamp NOT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`heat_event_id`),
  KEY `rivalry_id` (`rivalry_id`),
  CONSTRAINT `heat_event_ibfk_1` FOREIGN KEY (`rivalry_id`) REFERENCES `rivalry` (`rivalry_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `heat_event`
--

LOCK TABLES `heat_event` WRITE;
/*!40000 ALTER TABLE `heat_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `heat_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `holiday`
--

DROP TABLE IF EXISTS `holiday`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `holiday` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) NOT NULL,
  `theme` varchar(255) NOT NULL,
  `decorations` text,
  `day_of_month` int DEFAULT NULL,
  `holiday_month` varchar(255) DEFAULT NULL,
  `day_of_week` varchar(255) DEFAULT NULL,
  `week_of_month` int DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `description` (`description`),
  UNIQUE KEY `external_id` (`external_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `holiday`
--

LOCK TABLES `holiday` WRITE;
/*!40000 ALTER TABLE `holiday` DISABLE KEYS */;
INSERT INTO `holiday` VALUES (1,'New Year\'s Day','New Year\'s Day','New Year’s Day decorations are typically clean, festive, and hopeful in tone. They often feature metallic accents like gold, silver, and champagne, paired with white or soft neutrals to suggest a fresh start. Banners and signage display the new year, while streamers, balloons, and confetti add energy without feeling heavy. Clocks, stars, and fireworks motifs symbolize time, renewal, and celebration. Table settings may include sparkling centerpieces, candles, and subtle glitter, creating a bright, optimistic atmosphere that feels celebratory but calm—marking both reflection and new beginnings.',1,'JANUARY',NULL,NULL,'FIXED','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(2,'Valentine\'s Day','Valentine\'s Day','Valentine’s Day decorations create a warm, romantic atmosphere centered on **reds, pinks, and soft whites**. Common elements include **hearts, roses, and love-themed banners**, often accented with **lace, ribbons, and soft lighting** like candles or string lights. **Floral arrangements, plush accents, and subtle metallic touches** add elegance, while table settings may feature **romantic centerpieces and themed place cards**, setting a cozy, intimate mood focused on love and affection.\n',14,'FEBRUARY',NULL,NULL,'FIXED','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(3,'St. Patrick\'s Day','St. Patrick\'s Day','St. Patrick’s Day decorations are bright and festive, dominated by **shades of green** with accents of **gold and white**. Common elements include **shamrocks, leprechauns, rainbows, and pots of gold**, often paired with **Irish flags or Celtic patterns**. **Banners, garlands, and themed table décor** add a playful touch, while touches of **gold foil or glitter** bring a sense of luck and celebration, creating a cheerful, lively atmosphere rooted in Irish tradition.\n',17,'MARCH',NULL,NULL,'FIXED','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(4,'Independence Day','Independence Day','USA Independence Day decorations are bold and patriotic, featuring **red, white, and blue** throughout. Common elements include **American flags, stars, stripes, and bunting**, often paired with **fireworks imagery**. **Banners, balloons, and table décor** showcase patriotic patterns, while **rustic or outdoor accents** like lanterns and string lights enhance the celebratory feel. The overall atmosphere is energetic and proud, reflecting national unity and summer celebration.\n',4,'JULY',NULL,NULL,'FIXED','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(5,'Halloween','Halloween','Halloween decorations create a spooky yet playful atmosphere using **black, orange, and purple** as the primary colors. Common elements include **pumpkins, jack-o’-lanterns, ghosts, bats, spiders, and cobwebs**, often paired with **dim lighting, candles, or colored lights**. **Haunted house props, eerie silhouettes, and fog effects** add drama, while whimsical touches keep the mood fun and festive rather than frightening.\n',31,'OCTOBER',NULL,NULL,'FIXED','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(6,'Christmas Day','Christmas Day','Christmas Day decorations create a warm, joyful atmosphere centered on **reds, greens, golds, and whites**. Common elements include **Christmas trees adorned with ornaments, lights, and garlands**, along with **wreaths, stockings, and nativity scenes**. **Twinkling lights, candles, and festive table settings** add warmth and sparkle, while touches of **pine, holly, and ribbon** evoke tradition, togetherness, and holiday cheer.\n',25,'DECEMBER',NULL,NULL,'FIXED','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(7,'Memorial Day','Memorial Day','Memorial Day decorations are respectful and patriotic, featuring **red, white, and blue** with a more subdued tone than other holidays. Common elements include **American flags, banners, and bunting**, often paired with **stars, ribbons, and wreaths**. **Floral arrangements**, especially red and white flowers, and **simple table décor** reflect remembrance and honor, creating an atmosphere that balances national pride with solemn respect.\n',NULL,'MAY','MONDAY',-1,'FLOATING','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(8,'Labor Day','Labor Day','Labor Day decorations are casual and patriotic, reflecting both national pride and the spirit of the working community. They often feature **red, white, and blue** with simple, relaxed elements like **flags, banners, and bunting**. **Outdoor-friendly décor**, such as table coverings, string lights, and picnic accents, is common, creating a laid-back, celebratory atmosphere that marks the end of summer and honors workers’ contributions.\n',NULL,'SEPTEMBER','MONDAY',1,'FLOATING','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10'),(9,'Thanksgiving','Thanksgiving','Thanksgiving decorations create a warm, welcoming atmosphere inspired by the **fall harvest**. They feature **earthy tones** like orange, brown, gold, and deep red, with elements such as **pumpkins, gourds, autumn leaves, and cornucopias**. **Rustic table settings, candles, and natural textures** like wood and burlap add coziness, emphasizing gratitude, abundance, and togetherness.\n',NULL,'NOVEMBER','THURSDAY',4,'FLOATING','2026-06-09 01:51:06',NULL,NULL,'2026-06-09 01:51:10');
/*!40000 ALTER TABLE `holiday` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inbox_item`
--

DROP TABLE IF EXISTS `inbox_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inbox_item` (
  `inbox_item_id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(255) NOT NULL,
  `description` varchar(1024) NOT NULL,
  `event_timestamp` timestamp NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`inbox_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inbox_item`
--

LOCK TABLES `inbox_item` WRITE;
/*!40000 ALTER TABLE `inbox_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `inbox_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inbox_item_target`
--

DROP TABLE IF EXISTS `inbox_item_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inbox_item_target` (
  `inbox_item_target_id` bigint NOT NULL AUTO_INCREMENT,
  `inbox_item_id` bigint DEFAULT NULL,
  `target_id` varchar(255) NOT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `target_type` varchar(20) NOT NULL DEFAULT 'ACCOUNT',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`inbox_item_target_id`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `inbox_item_id` (`inbox_item_id`),
  CONSTRAINT `inbox_item_target_ibfk_1` FOREIGN KEY (`inbox_item_id`) REFERENCES `inbox_item` (`inbox_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inbox_item_target`
--

LOCK TABLES `inbox_item_target` WRITE;
/*!40000 ALTER TABLE `inbox_item_target` DISABLE KEYS */;
/*!40000 ALTER TABLE `inbox_item_target` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `injury`
--

DROP TABLE IF EXISTS `injury`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `injury` (
  `injury_id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `severity` varchar(255) NOT NULL,
  `health_penalty` int NOT NULL,
  `is_active` tinyint(1) NOT NULL,
  `injury_date` timestamp NOT NULL,
  `healed_date` timestamp NULL DEFAULT NULL,
  `healing_cost` bigint NOT NULL,
  `injury_notes` longtext,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `stamina_penalty` int NOT NULL DEFAULT '0',
  `hand_size_penalty` int NOT NULL DEFAULT '0',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `universe_id` bigint DEFAULT NULL,
  `injury_type_id` bigint NOT NULL,
  PRIMARY KEY (`injury_id`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `wrestler_id` (`wrestler_id`),
  KEY `fk_injury_universe` (`universe_id`),
  KEY `fk_injury_injury_type` (`injury_type_id`),
  CONSTRAINT `fk_injury_injury_type` FOREIGN KEY (`injury_type_id`) REFERENCES `injury_type` (`injury_type_id`),
  CONSTRAINT `fk_injury_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`),
  CONSTRAINT `injury_ibfk_1` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `injury`
--

LOCK TABLES `injury` WRITE;
/*!40000 ALTER TABLE `injury` DISABLE KEYS */;
/*!40000 ALTER TABLE `injury` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `injury_type`
--

DROP TABLE IF EXISTS `injury_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `injury_type` (
  `injury_type_id` bigint NOT NULL AUTO_INCREMENT,
  `injury_name` varchar(100) NOT NULL,
  `health_effect` int DEFAULT NULL,
  `stamina_effect` int DEFAULT NULL,
  `card_effect` int DEFAULT NULL,
  `special_effects` longtext,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`injury_type_id`),
  UNIQUE KEY `injury_name` (`injury_name`),
  UNIQUE KEY `external_id` (`external_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `injury_type`
--

LOCK TABLES `injury_type` WRITE;
/*!40000 ALTER TABLE `injury_type` DISABLE KEYS */;
INSERT INTO `injury_type` VALUES (1,'Legacy Injury',0,0,0,'Placeholder for injuries that existed before injury types were introduced. Update to the correct type when known.',NULL,NULL,'2026-06-09 01:51:13'),(2,'Sprain',-2,-1,0,NULL,NULL,NULL,'2026-06-09 01:51:13');
/*!40000 ALTER TABLE `injury_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `league`
--

DROP TABLE IF EXISTS `league`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `league` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `commissioner_id` bigint NOT NULL,
  `status` varchar(50) NOT NULL,
  `max_picks_per_player` int NOT NULL DEFAULT '1',
  `budget` decimal(19,2) DEFAULT '0.00',
  `duration_weeks` int DEFAULT NULL,
  `locker_room_morale` int NOT NULL DEFAULT '100',
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `fk_league_commissioner` (`commissioner_id`),
  KEY `fk_league_universe` (`universe_id`),
  CONSTRAINT `fk_league_commissioner` FOREIGN KEY (`commissioner_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_league_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `league`
--

LOCK TABLES `league` WRITE;
/*!40000 ALTER TABLE `league` DISABLE KEYS */;
/*!40000 ALTER TABLE `league` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `league_excluded_wrestler`
--

DROP TABLE IF EXISTS `league_excluded_wrestler`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `league_excluded_wrestler` (
  `league_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  PRIMARY KEY (`league_id`,`wrestler_id`),
  KEY `fk_excluded_wrestler` (`wrestler_id`),
  CONSTRAINT `fk_excluded_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_excluded_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `league_excluded_wrestler`
--

LOCK TABLES `league_excluded_wrestler` WRITE;
/*!40000 ALTER TABLE `league_excluded_wrestler` DISABLE KEYS */;
/*!40000 ALTER TABLE `league_excluded_wrestler` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `league_membership`
--

DROP TABLE IF EXISTS `league_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `league_membership` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `league_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `role` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_league_member` (`league_id`,`member_id`),
  KEY `fk_membership_member` (`member_id`),
  CONSTRAINT `fk_membership_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`),
  CONSTRAINT `fk_membership_member` FOREIGN KEY (`member_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `league_membership`
--

LOCK TABLES `league_membership` WRITE;
/*!40000 ALTER TABLE `league_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `league_membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `league_roster`
--

DROP TABLE IF EXISTS `league_roster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `league_roster` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `league_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  `wins` int NOT NULL DEFAULT '0',
  `losses` int NOT NULL DEFAULT '0',
  `draws` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_league_wrestler` (`league_id`,`wrestler_id`),
  KEY `fk_roster_owner` (`owner_id`),
  KEY `fk_roster_wrestler` (`wrestler_id`),
  CONSTRAINT `fk_roster_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`),
  CONSTRAINT `fk_roster_owner` FOREIGN KEY (`owner_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_roster_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `league_roster`
--

LOCK TABLES `league_roster` WRITE;
/*!40000 ALTER TABLE `league_roster` DISABLE KEYS */;
/*!40000 ALTER TABLE `league_roster` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location`
--

DROP TABLE IF EXISTS `location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `location` (
  `location_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `image_url` varchar(255) DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` datetime(6) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`location_id`),
  UNIQUE KEY `uc_location_name` (`name`),
  UNIQUE KEY `uc_location_external_id` (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location`
--

LOCK TABLES `location` WRITE;
/*!40000 ALTER TABLE `location` DISABLE KEYS */;
/*!40000 ALTER TABLE `location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_cultural_tag`
--

DROP TABLE IF EXISTS `location_cultural_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `location_cultural_tag` (
  `location_id` bigint NOT NULL,
  `cultural_tag` varchar(255) DEFAULT NULL,
  KEY `fk_loc_cult_tag_on_location` (`location_id`),
  CONSTRAINT `fk_loc_cult_tag_on_location` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_cultural_tag`
--

LOCK TABLES `location_cultural_tag` WRITE;
/*!40000 ALTER TABLE `location_cultural_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `location_cultural_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `match_fulfillment`
--

DROP TABLE IF EXISTS `match_fulfillment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `match_fulfillment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `segment_id` bigint NOT NULL,
  `league_id` bigint NOT NULL,
  `status` varchar(50) NOT NULL,
  `winner_id` bigint DEFAULT NULL,
  `submitted_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_fulfillment_segment` (`segment_id`),
  KEY `fk_fulfillment_league` (`league_id`),
  KEY `fk_fulfillment_winner` (`winner_id`),
  KEY `fk_fulfillment_submitter` (`submitted_by_id`),
  CONSTRAINT `fk_fulfillment_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`),
  CONSTRAINT `fk_fulfillment_segment` FOREIGN KEY (`segment_id`) REFERENCES `segment` (`segment_id`),
  CONSTRAINT `fk_fulfillment_submitter` FOREIGN KEY (`submitted_by_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_fulfillment_winner` FOREIGN KEY (`winner_id`) REFERENCES `wrestler` (`wrestler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `match_fulfillment`
--

LOCK TABLES `match_fulfillment` WRITE;
/*!40000 ALTER TABLE `match_fulfillment` DISABLE KEYS */;
/*!40000 ALTER TABLE `match_fulfillment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `milestone_status_rewards`
--

DROP TABLE IF EXISTS `milestone_status_rewards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milestone_status_rewards` (
  `milestone_id` bigint NOT NULL,
  `status_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`milestone_id`,`status_key`),
  CONSTRAINT `fk_milestone_status_milestone` FOREIGN KEY (`milestone_id`) REFERENCES `storyline_milestone` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `milestone_status_rewards`
--

LOCK TABLES `milestone_status_rewards` WRITE;
/*!40000 ALTER TABLE `milestone_status_rewards` DISABLE KEYS */;
/*!40000 ALTER TABLE `milestone_status_rewards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `multi_wrestler_feud`
--

DROP TABLE IF EXISTS `multi_wrestler_feud`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `multi_wrestler_feud` (
  `multi_wrestler_feud_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `heat` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `started_date` timestamp NOT NULL,
  `ended_date` timestamp NULL DEFAULT NULL,
  `storyline_notes` longtext,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`multi_wrestler_feud_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `multi_wrestler_feud`
--

LOCK TABLES `multi_wrestler_feud` WRITE;
/*!40000 ALTER TABLE `multi_wrestler_feud` DISABLE KEYS */;
/*!40000 ALTER TABLE `multi_wrestler_feud` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `news_item`
--

DROP TABLE IF EXISTS `news_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `news_item` (
  `news_item_id` bigint NOT NULL AUTO_INCREMENT,
  `headline` varchar(255) NOT NULL,
  `content` varchar(2000) NOT NULL,
  `publish_date` timestamp NOT NULL,
  `category` varchar(50) NOT NULL,
  `is_rumor` tinyint(1) NOT NULL DEFAULT '0',
  `importance` int NOT NULL DEFAULT '3',
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`news_item_id`),
  UNIQUE KEY `external_id` (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `news_item`
--

LOCK TABLES `news_item` WRITE;
/*!40000 ALTER TABLE `news_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `news_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `npc`
--

DROP TABLE IF EXISTS `npc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `npc` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `npc_type` varchar(255) NOT NULL,
  `description` longtext,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `alignment` varchar(255) DEFAULT NULL,
  `attributes` text,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `expansion_code` varchar(255) NOT NULL DEFAULT 'BASE_GAME',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `npc`
--

LOCK TABLES `npc` WRITE;
/*!40000 ALTER TABLE `npc` DISABLE KEYS */;
/*!40000 ALTER TABLE `npc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `outcome_matrix`
--

DROP TABLE IF EXISTS `outcome_matrix`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outcome_matrix` (
  `outcome_matrix_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `category` varchar(50) NOT NULL,
  PRIMARY KEY (`outcome_matrix_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `outcome_matrix`
--

LOCK TABLES `outcome_matrix` WRITE;
/*!40000 ALTER TABLE `outcome_matrix` DISABLE KEYS */;
/*!40000 ALTER TABLE `outcome_matrix` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `outcome_matrix_entry`
--

DROP TABLE IF EXISTS `outcome_matrix_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outcome_matrix_entry` (
  `outcome_matrix_entry_id` bigint NOT NULL AUTO_INCREMENT,
  `outcome_matrix_id` bigint NOT NULL,
  `dice_roll` int NOT NULL,
  `template_text` longtext NOT NULL,
  `heat_delta` int DEFAULT NULL,
  `fan_delta` bigint DEFAULT NULL,
  `tv_grade_delta` int DEFAULT NULL,
  `grudge_grade_delta` int DEFAULT NULL,
  `injury_caused` tinyint(1) NOT NULL DEFAULT '0',
  `redirect_matrix_id` bigint DEFAULT NULL,
  PRIMARY KEY (`outcome_matrix_entry_id`),
  UNIQUE KEY `uq_entry_roll` (`outcome_matrix_id`,`dice_roll`),
  KEY `redirect_matrix_id` (`redirect_matrix_id`),
  CONSTRAINT `outcome_matrix_entry_ibfk_1` FOREIGN KEY (`outcome_matrix_id`) REFERENCES `outcome_matrix` (`outcome_matrix_id`) ON DELETE CASCADE,
  CONSTRAINT `outcome_matrix_entry_ibfk_2` FOREIGN KEY (`redirect_matrix_id`) REFERENCES `outcome_matrix` (`outcome_matrix_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `outcome_matrix_entry`
--

LOCK TABLES `outcome_matrix_entry` WRITE;
/*!40000 ALTER TABLE `outcome_matrix_entry` DISABLE KEYS */;
/*!40000 ALTER TABLE `outcome_matrix_entry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_token`
--

DROP TABLE IF EXISTS `password_reset_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_token` (
  `id` bigint NOT NULL,
  `token` varchar(255) DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `expiry_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_password_reset_token_account` (`account_id`),
  CONSTRAINT `fk_password_reset_token_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_token`
--

LOCK TABLES `password_reset_token` WRITE;
/*!40000 ALTER TABLE `password_reset_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ringside_action`
--

DROP TABLE IF EXISTS `ringside_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ringside_action` (
  `ringside_action_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `impact` int NOT NULL,
  `risk` int NOT NULL,
  `alignment` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ringside_action_type_id` bigint NOT NULL,
  `external_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_sync` datetime(6) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ringside_action_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uk_ringside_action_name` (`name`),
  UNIQUE KEY `uk_ringside_action_external_id` (`external_id`),
  KEY `fk_ringside_action_type` (`ringside_action_type_id`),
  CONSTRAINT `fk_ringside_action_type` FOREIGN KEY (`ringside_action_type_id`) REFERENCES `ringside_action_type` (`ringside_action_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ringside_action`
--

LOCK TABLES `ringside_action` WRITE;
/*!40000 ALTER TABLE `ringside_action` DISABLE KEYS */;
/*!40000 ALTER TABLE `ringside_action` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ringside_action_type`
--

DROP TABLE IF EXISTS `ringside_action_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ringside_action_type` (
  `ringside_action_type_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `increases_awareness` tinyint(1) NOT NULL DEFAULT '1',
  `can_cause_dq` tinyint(1) NOT NULL DEFAULT '1',
  `base_risk_multiplier` double NOT NULL DEFAULT '1',
  `external_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_sync` datetime(6) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ringside_action_type_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uk_ringside_action_type_name` (`name`),
  UNIQUE KEY `uk_ringside_action_type_external_id` (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ringside_action_type`
--

LOCK TABLES `ringside_action_type` WRITE;
/*!40000 ALTER TABLE `ringside_action_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `ringside_action_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rivalry`
--

DROP TABLE IF EXISTS `rivalry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rivalry` (
  `rivalry_id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler1_id` bigint NOT NULL,
  `wrestler2_id` bigint NOT NULL,
  `heat` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `started_date` timestamp NOT NULL,
  `ended_date` timestamp NULL DEFAULT NULL,
  `storyline_notes` longtext,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `priority` int DEFAULT '0',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `league_id` bigint DEFAULT NULL,
  `universe_id` bigint NOT NULL,
  PRIMARY KEY (`rivalry_id`),
  KEY `wrestler2_id` (`wrestler2_id`),
  KEY `idx_rivalry_wrestlers` (`wrestler1_id`,`wrestler2_id`),
  KEY `fk_rivalry_league` (`league_id`),
  KEY `fk_rivalry_universe` (`universe_id`),
  CONSTRAINT `fk_rivalry_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`),
  CONSTRAINT `fk_rivalry_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`),
  CONSTRAINT `rivalry_ibfk_1` FOREIGN KEY (`wrestler1_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE,
  CONSTRAINT `rivalry_ibfk_2` FOREIGN KEY (`wrestler2_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE,
  CONSTRAINT `rivalry_chk_1` CHECK ((`wrestler1_id` <> `wrestler2_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rivalry`
--

LOCK TABLES `rivalry` WRITE;
/*!40000 ALTER TABLE `rivalry` DISABLE KEYS */;
/*!40000 ALTER TABLE `rivalry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_role_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'ADMIN','Full system access - can manage accounts and all content'),(2,'BOOKER','Can manage shows, wrestlers, and content but not system administration'),(3,'PLAYER','Can manage own content and view most data'),(4,'VIEWER','Read-only access to content');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `season`
--

DROP TABLE IF EXISTS `season`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `season` (
  `season_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `creation_date` timestamp NOT NULL,
  `notion_id` varchar(255) DEFAULT NULL,
  `shows_per_ppv` int NOT NULL DEFAULT '5',
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `budget` decimal(19,2) DEFAULT '0.00',
  `duration_weeks` int DEFAULT NULL,
  PRIMARY KEY (`season_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `season`
--

LOCK TABLES `season` WRITE;
/*!40000 ALTER TABLE `season` DISABLE KEYS */;
/*!40000 ALTER TABLE `season` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `segment`
--

DROP TABLE IF EXISTS `segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `segment` (
  `segment_id` bigint NOT NULL AUTO_INCREMENT,
  `show_id` bigint NOT NULL,
  `segment_type_id` bigint NOT NULL,
  `winner_id` bigint DEFAULT NULL,
  `segment_date` timestamp NOT NULL,
  `duration_minutes` int DEFAULT NULL,
  `segment_rating` int DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `adjudication_status` varchar(255) NOT NULL DEFAULT 'ADJUDICATED',
  `segment_order` int NOT NULL DEFAULT '0',
  `is_main_event` tinyint(1) NOT NULL DEFAULT '0',
  `narration` longtext,
  `summary` longtext,
  `is_title_segment` tinyint(1) NOT NULL DEFAULT '0',
  `is_npc_generated` tinyint(1) NOT NULL DEFAULT '0',
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `referee_id` bigint DEFAULT NULL,
  `referee_awareness_level` int NOT NULL DEFAULT '0',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `crowd_noise_level` int NOT NULL DEFAULT '0',
  `notes` longtext,
  `rivalry_id` bigint DEFAULT NULL,
  PRIMARY KEY (`segment_id`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `show_id` (`show_id`),
  KEY `segment_type_id` (`segment_type_id`),
  KEY `winner_id` (`winner_id`),
  KEY `fk_segment_referee` (`referee_id`),
  CONSTRAINT `fk_segment_referee` FOREIGN KEY (`referee_id`) REFERENCES `npc` (`id`),
  CONSTRAINT `segment_ibfk_1` FOREIGN KEY (`show_id`) REFERENCES `wrestling_show` (`show_id`) ON DELETE CASCADE,
  CONSTRAINT `segment_ibfk_2` FOREIGN KEY (`segment_type_id`) REFERENCES `segment_type` (`segment_type_id`) ON DELETE RESTRICT,
  CONSTRAINT `segment_ibfk_3` FOREIGN KEY (`winner_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `segment`
--

LOCK TABLES `segment` WRITE;
/*!40000 ALTER TABLE `segment` DISABLE KEYS */;
/*!40000 ALTER TABLE `segment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `segment_participant`
--

DROP TABLE IF EXISTS `segment_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `segment_participant` (
  `segment_participant_id` bigint NOT NULL AUTO_INCREMENT,
  `segment_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  `is_winner` tinyint(1) NOT NULL DEFAULT '0',
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `team_number` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`segment_participant_id`),
  UNIQUE KEY `segment_id` (`segment_id`,`wrestler_id`),
  KEY `wrestler_id` (`wrestler_id`),
  CONSTRAINT `segment_participant_ibfk_1` FOREIGN KEY (`segment_id`) REFERENCES `segment` (`segment_id`) ON DELETE CASCADE,
  CONSTRAINT `segment_participant_ibfk_2` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `segment_participant`
--

LOCK TABLES `segment_participant` WRITE;
/*!40000 ALTER TABLE `segment_participant` DISABLE KEYS */;
/*!40000 ALTER TABLE `segment_participant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `segment_rule`
--

DROP TABLE IF EXISTS `segment_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `segment_rule` (
  `segment_rule_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `requires_high_heat` tinyint(1) NOT NULL DEFAULT '0',
  `bump_addition` varchar(255) NOT NULL DEFAULT 'NONE',
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `no_dq` tinyint(1) NOT NULL DEFAULT '0',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`segment_rule_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `segment_rule`
--

LOCK TABLES `segment_rule` WRITE;
/*!40000 ALTER TABLE `segment_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `segment_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `segment_segment_rule`
--

DROP TABLE IF EXISTS `segment_segment_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `segment_segment_rule` (
  `segment_id` bigint NOT NULL,
  `segment_rule_id` bigint NOT NULL,
  PRIMARY KEY (`segment_id`,`segment_rule_id`),
  KEY `segment_rule_id` (`segment_rule_id`),
  CONSTRAINT `segment_segment_rule_ibfk_1` FOREIGN KEY (`segment_id`) REFERENCES `segment` (`segment_id`) ON DELETE CASCADE,
  CONSTRAINT `segment_segment_rule_ibfk_2` FOREIGN KEY (`segment_rule_id`) REFERENCES `segment_rule` (`segment_rule_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `segment_segment_rule`
--

LOCK TABLES `segment_segment_rule` WRITE;
/*!40000 ALTER TABLE `segment_segment_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `segment_segment_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `segment_title`
--

DROP TABLE IF EXISTS `segment_title`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `segment_title` (
  `segment_id` bigint NOT NULL,
  `title_id` bigint NOT NULL,
  PRIMARY KEY (`segment_id`,`title_id`),
  KEY `title_id` (`title_id`),
  CONSTRAINT `segment_title_ibfk_1` FOREIGN KEY (`segment_id`) REFERENCES `segment` (`segment_id`) ON DELETE CASCADE,
  CONSTRAINT `segment_title_ibfk_2` FOREIGN KEY (`title_id`) REFERENCES `title` (`title_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `segment_title`
--

LOCK TABLES `segment_title` WRITE;
/*!40000 ALTER TABLE `segment_title` DISABLE KEYS */;
/*!40000 ALTER TABLE `segment_title` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `segment_type`
--

DROP TABLE IF EXISTS `segment_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `segment_type` (
  `segment_type_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`segment_type_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `segment_type`
--

LOCK TABLES `segment_type` WRITE;
/*!40000 ALTER TABLE `segment_type` DISABLE KEYS */;
INSERT INTO `segment_type` VALUES (1,'Match',NULL,'2026-06-09 01:51:13',NULL,NULL,'2026-06-09 01:51:13');
/*!40000 ALTER TABLE `segment_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `show_template`
--

DROP TABLE IF EXISTS `show_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `show_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `show_type_id` bigint NOT NULL,
  `notion_url` varchar(500) DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `creation_date` timestamp NOT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `expected_matches` int DEFAULT NULL,
  `expected_promos` int DEFAULT NULL,
  `duration_days` int DEFAULT '1',
  `recurrence_type` varchar(20) DEFAULT 'NONE',
  `recurrence_day_of_week` varchar(20) DEFAULT NULL,
  `recurrence_day_of_month` int DEFAULT NULL,
  `recurrence_week_of_month` int DEFAULT NULL,
  `recurrence_month` varchar(20) DEFAULT NULL,
  `commentary_team_id` bigint DEFAULT NULL,
  `gender_constraint` varchar(20) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `show_type_id` (`show_type_id`),
  KEY `fk_show_template_commentary_team` (`commentary_team_id`),
  CONSTRAINT `fk_show_template_commentary_team` FOREIGN KEY (`commentary_team_id`) REFERENCES `commentary_team` (`team_id`) ON DELETE SET NULL,
  CONSTRAINT `show_template_ibfk_1` FOREIGN KEY (`show_type_id`) REFERENCES `show_type` (`show_type_id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `show_template`
--

LOCK TABLES `show_template` WRITE;
/*!40000 ALTER TABLE `show_template` DISABLE KEYS */;
INSERT INTO `show_template` VALUES (1,'Reference Template',NULL,1,NULL,NULL,'2026-06-09 01:51:13',NULL,NULL,NULL,NULL,1,'NONE',NULL,NULL,NULL,NULL,NULL,NULL,'2026-06-09 01:51:13');
/*!40000 ALTER TABLE `show_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `show_type`
--

DROP TABLE IF EXISTS `show_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `show_type` (
  `show_type_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `is_ppv` tinyint(1) NOT NULL DEFAULT '0',
  `expected_matches` int NOT NULL DEFAULT '0',
  `expected_promos` int NOT NULL DEFAULT '0',
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`show_type_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `show_type`
--

LOCK TABLES `show_type` WRITE;
/*!40000 ALTER TABLE `show_type` DISABLE KEYS */;
INSERT INTO `show_type` VALUES (1,'Weekly Show',NULL,0,0,0,'2026-06-09 01:51:13',NULL,NULL,'2026-06-09 01:51:13');
/*!40000 ALTER TABLE `show_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `status_card`
--

DROP TABLE IF EXISTS `status_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `status_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `level_1_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `level_2_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `positive` tinyint(1) NOT NULL DEFAULT '1',
  `level_1_effect` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `level_2_effect` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flip_up_condition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flip_down_condition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discard_condition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `status_key` (`status_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `status_card`
--

LOCK TABLES `status_card` WRITE;
/*!40000 ALTER TABLE `status_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `status_card` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storyline_milestone`
--

DROP TABLE IF EXISTS `storyline_milestone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `storyline_milestone` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `storyline_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `narrative_goal` varchar(2000) NOT NULL,
  `status` varchar(20) NOT NULL,
  `display_order` int NOT NULL,
  `next_on_success_id` bigint DEFAULT NULL,
  `next_on_failure_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_milestone_storyline` (`storyline_id`),
  KEY `fk_milestone_next_success` (`next_on_success_id`),
  KEY `fk_milestone_next_failure` (`next_on_failure_id`),
  CONSTRAINT `fk_milestone_next_failure` FOREIGN KEY (`next_on_failure_id`) REFERENCES `storyline_milestone` (`id`),
  CONSTRAINT `fk_milestone_next_success` FOREIGN KEY (`next_on_success_id`) REFERENCES `storyline_milestone` (`id`),
  CONSTRAINT `fk_milestone_storyline` FOREIGN KEY (`storyline_id`) REFERENCES `campaign_storyline` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storyline_milestone`
--

LOCK TABLES `storyline_milestone` WRITE;
/*!40000 ALTER TABLE `storyline_milestone` DISABLE KEYS */;
/*!40000 ALTER TABLE `storyline_milestone` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `team`
--

DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team` (
  `team_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `wrestler1_id` bigint NOT NULL,
  `wrestler2_id` bigint NOT NULL,
  `faction_id` bigint DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `formed_date` timestamp NOT NULL,
  `disbanded_date` timestamp NULL DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `theme_song` varchar(255) DEFAULT NULL,
  `artist` varchar(255) DEFAULT NULL,
  `team_finisher` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `image_url` varchar(512) DEFAULT NULL,
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `name` (`name`),
  KEY `wrestler1_id` (`wrestler1_id`),
  KEY `wrestler2_id` (`wrestler2_id`),
  KEY `faction_id` (`faction_id`),
  KEY `manager_id` (`manager_id`),
  KEY `fk_team_universe` (`universe_id`),
  CONSTRAINT `fk_team_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`),
  CONSTRAINT `team_ibfk_1` FOREIGN KEY (`wrestler1_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE,
  CONSTRAINT `team_ibfk_2` FOREIGN KEY (`wrestler2_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE,
  CONSTRAINT `team_ibfk_3` FOREIGN KEY (`faction_id`) REFERENCES `faction` (`faction_id`) ON DELETE SET NULL,
  CONSTRAINT `team_ibfk_4` FOREIGN KEY (`manager_id`) REFERENCES `npc` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `team`
--

LOCK TABLES `team` WRITE;
/*!40000 ALTER TABLE `team` DISABLE KEYS */;
/*!40000 ALTER TABLE `team` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tier_boundary`
--

DROP TABLE IF EXISTS `tier_boundary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tier_boundary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tier` varchar(255) NOT NULL,
  `gender` varchar(255) NOT NULL,
  `min_fans` bigint NOT NULL,
  `max_fans` bigint NOT NULL,
  `challenge_cost` bigint NOT NULL,
  `contender_entry_fee` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_tier_boundary_tier_gender` (`tier`,`gender`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tier_boundary`
--

LOCK TABLES `tier_boundary` WRITE;
/*!40000 ALTER TABLE `tier_boundary` DISABLE KEYS */;
/*!40000 ALTER TABLE `tier_boundary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `title`
--

DROP TABLE IF EXISTS `title`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `title` (
  `title_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `tier` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `championship_type` varchar(255) NOT NULL DEFAULT 'SINGLE',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `include_in_rankings` tinyint(1) NOT NULL DEFAULT '1',
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `image_url` varchar(512) DEFAULT NULL,
  `effect_script` varchar(255) DEFAULT NULL,
  `universe_id` bigint DEFAULT NULL,
  `defense_frequency_type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`title_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `fk_title_universe` (`universe_id`),
  CONSTRAINT `fk_title_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `title`
--

LOCK TABLES `title` WRITE;
/*!40000 ALTER TABLE `title` DISABLE KEYS */;
INSERT INTO `title` VALUES (1,'Reference Championship',NULL,NULL,NULL,'SINGLE',1,1,'2026-06-09 01:51:13',NULL,NULL,'2026-06-09 01:51:13',NULL,NULL,1,NULL);
/*!40000 ALTER TABLE `title` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `title_champion`
--

DROP TABLE IF EXISTS `title_champion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `title_champion` (
  `title_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  PRIMARY KEY (`title_id`,`wrestler_id`),
  KEY `wrestler_id` (`wrestler_id`),
  CONSTRAINT `title_champion_ibfk_1` FOREIGN KEY (`title_id`) REFERENCES `title` (`title_id`) ON DELETE CASCADE,
  CONSTRAINT `title_champion_ibfk_2` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `title_champion`
--

LOCK TABLES `title_champion` WRITE;
/*!40000 ALTER TABLE `title_champion` DISABLE KEYS */;
/*!40000 ALTER TABLE `title_champion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `title_contender`
--

DROP TABLE IF EXISTS `title_contender`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `title_contender` (
  `title_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  PRIMARY KEY (`title_id`,`wrestler_id`),
  KEY `wrestler_id` (`wrestler_id`),
  CONSTRAINT `title_contender_ibfk_1` FOREIGN KEY (`title_id`) REFERENCES `title` (`title_id`) ON DELETE CASCADE,
  CONSTRAINT `title_contender_ibfk_2` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `title_contender`
--

LOCK TABLES `title_contender` WRITE;
/*!40000 ALTER TABLE `title_contender` DISABLE KEYS */;
/*!40000 ALTER TABLE `title_contender` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `title_reign`
--

DROP TABLE IF EXISTS `title_reign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `title_reign` (
  `title_reign_id` bigint NOT NULL AUTO_INCREMENT,
  `external_id` varchar(255) DEFAULT NULL,
  `title_id` bigint NOT NULL,
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NULL DEFAULT NULL,
  `reign_number` int NOT NULL DEFAULT '1',
  `notes` longtext,
  `creation_date` timestamp NOT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `won_at_segment_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`title_reign_id`),
  KEY `title_id` (`title_id`),
  KEY `fk_title_reign_won_at_segment` (`won_at_segment_id`),
  CONSTRAINT `fk_title_reign_won_at_segment` FOREIGN KEY (`won_at_segment_id`) REFERENCES `segment` (`segment_id`),
  CONSTRAINT `title_reign_ibfk_1` FOREIGN KEY (`title_id`) REFERENCES `title` (`title_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `title_reign`
--

LOCK TABLES `title_reign` WRITE;
/*!40000 ALTER TABLE `title_reign` DISABLE KEYS */;
/*!40000 ALTER TABLE `title_reign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `title_reign_champion`
--

DROP TABLE IF EXISTS `title_reign_champion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `title_reign_champion` (
  `title_reign_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  PRIMARY KEY (`title_reign_id`,`wrestler_id`),
  KEY `wrestler_id` (`wrestler_id`),
  CONSTRAINT `title_reign_champion_ibfk_1` FOREIGN KEY (`title_reign_id`) REFERENCES `title_reign` (`title_reign_id`) ON DELETE CASCADE,
  CONSTRAINT `title_reign_champion_ibfk_2` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `title_reign_champion`
--

LOCK TABLES `title_reign_champion` WRITE;
/*!40000 ALTER TABLE `title_reign_champion` DISABLE KEYS */;
/*!40000 ALTER TABLE `title_reign_champion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `universe`
--

DROP TABLE IF EXISTS `universe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `type` varchar(50) NOT NULL,
  `creation_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `universe`
--

LOCK TABLES `universe` WRITE;
/*!40000 ALTER TABLE `universe` DISABLE KEYS */;
INSERT INTO `universe` VALUES (1,'Default Universe','GLOBAL','2026-06-09 01:51:11');
/*!40000 ALTER TABLE `universe` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `universe_expansion_settings`
--

DROP TABLE IF EXISTS `universe_expansion_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universe_expansion_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `universe_id` bigint NOT NULL,
  `expansion_code` varchar(255) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_universe_expansion` (`universe_id`,`expansion_code`),
  CONSTRAINT `fk_ues_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `universe_expansion_settings`
--

LOCK TABLES `universe_expansion_settings` WRITE;
/*!40000 ALTER TABLE `universe_expansion_settings` DISABLE KEYS */;
/*!40000 ALTER TABLE `universe_expansion_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `universe_invite`
--

DROP TABLE IF EXISTS `universe_invite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universe_invite` (
  `id` varchar(36) NOT NULL,
  `universe_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NULL DEFAULT NULL,
  `revoked_at` timestamp NULL DEFAULT NULL,
  `max_uses` int DEFAULT NULL,
  `use_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_invite_created_by` (`created_by`),
  KEY `idx_universe_invite_universe` (`universe_id`),
  CONSTRAINT `fk_invite_created_by` FOREIGN KEY (`created_by`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_invite_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `universe_invite`
--

LOCK TABLES `universe_invite` WRITE;
/*!40000 ALTER TABLE `universe_invite` DISABLE KEYS */;
/*!40000 ALTER TABLE `universe_invite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `universe_join_request`
--

DROP TABLE IF EXISTS `universe_join_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universe_join_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `universe_id` bigint NOT NULL,
  `invite_id` varchar(36) DEFAULT NULL,
  `account_id` bigint DEFAULT NULL,
  `requester_name` varchar(255) NOT NULL,
  `requester_email` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `requested_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_at` timestamp NULL DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_request_invite` (`invite_id`),
  KEY `fk_request_resolved_by` (`resolved_by`),
  KEY `idx_universe_join_request_universe` (`universe_id`),
  KEY `idx_universe_join_request_status` (`status`),
  KEY `idx_universe_join_request_account` (`account_id`),
  CONSTRAINT `fk_request_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_request_invite` FOREIGN KEY (`invite_id`) REFERENCES `universe_invite` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_request_resolved_by` FOREIGN KEY (`resolved_by`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_request_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `universe_join_request`
--

LOCK TABLES `universe_join_request` WRITE;
/*!40000 ALTER TABLE `universe_join_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `universe_join_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `universe_members`
--

DROP TABLE IF EXISTS `universe_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universe_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `universe_id` bigint NOT NULL,
  `account_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'MEMBER',
  `joined_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_universe_member` (`universe_id`,`account_id`),
  KEY `fk_um_account` (`account_id`),
  CONSTRAINT `fk_um_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_um_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `universe_members`
--

LOCK TABLES `universe_members` WRITE;
/*!40000 ALTER TABLE `universe_members` DISABLE KEYS */;
INSERT INTO `universe_members` VALUES (1,1,1,'MEMBER','2026-06-09 01:51:11'),(2,1,2,'MEMBER','2026-06-09 01:51:11'),(3,1,3,'MEMBER','2026-06-09 01:51:11'),(4,1,4,'MEMBER','2026-06-09 01:51:11');
/*!40000 ALTER TABLE `universe_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `universe_wrestler_exclusions`
--

DROP TABLE IF EXISTS `universe_wrestler_exclusions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universe_wrestler_exclusions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `universe_id` bigint NOT NULL,
  `wrestler_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_universe_wrestler` (`universe_id`,`wrestler_id`),
  KEY `fk_uwe_wrestler` (`wrestler_id`),
  CONSTRAINT `fk_uwe_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`),
  CONSTRAINT `fk_uwe_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `universe_wrestler_exclusions`
--

LOCK TABLES `universe_wrestler_exclusions` WRITE;
/*!40000 ALTER TABLE `universe_wrestler_exclusions` DISABLE KEYS */;
/*!40000 ALTER TABLE `universe_wrestler_exclusions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler`
--

DROP TABLE IF EXISTS `wrestler`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler` (
  `wrestler_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `starting_stamina` int NOT NULL,
  `low_stamina` int NOT NULL,
  `starting_health` int NOT NULL,
  `low_health` int NOT NULL,
  `deck_size` int NOT NULL,
  `creation_date` timestamp NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `is_player` tinyint(1) NOT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `description` varchar(4000) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `account_id` bigint DEFAULT NULL,
  `drive` int NOT NULL DEFAULT '1',
  `resilience` int NOT NULL DEFAULT '1',
  `charisma` int NOT NULL DEFAULT '1',
  `brawl` int NOT NULL DEFAULT '1',
  `heritage_tag` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `expansion_code` varchar(255) NOT NULL DEFAULT 'BASE_GAME',
  PRIMARY KEY (`wrestler_id`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `account_id` (`account_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler`
--

LOCK TABLES `wrestler` WRITE;
/*!40000 ALTER TABLE `wrestler` DISABLE KEYS */;
INSERT INTO `wrestler` VALUES (1,'Reference Wrestler',12,2,12,2,12,'2026-06-09 01:51:13',NULL,0,'MALE',NULL,NULL,NULL,1,NULL,1,1,1,1,NULL,'2026-06-09 01:51:13','BASE_GAME'),(2,'Reference Wrestler 2',14,3,14,3,14,'2026-06-09 01:51:13',NULL,0,'FEMALE',NULL,NULL,NULL,1,NULL,1,1,1,1,NULL,'2026-06-09 01:51:13','BASE_GAME');
/*!40000 ALTER TABLE `wrestler` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler_alignment`
--

DROP TABLE IF EXISTS `wrestler_alignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler_alignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `campaign_id` bigint DEFAULT NULL,
  `alignment_type` varchar(50) NOT NULL,
  `level` int NOT NULL DEFAULT '0',
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_wa_campaign` (`campaign_id`),
  KEY `idx_wrestler_alignment_wrestler` (`wrestler_id`),
  KEY `fk_wrestler_alignment_universe` (`universe_id`),
  KEY `idx_wrestler_alignment_universe` (`wrestler_id`,`universe_id`),
  CONSTRAINT `fk_wa_campaign` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`),
  CONSTRAINT `fk_wa_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`),
  CONSTRAINT `fk_wrestler_alignment_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler_alignment`
--

LOCK TABLES `wrestler_alignment` WRITE;
/*!40000 ALTER TABLE `wrestler_alignment` DISABLE KEYS */;
/*!40000 ALTER TABLE `wrestler_alignment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler_contract`
--

DROP TABLE IF EXISTS `wrestler_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler_contract` (
  `contract_id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `league_id` bigint NOT NULL,
  `salary_per_show` decimal(19,2) NOT NULL,
  `start_date` datetime NOT NULL,
  `expiry_date` datetime DEFAULT NULL,
  `duration_weeks` int NOT NULL,
  `is_initial_draft` tinyint(1) NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `creation_date` datetime NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`contract_id`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `idx_contract_league` (`league_id`),
  KEY `idx_contract_wrestler` (`wrestler_id`),
  CONSTRAINT `fk_contract_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_contract_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler_contract`
--

LOCK TABLES `wrestler_contract` WRITE;
/*!40000 ALTER TABLE `wrestler_contract` DISABLE KEYS */;
/*!40000 ALTER TABLE `wrestler_contract` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler_relationship`
--

DROP TABLE IF EXISTS `wrestler_relationship`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler_relationship` (
  `relationship_id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler1_id` bigint NOT NULL,
  `wrestler2_id` bigint NOT NULL,
  `relationship_type` varchar(255) NOT NULL,
  `level` int NOT NULL DEFAULT '50',
  `is_storyline` tinyint(1) NOT NULL DEFAULT '0',
  `started_date` datetime NOT NULL,
  `creation_date` datetime NOT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_sync` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`relationship_id`),
  UNIQUE KEY `wrestler1_id` (`wrestler1_id`,`wrestler2_id`,`relationship_type`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `fk_rel_wrestler2` (`wrestler2_id`),
  KEY `idx_rel_wrestlers` (`wrestler1_id`,`wrestler2_id`),
  CONSTRAINT `fk_rel_wrestler1` FOREIGN KEY (`wrestler1_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rel_wrestler2` FOREIGN KEY (`wrestler2_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler_relationship`
--

LOCK TABLES `wrestler_relationship` WRITE;
/*!40000 ALTER TABLE `wrestler_relationship` DISABLE KEYS */;
/*!40000 ALTER TABLE `wrestler_relationship` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler_season_snapshot`
--

DROP TABLE IF EXISTS `wrestler_season_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler_season_snapshot` (
  `snapshot_id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `season_id` bigint NOT NULL,
  `starting_fans` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL,
  PRIMARY KEY (`snapshot_id`),
  UNIQUE KEY `uq_wrestler_season` (`wrestler_id`,`season_id`),
  KEY `season_id` (`season_id`),
  CONSTRAINT `wrestler_season_snapshot_ibfk_1` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`) ON DELETE CASCADE,
  CONSTRAINT `wrestler_season_snapshot_ibfk_2` FOREIGN KEY (`season_id`) REFERENCES `season` (`season_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler_season_snapshot`
--

LOCK TABLES `wrestler_season_snapshot` WRITE;
/*!40000 ALTER TABLE `wrestler_season_snapshot` DISABLE KEYS */;
/*!40000 ALTER TABLE `wrestler_season_snapshot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler_state`
--

DROP TABLE IF EXISTS `wrestler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler_state` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `universe_id` bigint NOT NULL,
  `fans` bigint DEFAULT '0',
  `tier` varchar(255) NOT NULL,
  `bumps` int DEFAULT '0',
  `current_health` int DEFAULT NULL,
  `physical_condition` int DEFAULT '100',
  `morale` int DEFAULT '100',
  `management_stamina` int DEFAULT '100',
  `faction_id` bigint DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wrestler_universe` (`wrestler_id`,`universe_id`),
  KEY `fk_ws_universe` (`universe_id`),
  KEY `fk_ws_faction` (`faction_id`),
  KEY `fk_ws_manager` (`manager_id`),
  CONSTRAINT `fk_ws_faction` FOREIGN KEY (`faction_id`) REFERENCES `faction` (`faction_id`),
  CONSTRAINT `fk_ws_manager` FOREIGN KEY (`manager_id`) REFERENCES `npc` (`id`),
  CONSTRAINT `fk_ws_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`),
  CONSTRAINT `fk_ws_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler_state`
--

LOCK TABLES `wrestler_state` WRITE;
/*!40000 ALTER TABLE `wrestler_state` DISABLE KEYS */;
INSERT INTO `wrestler_state` VALUES (1,1,1,1000,'ROOKIE',0,12,100,100,100,NULL,NULL,'2026-06-09 01:51:13'),(2,2,1,500,'VETERAN',2,14,100,100,100,NULL,NULL,'2026-06-09 01:51:13');
/*!40000 ALTER TABLE `wrestler_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler_status`
--

DROP TABLE IF EXISTS `wrestler_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `status_card_id` bigint NOT NULL,
  `level` int NOT NULL DEFAULT '1',
  `creation_date` datetime(6) NOT NULL,
  `last_updated` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_wrestler_status_card` (`status_card_id`),
  KEY `idx_wrestler_status_wrestler` (`wrestler_id`),
  CONSTRAINT `fk_wrestler_status_card` FOREIGN KEY (`status_card_id`) REFERENCES `status_card` (`id`),
  CONSTRAINT `fk_wrestler_status_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler_status`
--

LOCK TABLES `wrestler_status` WRITE;
/*!40000 ALTER TABLE `wrestler_status` DISABLE KEYS */;
/*!40000 ALTER TABLE `wrestler_status` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestler_status_history`
--

DROP TABLE IF EXISTS `wrestler_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestler_status_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wrestler_id` bigint NOT NULL,
  `status_card_id` bigint NOT NULL,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_level` int DEFAULT NULL,
  `new_level` int DEFAULT NULL,
  `creation_date` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_wrestler_status_history_card` (`status_card_id`),
  KEY `idx_wrestler_status_history_wrestler` (`wrestler_id`),
  CONSTRAINT `fk_wrestler_status_history_card` FOREIGN KEY (`status_card_id`) REFERENCES `status_card` (`id`),
  CONSTRAINT `fk_wrestler_status_history_wrestler` FOREIGN KEY (`wrestler_id`) REFERENCES `wrestler` (`wrestler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestler_status_history`
--

LOCK TABLES `wrestler_status_history` WRITE;
/*!40000 ALTER TABLE `wrestler_status_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `wrestler_status_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wrestling_show`
--

DROP TABLE IF EXISTS `wrestling_show`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wrestling_show` (
  `show_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext,
  `show_date` timestamp NULL DEFAULT NULL,
  `show_type_id` bigint NOT NULL,
  `season_id` bigint DEFAULT NULL,
  `template_id` bigint DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `creation_date` timestamp NOT NULL,
  `last_sync` timestamp NULL DEFAULT NULL,
  `league_id` bigint DEFAULT NULL,
  `commentary_team_id` bigint DEFAULT NULL,
  `arena_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `attendance` int DEFAULT '0',
  `gate_revenue` decimal(19,2) DEFAULT '0.00',
  `universe_id` bigint DEFAULT NULL,
  PRIMARY KEY (`show_id`),
  UNIQUE KEY `external_id` (`external_id`),
  KEY `show_type_id` (`show_type_id`),
  KEY `season_id` (`season_id`),
  KEY `template_id` (`template_id`),
  KEY `fk_show_league` (`league_id`),
  KEY `fk_show_commentary_team` (`commentary_team_id`),
  KEY `fk_wrestling_show_on_arena` (`arena_id`),
  KEY `fk_wrestling_show_universe` (`universe_id`),
  CONSTRAINT `fk_show_commentary_team` FOREIGN KEY (`commentary_team_id`) REFERENCES `commentary_team` (`team_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_show_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`),
  CONSTRAINT `fk_wrestling_show_on_arena` FOREIGN KEY (`arena_id`) REFERENCES `arena` (`arena_id`),
  CONSTRAINT `fk_wrestling_show_universe` FOREIGN KEY (`universe_id`) REFERENCES `universe` (`id`),
  CONSTRAINT `wrestling_show_ibfk_1` FOREIGN KEY (`show_type_id`) REFERENCES `show_type` (`show_type_id`) ON DELETE RESTRICT,
  CONSTRAINT `wrestling_show_ibfk_2` FOREIGN KEY (`season_id`) REFERENCES `season` (`season_id`) ON DELETE SET NULL,
  CONSTRAINT `wrestling_show_ibfk_3` FOREIGN KEY (`template_id`) REFERENCES `show_template` (`template_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wrestling_show`
--

LOCK TABLES `wrestling_show` WRITE;
/*!40000 ALTER TABLE `wrestling_show` DISABLE KEYS */;
INSERT INTO `wrestling_show` VALUES (1,'Reference Show',NULL,NULL,1,NULL,NULL,NULL,'2026-06-09 01:51:13',NULL,NULL,NULL,NULL,'2026-06-09 01:51:13',0,0.00,1);
/*!40000 ALTER TABLE `wrestling_show` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'atwrpg'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09  1:51:14
