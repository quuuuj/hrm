-- MySQL dump 10.13  Distrib 8.1.0, for Linux (x86_64)
--
-- Host: localhost    Database: hrm_flowable
-- ------------------------------------------------------
-- Server version	8.1.0

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
-- Table structure for table `ACT_CMMN_CASEDEF`
--

DROP TABLE IF EXISTS `ACT_CMMN_CASEDEF`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_CASEDEF` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `VERSION_` int NOT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CASE_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`),
  KEY `ACT_IDX_CASE_DEF_DPLY` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_CASE_DEF_DPLY` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_CMMN_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_CASEDEF`
--

LOCK TABLES `ACT_CMMN_CASEDEF` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_CASEDEF` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_CASEDEF` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_DEPLOYMENT`
--

DROP TABLE IF EXISTS `ACT_CMMN_DEPLOYMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_DEPLOYMENT` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_DEPLOYMENT`
--

LOCK TABLES `ACT_CMMN_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_DEPLOYMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_DEPLOYMENT_RESOURCE`
--

DROP TABLE IF EXISTS `ACT_CMMN_DEPLOYMENT_RESOURCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_DEPLOYMENT_RESOURCE` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_CMMN_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_CMMN_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_CMMN_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_DEPLOYMENT_RESOURCE`
--

LOCK TABLES `ACT_CMMN_DEPLOYMENT_RESOURCE` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_DEPLOYMENT_RESOURCE` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_DEPLOYMENT_RESOURCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_HI_CASE_INST`
--

DROP TABLE IF EXISTS `ACT_CMMN_HI_CASE_INST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_HI_CASE_INST` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_REACTIVATION_TIME_` datetime(3) DEFAULT NULL,
  `LAST_REACTIVATION_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `END_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_CASE_INST_END` (`END_TIME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_HI_CASE_INST`
--

LOCK TABLES `ACT_CMMN_HI_CASE_INST` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_HI_CASE_INST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_HI_CASE_INST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_HI_MIL_INST`
--

DROP TABLE IF EXISTS `ACT_CMMN_HI_MIL_INST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_HI_MIL_INST` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIME_STAMP_` datetime(3) NOT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_HI_MIL_INST`
--

LOCK TABLES `ACT_CMMN_HI_MIL_INST` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_HI_MIL_INST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_HI_MIL_INST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_HI_PLAN_ITEM_INST`
--

DROP TABLE IF EXISTS `ACT_CMMN_HI_PLAN_ITEM_INST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_HI_PLAN_ITEM_INST` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STAGE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_STAGE_` tinyint DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_AVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_ENABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_DISABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_STARTED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_TIME_` datetime(3) DEFAULT NULL,
  `OCCURRED_TIME_` datetime(3) DEFAULT NULL,
  `TERMINATED_TIME_` datetime(3) DEFAULT NULL,
  `EXIT_TIME_` datetime(3) DEFAULT NULL,
  `ENDED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ENTRY_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SHOW_IN_OVERVIEW_` tinyint DEFAULT NULL,
  `EXTRA_VALUE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DERIVED_CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UNAVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PLAN_ITEM_INST_CASE` (`CASE_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_HI_PLAN_ITEM_INST`
--

LOCK TABLES `ACT_CMMN_HI_PLAN_ITEM_INST` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_HI_PLAN_ITEM_INST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_HI_PLAN_ITEM_INST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_RU_CASE_INST`
--

DROP TABLE IF EXISTS `ACT_CMMN_RU_CASE_INST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_RU_CASE_INST` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LOCK_TIME_` datetime(3) DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_COMPLETEABLE_` tinyint DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_REACTIVATION_TIME_` datetime(3) DEFAULT NULL,
  `LAST_REACTIVATION_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_CASE_INST_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_CASE_INST_PARENT` (`PARENT_ID_`),
  KEY `ACT_IDX_CASE_INST_REF_ID_` (`REFERENCE_ID_`),
  CONSTRAINT `ACT_FK_CASE_INST_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `ACT_CMMN_CASEDEF` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_RU_CASE_INST`
--

LOCK TABLES `ACT_CMMN_RU_CASE_INST` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_RU_CASE_INST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_RU_CASE_INST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_RU_MIL_INST`
--

DROP TABLE IF EXISTS `ACT_CMMN_RU_MIL_INST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_RU_MIL_INST` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIME_STAMP_` datetime(3) NOT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_MIL_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_MIL_CASE_INST` (`CASE_INST_ID_`),
  CONSTRAINT `ACT_FK_MIL_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `ACT_CMMN_CASEDEF` (`ID_`),
  CONSTRAINT `ACT_FK_MIL_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `ACT_CMMN_RU_CASE_INST` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_RU_MIL_INST`
--

LOCK TABLES `ACT_CMMN_RU_MIL_INST` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_RU_MIL_INST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_RU_MIL_INST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_RU_PLAN_ITEM_INST`
--

DROP TABLE IF EXISTS `ACT_CMMN_RU_PLAN_ITEM_INST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_RU_PLAN_ITEM_INST` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STAGE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_STAGE_` tinyint DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_COMPLETEABLE_` tinyint DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `SENTRY_PART_INST_COUNT_` int DEFAULT NULL,
  `LAST_AVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_ENABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_DISABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_STARTED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_TIME_` datetime(3) DEFAULT NULL,
  `OCCURRED_TIME_` datetime(3) DEFAULT NULL,
  `TERMINATED_TIME_` datetime(3) DEFAULT NULL,
  `EXIT_TIME_` datetime(3) DEFAULT NULL,
  `ENDED_TIME_` datetime(3) DEFAULT NULL,
  `ENTRY_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXTRA_VALUE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DERIVED_CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UNAVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_PLAN_ITEM_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_PLAN_ITEM_CASE_INST` (`CASE_INST_ID_`),
  KEY `ACT_IDX_PLAN_ITEM_STAGE_INST` (`STAGE_INST_ID_`),
  CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `ACT_CMMN_CASEDEF` (`ID_`),
  CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `ACT_CMMN_RU_CASE_INST` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_RU_PLAN_ITEM_INST`
--

LOCK TABLES `ACT_CMMN_RU_PLAN_ITEM_INST` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_RU_PLAN_ITEM_INST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_RU_PLAN_ITEM_INST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_CMMN_RU_SENTRY_PART_INST`
--

DROP TABLE IF EXISTS `ACT_CMMN_RU_SENTRY_PART_INST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_CMMN_RU_SENTRY_PART_INST` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PLAN_ITEM_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ON_PART_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IF_PART_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TIME_STAMP_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SENTRY_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_SENTRY_CASE_INST` (`CASE_INST_ID_`),
  KEY `ACT_IDX_SENTRY_PLAN_ITEM` (`PLAN_ITEM_INST_ID_`),
  CONSTRAINT `ACT_FK_SENTRY_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `ACT_CMMN_CASEDEF` (`ID_`),
  CONSTRAINT `ACT_FK_SENTRY_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `ACT_CMMN_RU_CASE_INST` (`ID_`),
  CONSTRAINT `ACT_FK_SENTRY_PLAN_ITEM` FOREIGN KEY (`PLAN_ITEM_INST_ID_`) REFERENCES `ACT_CMMN_RU_PLAN_ITEM_INST` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_CMMN_RU_SENTRY_PART_INST`
--

LOCK TABLES `ACT_CMMN_RU_SENTRY_PART_INST` WRITE;
/*!40000 ALTER TABLE `ACT_CMMN_RU_SENTRY_PART_INST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_CMMN_RU_SENTRY_PART_INST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_EVT_LOG`
--

DROP TABLE IF EXISTS `ACT_EVT_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_EVT_LOG` (
  `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_EVT_LOG`
--

LOCK TABLES `ACT_EVT_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_EVT_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_EVT_LOG` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_GE_BYTEARRAY`
--

DROP TABLE IF EXISTS `ACT_GE_BYTEARRAY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_GE_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_BYTEAR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_GE_BYTEARRAY`
--

LOCK TABLES `ACT_GE_BYTEARRAY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` DISABLE KEYS */;
INSERT INTO `ACT_GE_BYTEARRAY` VALUES ('5c8e8e8b-ab91-11f1-8dda-aa80f599cca5',1,'D:\\project\\vscode\\hrm\\hrm-server\\target\\classes\\processes\\leave.bpmn20.xml','5c8e8e8a-ab91-11f1-8dda-aa80f599cca5',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:activiti=\"http://activiti.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://www.activiti.org/processdef\">\r\n  <process id=\"leave\" name=\"ËØ∑ÂÅáÊµÅÁ®ã\" isExecutable=\"true\">\r\n    <startEvent id=\"sid-a0962612-3504-47dc-9a45-24b6dfb9cc2b\"/>\r\n    <userTask id=\"hr_audit\" name=\"‰∫∫‰∫ãÂÆ°Êâπ\" activiti:candidateUsers=\"${hr}\"/>\r\n    <sequenceFlow id=\"sid-d4d4c013-574f-4da9-8f7b-5742ffff3342\" sourceRef=\"sid-a0962612-3504-47dc-9a45-24b6dfb9cc2b\" targetRef=\"leave_apply\"/>\r\n    <userTask id=\"manager_audit\" name=\"ÁªèÁêÜÂÆ°Êâπ\" activiti:candidateUsers=\"${manager}\"/>\r\n    <sequenceFlow id=\"sid-6a2c08f6-c31a-4b5d-a0c5-8883cab86f80\" sourceRef=\"hr_audit\" targetRef=\"manager_audit\">\r\n      <extensionElements>\r\n        <activiti:executionListener event=\"take\" delegateExpression=\"${hrApproveListener}\"/>\r\n      </extensionElements>\r\n      <conditionExpression>${hrAuditStatus == 1}</conditionExpression>\r\n    </sequenceFlow>\r\n    <endEvent id=\"sid-152bed44-c3a7-4c1c-a0fc-9b6c72b4a346\"/>\r\n    <sequenceFlow id=\"sid-3dd17b07-4a46-4330-a641-0b4df7a11409\" sourceRef=\"manager_audit\" targetRef=\"sid-152bed44-c3a7-4c1c-a0fc-9b6c72b4a346\">\r\n      <extensionElements>\r\n        <activiti:executionListener event=\"take\" delegateExpression=\"${managerApproveListener}\"/>\r\n      </extensionElements>\r\n      <conditionExpression>${managerAuditStatus == 1}</conditionExpression>\r\n    </sequenceFlow>\r\n    <userTask id=\"leave_apply\" name=\"ËØ∑ÂÅáÁî≥ËØ∑\" activiti:assignee=\"${staff}\"/>\r\n    <sequenceFlow id=\"sid-f6d07268-16bf-48d5-ad9e-10fc17ccc3c2\" sourceRef=\"leave_apply\" targetRef=\"hr_audit\"/>\r\n    <sequenceFlow id=\"sid-48777b7d-bf8a-436e-a891-046fce766ace\" sourceRef=\"hr_audit\" targetRef=\"leave_apply\">\r\n      <conditionExpression>${hrAuditStatus == 2}</conditionExpression>\r\n    </sequenceFlow>\r\n    <sequenceFlow id=\"sid-921e87e3-6637-4fcd-952a-00d4feb9cb80\" sourceRef=\"manager_audit\" targetRef=\"hr_audit\">\r\n      <conditionExpression>${managerAuditStatus == 2}</conditionExpression>\r\n    </sequenceFlow>\r\n  </process>\r\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_leave\">\r\n    <bpmndi:BPMNPlane bpmnElement=\"leave\" id=\"BPMNPlane_leave\">\r\n      <bpmndi:BPMNShape id=\"shape-673e3a47-cf51-49db-88d2-6c4aca79cd53\" bpmnElement=\"sid-a0962612-3504-47dc-9a45-24b6dfb9cc2b\">\r\n        <omgdc:Bounds x=\"-55.0\" y=\"55.0\" width=\"30.0\" height=\"30.0\"/>\r\n      </bpmndi:BPMNShape>\r\n      <bpmndi:BPMNShape id=\"shape-1a97734a-7a57-49a7-bd6b-c7fcbfe00182\" bpmnElement=\"hr_audit\">\r\n        <omgdc:Bounds x=\"105.0\" y=\"47.5\" width=\"70.0\" height=\"45.0\"/>\r\n      </bpmndi:BPMNShape>\r\n      <bpmndi:BPMNEdge id=\"edge-10cf0bd9-f844-44ff-9cd0-d189b123c5ea\" bpmnElement=\"sid-d4d4c013-574f-4da9-8f7b-5742ffff3342\">\r\n        <omgdi:waypoint x=\"-25.0\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"10.0\" y=\"70.0\"/>\r\n      </bpmndi:BPMNEdge>\r\n      <bpmndi:BPMNShape id=\"shape-dc315ef3-ea8d-45eb-bf65-bb6d7580f9e9\" bpmnElement=\"manager_audit\">\r\n        <omgdc:Bounds x=\"219.99998\" y=\"47.499992\" width=\"75.0\" height=\"45.0\"/>\r\n      </bpmndi:BPMNShape>\r\n      <bpmndi:BPMNEdge id=\"edge-5207b15c-abe3-4ab7-a4fc-b1f93d4124bd\" bpmnElement=\"sid-6a2c08f6-c31a-4b5d-a0c5-8883cab86f80\">\r\n        <omgdi:waypoint x=\"175.0\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"218.75002\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"219.99998\" y=\"69.99999\"/>\r\n      </bpmndi:BPMNEdge>\r\n      <bpmndi:BPMNShape id=\"shape-0c69d7d5-2820-47a9-b6f9-b17190f4eee0\" bpmnElement=\"sid-152bed44-c3a7-4c1c-a0fc-9b6c72b4a346\">\r\n        <omgdc:Bounds x=\"350.0\" y=\"55.000004\" width=\"30.0\" height=\"30.0\"/>\r\n      </bpmndi:BPMNShape>\r\n      <bpmndi:BPMNEdge id=\"edge-0ceee8b7-29a5-4632-bd13-473bbc3fdb29\" bpmnElement=\"sid-3dd17b07-4a46-4330-a641-0b4df7a11409\">\r\n        <omgdi:waypoint x=\"292.5\" y=\"70.00001\"/>\r\n        <omgdi:waypoint x=\"295.00003\" y=\"69.999985\"/>\r\n        <omgdi:waypoint x=\"283.75\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"295.0\" y=\"69.99999\"/>\r\n        <omgdi:waypoint x=\"292.5\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"316.875\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"331.25\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"350.0\" y=\"70.0\"/>\r\n      </bpmndi:BPMNEdge>\r\n      <bpmndi:BPMNShape id=\"shape-a0e0d5b7-6dc9-4780-99b1-3ff2e4577474\" bpmnElement=\"leave_apply\">\r\n        <omgdc:Bounds x=\"10.0\" y=\"45.0\" width=\"70.0\" height=\"50.0\"/>\r\n      </bpmndi:BPMNShape>\r\n      <bpmndi:BPMNEdge id=\"edge-d8843222-9c15-4c2a-8198-95cea8524aa1\" bpmnElement=\"sid-f6d07268-16bf-48d5-ad9e-10fc17ccc3c2\">\r\n        <omgdi:waypoint x=\"79.99999\" y=\"70.0\"/>\r\n        <omgdi:waypoint x=\"105.0\" y=\"70.0\"/>\r\n      </bpmndi:BPMNEdge>\r\n      <bpmndi:BPMNEdge id=\"edge-be18ba51-17d7-4f54-a1ac-2b0d150405e7\" bpmnElement=\"sid-48777b7d-bf8a-436e-a891-046fce766ace\">\r\n        <omgdi:waypoint x=\"140.0\" y=\"47.499992\"/>\r\n        <omgdi:waypoint x=\"140.0\" y=\"-1.875\"/>\r\n        <omgdi:waypoint x=\"45.0\" y=\"-1.875\"/>\r\n        <omgdi:waypoint x=\"45.0\" y=\"45.0\"/>\r\n      </bpmndi:BPMNEdge>\r\n      <bpmndi:BPMNEdge id=\"edge-f7d60e66-de25-45a3-ae5d-79d53e73c6d6\" bpmnElement=\"sid-921e87e3-6637-4fcd-952a-00d4feb9cb80\">\r\n        <omgdi:waypoint x=\"257.5\" y=\"47.499992\"/>\r\n        <omgdi:waypoint x=\"257.5\" y=\"-46.875\"/>\r\n        <omgdi:waypoint x=\"45.0\" y=\"-46.875\"/>\r\n        <omgdi:waypoint x=\"45.0\" y=\"45.0\"/>\r\n      </bpmndi:BPMNEdge>\r\n    </bpmndi:BPMNPlane>\r\n  </bpmndi:BPMNDiagram>\r\n</definitions>\r\n',0),('5d52effc-ab91-11f1-8dda-aa80f599cca5',1,'D:\\project\\vscode\\hrm\\hrm-server\\target\\classes\\processes\\leave.leave.png','5c8e8e8a-ab91-11f1-8dda-aa80f599cca5',_binary 'âPNG\r\n\Z\n\0\0\0\rIHDR\0\0Ω\0\0\0ó\0\0\0ÅnÖ†\0\0IDATx^\ÌùpU\ıï«É\⁄\÷Z[w˚g⁄é€∫∂:;\€\Ètg;\Ì∂\Î\Ó¥\Èémóq¢ªŒæ‹º\˜LH¯[˛àÇN£°∏\Ì¿0¬¥VYgîQ\n•ÉF! \"(R™P¬ø»üÑ@ ¸\”@Ä\‹=\'ºáó\Ûní\ﬂÀª\˜æ\ﬂ\Ôwøüô\Ôº\‰û˚\˜úw~\'\Á\Ê˛))\⁄PVVvMEEÖAPpíy\0\–J\“ˇ!µ$â/I\0 ?P\Ù\00\0J\‘_ë÷ç9\Ú\“\0PE\03B…∫ò\Ù§4\0\0\‘A\—¿âƒµé\„¸ï4N\⁄\0\0j†\Ë`T¯n§§mK&ìˇ!m\0ÄÅA\—¿0(iKIáR©\‘7§\r\0\–?(z\0à\„8c(y∑\ﬁ~˚Ìüï6\0@ﬂ†\Ë`(îºOê\Z\È\«!\“\0\E\0C\·\€(Å\◊&ì\…G§\r\0\‡ä\0CÔãî\ƒ{Hé¥\0rA\—¿p®\}á˘∞\„8ﬂï6\0¿\Â†\Ë`T\\Ó§d˛Ä\n\‡ó•\r\0\1(z\0X%\Û√§\ıâD\‚ì\“\0∏ä\0\ˆ0Ñ:æ?QR?%\r\0Äã†\Ë`UUUü°§~õ4A\⁄\0\0(z\0XG*ï∫Åªµºº¸Vi \Ó†\Ë`!T\~D\…}(ëH\‹$m\0\ƒ=\0,Öí{d2ô‹ñNß?\'m\0\ƒ=\0,\∆qú\ﬂSí/ihh∏B\⁄\0à#(z\0XLii\ÈU\‘\Ì5ë~#m\0\ƒ=\0,gÿ∞a_†D\ﬂMÖ/)m\0\ƒ\r=\0bÄ\„8ﬂ¶d?L˙û¥\'P\Ù\0à	\‘\Ë˝%¸æD\"\Òi .†\Ë#(\·$m:tËß§\rÄ8Ä¢@Ã†éÔèî¯s\Ât\0\‚\0ä\01£¨¨\Ï\ZJ¸-§I\“Ä\Ì†\ËCâ\ƒ\◊)˘[\«˘ô¥`3(z\0ƒîd2˘\Ô4\0¥ßR©õ•\r\0[A\— \∆P·´£A\‡=\Í¸Æì6\0lEÄò\„8\ŒoIK\Ò®2P\Ù\0à9¸®2\ZVQ\◊7C\⁄\0∞\r=\0\0_\ÿ\Úy\ZvQ·´î6\0lE\0\–æo—Äp\ÿqú\ÔK\0∂Ä¢\0∏uze4(ÏØ¨¨¸™¥`(z\0ÄÀ†\¬\˜K\Z\ﬁƒ£ Äç†\Ë\0r†\¬\˜\Z\Ê\…\È\0òä\0 áD\"\Òi\Z6ì&K\0&É¢\0\%ùNˇ\r©\Î˚Oi¿TP\Ù\0\0}B\ƒ-§v\Í¸˛A\⁄\00=\0@ø8éSC≈é\Í\ÍÍøë6\0LE\00 T¯fëñS\«w•¥`(z\0Ä\·bGÉ\≈J*|3•\r\0ì@\—\0(ëJ•˛ñåf*|√§\r\0S@\—\0(C\Ô˘\¬\“§\r\0@\—@s8I°\‚I\∆ò\rb\nÄ\ÊËò§¥O\˜ì6UWW_-m6°£\ÔAa ¶\0héÆIJ˚\ı\\2ô|VN∑	]}b\nÄ\ÊËö§\‹\Â—æΩ\≈]ü¥ŸÇÆæÉ1@stN\“D\"q=\Ì\ﬂ\“m\“f:˚\ƒ\0\Õ\—=Iiˇ~@j\Á+;•\Õtt\˜=\»\ƒ\0\Õ1!I˘\ﬁ=\⁄\œfæóO\⁄L\∆ﬂÉ¸@L\–Síîü\÷B˚∫“¶Gïô\‚{†b\nÄÊòí§\\\Ï¯˘ú¸úNi3S|\‘AL\–ìíî\ﬂ\ƒ@˚ªÉ\ﬂ\Ã m&bí\ÔÅ\Zà)\0öcZí\Úª\˜hü€ì\…\‰øJõiò\Ê{00à)\0öcbíRß7î\ˆ˚ ø}]\⁄L\¬DﬂÉ˛AL\–Sìî\ˆ{2i3u~üñ6S0\’\˜†oS\04\«\‰$•}üóL&ˇ ßõÇ…æ˛ ¶\0hé\…IöyTŸõT¯~)m&`≤\ÔÅ?à)\0öczíVVV~ïéa?æ2i\”\”}rAL\–í4ïJ˝\«\·D\"\Ò-i\”|.1@slIR\Í\Ù*\ÈXvQ\·˚º¥\Èä-æÉò†96%)æt<´JKKØí6±\…\˜\‡\"à)\0öcSí644\\\·8\ŒR\“o•MGl\Ú=∏b\nÄ\Êÿñ§âD\‚::¶\˜®Î´ì6›∞\Õ\˜\01@{lL\“T*u3W{yy˘øIõN\ÿ\Ë˚∏Éò†9∂&©\„8?£ck•\Œ\Ô\Î“¶∂˙>\Œ ¶\0hé\ÕIJ\«6â¥•¨¨\Ï\Zi\”õ}WS\04\«\ˆ$•\„õKZ(ß\ÎÄ\Ìæè#à)\0öc{í:\ÙStåIJ[±±\›\˜q1@s‚ê§âD\‚+tú˚«πC⁄äI|7S\04\'.IJ\«˘=\“a*|ﬂñ∂b\ﬂ\«	\ƒ\0ÕâSí&	:\ﬁ\›√Ü\r˚Ç¥É8˘>. ¶\0hN‹íî\Í\ﬁoHM:<™,næèà)\0ö∑$\ÂGï\—1/q\Á\˜\“5q\Û}@L–ú8&i:ù˛\˜v\“Hiãí8˙\ﬁvS\04\'ÆIöH$n¢c?T^^˛#iãä∏˙\ﬁVöõõGŒú9”•\Ô÷ç\“\0–Ñ8ºT\n•\„oM•R7H[\ÿ`Ä¥\◊uØ=q\‚DWGGá[[[ªB\⁄\0ö\Á¢\«8é3ë|\vUU\’g§-,0@\⁄«Æ]ªñ∫öööé$ì\…r9\0@\‚^\Ù\Ú¡ST¸˛D?ë∂0¿\0i{\˜\Ó˝\ÒÖ≤!u{zz.\‘\◊◊∑\ÛkÆ\‰º\0Ä°¡}\rπ¥F.g;48}íé˚u\“\√\“4 \ÌÇBxe{{˚\·K\Õ¿\”\“\È\Ùr~\0@Ñ–†^\ÍS\‰§J\ÂrqÄ∫≠/”±@ﬂù\“ \Ì£ππyñågñ˘\Û\Áß\Ô\”\Â2\0Ä\·NŒß\–≈∂\À\ÛB\‘w\…á©\0~\«;=ïJ}ÉlSº\”Hªhkk˚˚3g\Œ|‹∂Œü?ˇ—Ñ	∂\Î\ \0bwr>\≈.\÷]û\ÚÅC\⁄CÖ\Ôã¸{yy˘O\Ë\˜˙˝}9o>`Ä¥è}˚\ˆmïqîlﬂæ˝}w\Óì\À\0\"Ñ;:üÇ\Î.\œ\rRèê?÷í&ê\Œe¸”ì-ÑÉ§]$â+∑l\ŸrV\∆Pr\Ïÿ±\◊\ËªsÑ\Áó\Î\0\0Dwt>EØT\ŒW2∂¥HQA*ì\Û™Ä\“N\Í\Î\Îˇ\˜\ÏŸ≥\Ád=úú4iR˛ê@*.\Ô\ˆ\–\Âe†Ç\Û%\Ú\«:Y\2˙µú_ê\ˆ¡ß¢.\\xP2\À\ÊÕõü¶\Ô\Ã;8e\rÄT\\\ﬁ\ÌïJ{q\Áü*|:<è\÷\ eT¡\0i\'555∑¥¥¥t\Àx^∏p\·/\Èt˙\0.N@#*2›ûúG™´´Ø&_º\‰S\Ëº˙p\‰»ëüêÀ™Ç\“N¶Oüæ®á\Ñ\Ù<O£\Ô\À\„r^\0@©\»t{rzú!¸s2ô|ñ>ª}äûKÖ\È˚rô|¿\0i¸`ÅW^y\ÂT6†mmm(û\¿\02P^|rœû=‹∏qc\˜\Í’´]Jòÿäéøg›∫u≠MMMwH?\Z∞Æß\‚7Éä\\ß(z\Âº˘Ä\“N&Nú8¨≥≥≥õüß:|¯\w\Òh9\0<P¡[D=?â\√\Ì\Í\Írœû=[\Ò\Ò≥÷Æ]{Çä¡m\“W≈Üä—µo]ÿõ)|[\Â<˘Ç\“N\ÊÃô\Ûg~s}GñI\0±Ü;<\Ëeà≥®\„È†¢∑I˙*h\€eØ\\π\“}¸\Ò\«\›\ \ J\˜\…\'üÃ±Á´ßü~\⁄\Âr î)9∂®§kó≠\¬`\„¶/^\‹{6Ä?•-jô[`!ú§q\Ô\§\ÿî¨g§ØÇ¶\–.{\ÁŒù\Ó∏q\„‹ì\'O\Ê\ÿ\Ú—ÅzH˛î∂®§{ó\›Ö\∆1,µ∂∂\ÊL+ÜLé-∞˛KL~I°≥Ω°J_M]v°/+]»®∫\Ï 	\"éqêâ±¢Z\ÙNos\˜º5\◊›∂\ÍWΩ\‚üyöú\œEQ\Ù\–e\Á*™.;HG5ô[`!*E\ÔT\ÁAw\Îäz\˜ù•S.Ocõú\ﬂEQ\ÙT|GE\·˚ A\’eZlÅÖ®$\Ï˛m/\Êº¨l[í3ø\rä\"9U|\œBó≠7à£∫Lã-∞ïÑ}o\Õ\Ùúbó\€\‰¸6(ä\‰T\Ò=∫l˝A\’eZlÅÖ®$\Ï\÷W\Zrí\ıR“íM\ŒoÉ¢HNﬂ£\À\÷\ƒQ]¶\≈6,â\ƒM¸2h“ãõH˚3\˜\ﬁ\Ú\Á&û\ŒvûO.:Æ\Î^±q\„\∆;\ÁÕõ∑r\Í‘©\˜\ﬂ◊ò1czxGè}n\Ú\‰\…\'i⁄Æi”¶Õ¨©©˘ö\\^wTE/T|è.[Guô\€ \·\◊tQ›®M&ì\€2NIô˘kC\Õª´ó/_˛d}}}w]]]\Ô\Õ¿oº\ÒÜª{\˜n\˜\Ë—£Ωèm\‚O˛ùß≥Ω∂∂\ˆ\¬¯\Ò\„\˜å1\‚\Ár}∫¢í∞¸ˇô®Y±M\ŒoÉ¢HN\ﬂ\„˝A\’eZlÉÇä\÷mTº∂ÀÇñè2\≈/ú˚©à˝Ç:∑Æª\Ôæ€•.èü<ü}<aø\|<ˇÑ	Œç;vµßﬂî\Î\÷\rïÑ\›˘˙c9âö\€\‰¸6(ä\‰T\Ò}\À(|$à£∫Lãm°444\\¡\œÕï¨∫∫⁄ù={∂\À5\k§x:\€y>π,Øè\◊+∑5(hõCV¨X\Ò‹®Q£\‹eÀñπ\Áœüøº™)\¬\À\Ò\Ú¥√ßi*∑£*	{\Ù\‡_›≠+\ŒMVö\∆69ø\rä\"9U|è.[Guô\€B\‡g\ÂR\„≥\‘[∞jkk\›\∆\∆F∑ª;\Á≠^æ\|<?/\Á]Øó\◊/∑ô¥˛!ã/~sÃò1\Ó˚\Ôø/∑=(x=uuu•R©Zπ=]PIX\÷\ﬁ\Õ\Ûríïß\…˘lQ…©\‚{t\Ÿ˙É8™À¥\ÿ\Ó\ƒ*\ƒ;1}\ÙQ\˜‘©K/5\…^éó]\ﬂKu|\‹\·ç=∫\˜qA\¬Î´©©9U^^~´‹¶®$\Ï\Ÿ3g‹ùû\»MVö∆∂ú˘-P…©\‚{t\Ÿ˙É8™À¥\ÿ\Ô)M˙\Ÿ]¥hë{˘´+\Ûáó\Á\ı\˙<\Îû!∑≠ƒÜ\r~¡/®OB\Î\Ì6lXgQ.?ÄÅñ\Ô!j~˝w9…ö\€lº\œ(ä\‰\»\˜Y°\À\÷\ƒQ]¶\≈v0P1∫\Õ€ëq°\n^ü\Ë¯Úª∏Ö\÷q\ı#è<\“\≈ˇÉì\∆\∆\∆\ŒT*µ\\nø\ÿ\Ùô∞\‘¡µ\Ìlr\ﬂ]˛@N¢J\Ò<<ØM]_\…Ÿß\ÔΩBó≠=à£∫Lãmæ\mﬁ´4˘îd°ûÑ\◊\Á=\’\…Wu\Êu;\√\À/ø¸|ï\Ê`/ZQÖ\◊_WWw§ºº¸\'räâ_\¬\‘\›\ı%õ∫æ(í\”\œ\˜^\rõ¸\ÌUæ\ƒQ]*±-\ËˇTEÜäPm∂\Ò\≈\'É˝\ﬁ@\z\≈\≈-j◊ç–≤W<¯\‡É\›|õA¨_ø˛8\Ì\‹kr?äâ_¬™tw}âóï\Î3Q*\…Y(~æ\Ô∫\Ï~}Ø€†à8™´ø\ÿ:t\Ë\∆}˚\ˆm›≤e\ÀŸáz\Ë◊•••W\…yt\«{\„˘ã/æ(K@†\˙Ω›û\‹_6l\ÿ\\ﬂ#FåPæØPx;\Èt˙8µ¢\◊\À}	\Zr\ƒ\ZR©ú.\ÒKXôê˘JÆ\œD\ıóúQà\Ô\Í\n˙íM\›B_æ/∆†®K\ƒQ]~±•°\Ò\ \Ê\Ê\ÊYgŒúπ4”º\Á.\\x∞¶¶\Ê91P˘§R©õ≥Eà\ÔØ\„\„\r^ø\˜>>ﬁæ‹ßûy\ÊôW¯I*Q2u\Í‘≠é„åë˚4YG,øÑÖ¸ìSïB|Ø\“\Ù%[ªl∑àÉ¢J,Gu\…\ÿ\Ó›ª\˜\«\Ì\Ì\Ìá=C\‰e¥¥¥tOü>}5\n\◊yóã\Zï\ÔMüúùo÷¨Y\ÚPBÅ∑\„Ÿ∑\…rürò6m⁄ë®NmfYµj∑øã\Âæç\«˝\À/a°\‹\‰ÃáB|/¿|%\◊g¢ºæ/\ˆ†®K\ƒQ]\Ÿ\ÿR\Ë>ªk◊Æ•*g\ŸzZ\Ó‘§Iì\Ó\Ú˙=JTæ\Ù{c\÷\ŒOTâﬁégü\ZΩ˚\„\À}\˜\›◊µg\œπûPŸ±cG\Ì\‹[r_Ç\∆\'Hæ¡\ÚKX(\¢\ﬂ\Á!\ˆã´…†\Ë√úX\"é\Íb_Ω˝\ˆ\€\„Oú8\—%\„8ùùù\›s\Ê\Ã˘3˝Ås£S\Ë¯\ƒ?\Á{@üoeßGUWx;û}∏Æå=∫\Áÿ±cr=°r\Ù\Ë\—S¥s\ÂæçOp§zÉÖÑ\ıWHEæWü}\—eP\Ùâù‘ö∏\«\Ò\√?‰éª\˜¡˚Kñ,q\ÁŒù\€{⁄≠°°¡ù<y≤\ÀOπ\Z>|8_\œ\–Î≥ô3g\ˆ\∆J\∆o \Ëoõ˝è=\ˆ\ÿnZ«ëT*uÉåUò¯\ƒ]j\r©#˚{TuÖ∑\„ŸáÅ\ÎJUUï{\Ó\‹9πûP°\Ìù\ÙqX\—\˜Ñ\ÌK\“OaIn7\Ójmm\Ì;::d\Í/\√\ÀJá≠8\ÁPWWW\ÔBX<ûrq;v¨{\ÔΩ\˜\ˆ=.~O=\ıTé\œjjjzüO¨\“\…\Û<<//#◊£´¢™+º\œv\œ\»\Zó√®Q£\ŒEUë≥9rdWÖJE.è\÷T\‡€Ä\n©”É\Ô¥x\ÒbæßueSS\”˙\Î~\‡Q\—uœ∑µµ-†\˜]\ÚÔ≤à:ΩK±å{π\\…iRßOü\Ó˝\Ù\Ê\ \Ú˙˙˙\ˆ˛˛gK\Ô/¸?[\Ú\ıûˇR`\"\∆\'˛9\ﬂ˙<òùU]…ª”£øFNFu\Ó5\À\ˆ\Ì\€7W®ú{-ê˛Ç\„%åÑ•\’˙**{\n∏\Ë¡\˜y(\Î{EïXÜGØJ|¸Õä\ §d^\ÒH\Èt˙â˘\Û\Á?˛¸Gû–û‹ºy\Û\”d;@˛~<¨ïTQ˘Tò\?Ωx†9\Í´7ü˛y~=|îWo\Ê\«K	[\‚ì0\ﬁia€ÉêL\Œ|Ä\Ôì\◊\˜\≈UbFΩ*\Ò\ÒØwZ\ÿ\ˆ \’W^9é\Û√â\'RO∞˝u/Ø\›s\œ=M\‰\Ôwx∫ú∑®|*L∏zs\Í‘©3£æOo‹∏q\À\"∫OØ\œ\‡x	#aK|\∆;-l{\Í+9UÄ\Ôìü\Ôã5(™\ƒ2å8zU\‚\„_Ô¥∞\ÌA\ /∂Y¯a‘≠\ﬂG˛>¬üQ<|@ï\ÔAÖ	\˜\È\’\‘\‘|≠ÆÆ\ÓÇ\ ?SÉÄ∂sà\Z\≈YT	#aK|\∆;-l{\Í/9Éæ\˜W_æ\◊uP#é^ï¯¯\◊;-l{ê\Í+∂^t{Ãú*\≈x\"\À]w\›’ì›¶\“Yò\Ò\„\«\Ôç\Í\ÁÇû≠0\‡ŸõÖ™\ƒ\'aº\”¬∂!ï\‰,¯\ﬁ_˘^∑A1å8zU\‚\„_Ô¥∞\ÌAj†ÿöé\ˆ\œ\ﬁdFå\Ò\Û	&úã\‡-˚i\Ávö\ñÖBU\‚ì0\ﬁia€ÉP\…	\ﬂ˚+\n\ﬂIq\Ù™\ƒ«ø\ﬁia€Éîi±Õóäﬂ≤0|¯\\Ÿm\Òv\Âæ\ÙÀ∏q\„∂,[∂,ÿó]Nœå3\Ê“é-ì\€.6a$lâO\¬xßÖmBQ$\'|\ÔØ(|$a\ƒ—´ˇzßÖmR¶\≈6_2\Ô”ª\‘\Ìi˘>=Ü∏©¶¶\ÊTXoN\ı\’Wü°ùk\„\Ì\»mõ0ñVÎ´®\ÏA(ä\‰Ñ\Ô˝Ö\ÔÉ$å8zU\‚\„oVT\ˆ eZlCÖ\ÓoN\œB\’\Úß\‘.~\‘\ﬁ\ﬁ.∑Q---KhßZ\À\À\Àoï\€‘Å∞\÷TEëúΩø¢\}ê é\Í2-∂ÉÖ\Í\…O\'\÷[®\n\Ì¯xy^Øœ≥\Ór\€yëJ•j©\„;T«ó\È\¯.˝\·r[∫ÄÑ\ıW\…	\ﬂ˚+\n\ﬂ	\‚®.\”b;X¯b+\Z\˜_\Úvd|Jr∞ˇ\„\„Âºß43z)êã∫∏#´™™\Ílll\Ï\Ï\≈-|\—J\Êxát\Ì\≤ a˝Er\¬\˜˛ä\¬\˜AÇ8™À¥\ÿB\"ë∏\÷qú•\ﬁB\≈∑Pmqªª’û∑\Õ\Û\Ò¸\‚¢ó\◊\À\Îó\€4¥\¬oR◊∑¢ÆÆ\Ó\»˙\ıÎè´\ﬁ\«G\Ûµ/X∞\‡9⁄©ù§óy=r›∫ÅÑ\ıW\…	\ﬂ˚+\n\ﬂ	\‚®.\”b[(‹âyOuf\≈\˜\ÒÕû=ª\˜â*ªw\Ô\Ê∑\Ô\Ù\÷˛\‰\ﬂy:€Ω\˜\·e\≈\Î§\√\ÛÉo/†ç¨Kß\”\'h#\ÔÆ^Ωz\Îé;\ˆuttú¶˝;u¯\\·=€∂m\€¸\¬/4é?˛ÂäãØñXß\€m	˝ÅÑ\ıW\…	\ﬂ˚+\n\ﬂ	\‚®.\”bTn\Û^\’9eñ\‹E+˘\¬OP°çç¶Œçüõπâ\ƒ\˜\‹\Òé\\Á¶\Ã\Ù\—:=iE$¨ø¢HN¯\ﬁ_Q¯>HGuô\€ \·\€\n®N\‘\Ê[¸2\Û\◊\Ê}[\	\ÎØ(íæ\˜Wæ\ƒQ]¶\≈6,¯\ˆ5jñ¶\Ù\◊H±]\«\€‹å	\ÎØ(íæ\˜Wæ\ƒQ]¶\≈X\÷_Q$\'|\ÔØ(|$à£∫Lã-∞$¨ø¢H\Œ’´W\˜®ºu:N\"¥í\Ô\œH_\È\‚®&c,E\œ_QΩu\Î÷µ\ÚÄ\‰∂\„¨>¯`˘~ì\Ùï\Œ éj21∂¿BP\Ù¸E\—kjj∫c\Ì⁄µ\'\⁄\⁄\⁄:\‚\ﬁ)\–Ò∑µ¥¥\Ã\'ø\Ô#EsYv@ é˝\À\‰\ÿ¡©ô\\Ey\ZÜ˛\Îótñmå\≈\«\œ~0rP\‰˝\Œ\Ï\‹\„\Ë\'£c,ßfrÖ\”0\0\0`)85\Û±p\Z\0\0b\0\ô\”q?5É\”0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0 æ¸?\◊bôç\Ùp\Õa\0\0\0\0IENDÆB`Ç',1);
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_GE_PROPERTY`
--

DROP TABLE IF EXISTS `ACT_GE_PROPERTY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_GE_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_GE_PROPERTY`
--

LOCK TABLES `ACT_GE_PROPERTY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` DISABLE KEYS */;
INSERT INTO `ACT_GE_PROPERTY` VALUES ('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('cmmn.schema.version','8.0.0.0',1),('common.schema.version','8.0.0.0',1),('eventregistry.schema.version','8.0.0.0',1),('next.dbid','1',1),('schema.history','create(8.0.0.0)',1),('schema.version','8.0.0.0',1);
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ACTINST`
--

DROP TABLE IF EXISTS `ACT_HI_ACTINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ACTINST`
--

LOCK TABLES `ACT_HI_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ATTACHMENT`
--

DROP TABLE IF EXISTS `ACT_HI_ATTACHMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ATTACHMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ATTACHMENT`
--

LOCK TABLES `ACT_HI_ATTACHMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_COMMENT`
--

DROP TABLE IF EXISTS `ACT_HI_COMMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_COMMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_COMMENT`
--

LOCK TABLES `ACT_HI_COMMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_DETAIL`
--

DROP TABLE IF EXISTS `ACT_HI_DETAIL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_DETAIL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_DETAIL`
--

LOCK TABLES `ACT_HI_DETAIL` WRITE;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_HI_ENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ENTITYLINK`
--

LOCK TABLES `ACT_HI_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_IDENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_HI_IDENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_IDENTITYLINK`
--

LOCK TABLES `ACT_HI_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_PROCINST`
--

DROP TABLE IF EXISTS `ACT_HI_PROCINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_PROCINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_PROCINST`
--

LOCK TABLES `ACT_HI_PROCINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_TASKINST`
--

DROP TABLE IF EXISTS `ACT_HI_TASKINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_TASKINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_TASKINST`
--

LOCK TABLES `ACT_HI_TASKINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_TSK_LOG`
--

DROP TABLE IF EXISTS `ACT_HI_TSK_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_TSK_LOG` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ACT_HI_TSK_LOG_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_TSK_LOG`
--

LOCK TABLES `ACT_HI_TSK_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_VARINST`
--

DROP TABLE IF EXISTS `ACT_HI_VARINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_VARINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_VARINST`
--

LOCK TABLES `ACT_HI_VARINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_VARINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_VARINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_BYTEARRAY`
--

DROP TABLE IF EXISTS `ACT_ID_BYTEARRAY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_BYTEARRAY`
--

LOCK TABLES `ACT_ID_BYTEARRAY` WRITE;
/*!40000 ALTER TABLE `ACT_ID_BYTEARRAY` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_BYTEARRAY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_GROUP`
--

DROP TABLE IF EXISTS `ACT_ID_GROUP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_GROUP` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_GROUP`
--

LOCK TABLES `ACT_ID_GROUP` WRITE;
/*!40000 ALTER TABLE `ACT_ID_GROUP` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_GROUP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_INFO`
--

DROP TABLE IF EXISTS `ACT_ID_INFO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_INFO` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_INFO`
--

LOCK TABLES `ACT_ID_INFO` WRITE;
/*!40000 ALTER TABLE `ACT_ID_INFO` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_INFO` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_MEMBERSHIP`
--

DROP TABLE IF EXISTS `ACT_ID_MEMBERSHIP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_MEMBERSHIP` (
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `ACT_ID_GROUP` (`ID_`),
  CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `ACT_ID_USER` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_MEMBERSHIP`
--

LOCK TABLES `ACT_ID_MEMBERSHIP` WRITE;
/*!40000 ALTER TABLE `ACT_ID_MEMBERSHIP` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_MEMBERSHIP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PRIV`
--

DROP TABLE IF EXISTS `ACT_ID_PRIV`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PRIV` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PRIV`
--

LOCK TABLES `ACT_ID_PRIV` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PRIV` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_PRIV` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PRIV_MAPPING`
--

DROP TABLE IF EXISTS `ACT_ID_PRIV_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PRIV_MAPPING` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PRIV_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
  KEY `ACT_IDX_PRIV_USER` (`USER_ID_`),
  KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `ACT_ID_PRIV` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PRIV_MAPPING`
--

LOCK TABLES `ACT_ID_PRIV_MAPPING` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PRIV_MAPPING` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_PRIV_MAPPING` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PROPERTY`
--

DROP TABLE IF EXISTS `ACT_ID_PROPERTY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PROPERTY`
--

LOCK TABLES `ACT_ID_PROPERTY` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PROPERTY` DISABLE KEYS */;
INSERT INTO `ACT_ID_PROPERTY` VALUES ('schema.version','8.0.0.0',1);
/*!40000 ALTER TABLE `ACT_ID_PROPERTY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_TOKEN`
--

DROP TABLE IF EXISTS `ACT_ID_TOKEN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_TOKEN` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TOKEN_VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATE_` timestamp(3) NULL DEFAULT NULL,
  `IP_ADDRESS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_AGENT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATA_` varchar(2000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_TOKEN`
--

LOCK TABLES `ACT_ID_TOKEN` WRITE;
/*!40000 ALTER TABLE `ACT_ID_TOKEN` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_TOKEN` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_USER`
--

DROP TABLE IF EXISTS `ACT_ID_USER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_USER` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `FIRST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LAST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DISPLAY_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EMAIL_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PWD_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_USER`
--

LOCK TABLES `ACT_ID_USER` WRITE;
/*!40000 ALTER TABLE `ACT_ID_USER` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_USER` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_PROCDEF_INFO`
--

DROP TABLE IF EXISTS `ACT_PROCDEF_INFO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_PROCDEF_INFO` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_PROCDEF_INFO`
--

LOCK TABLES `ACT_PROCDEF_INFO` WRITE;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_DEPLOYMENT`
--

DROP TABLE IF EXISTS `ACT_RE_DEPLOYMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_DEPLOYMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_DEPLOYMENT`
--

LOCK TABLES `ACT_RE_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` DISABLE KEYS */;
INSERT INTO `ACT_RE_DEPLOYMENT` VALUES ('5c8e8e8a-ab91-11f1-8dda-aa80f599cca5','SpringBootAutoDeployment',NULL,NULL,'','2026-09-08 22:27:03.663',NULL,NULL,'5c8e8e8a-ab91-11f1-8dda-aa80f599cca5',NULL);
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_MODEL`
--

DROP TABLE IF EXISTS `ACT_RE_MODEL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_MODEL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_MODEL`
--

LOCK TABLES `ACT_RE_MODEL` WRITE;
/*!40000 ALTER TABLE `ACT_RE_MODEL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_MODEL` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_PROCDEF`
--

DROP TABLE IF EXISTS `ACT_RE_PROCDEF`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_PROCDEF` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_PROCDEF`
--

LOCK TABLES `ACT_RE_PROCDEF` WRITE;
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` DISABLE KEYS */;
INSERT INTO `ACT_RE_PROCDEF` VALUES ('leave:1:5d538c3d-ab91-11f1-8dda-aa80f599cca5',1,'http://www.activiti.org/processdef','ËØ∑ÂÅáÊµÅÁ®ã','leave',1,'5c8e8e8a-ab91-11f1-8dda-aa80f599cca5','D:\\project\\vscode\\hrm\\hrm-server\\target\\classes\\processes\\leave.bpmn20.xml','D:\\project\\vscode\\hrm\\hrm-server\\target\\classes\\processes\\leave.leave.png',NULL,0,1,1,'',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_ACTINST`
--

DROP TABLE IF EXISTS `ACT_RU_ACTINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_ACTINST`
--

LOCK TABLES `ACT_RU_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_DEADLETTER_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_DEADLETTER_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_DEADLETTER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_DEADLETTER_JOB`
--

LOCK TABLES `ACT_RU_DEADLETTER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_ENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_RU_ENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_ENTITYLINK`
--

LOCK TABLES `ACT_RU_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EVENT_SUBSCR`
--

DROP TABLE IF EXISTS `ACT_RU_EVENT_SUBSCR`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EVENT_SUBSCR` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_IDX_EVENT_SUBSCR_EXEC_ID` (`EXECUTION_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_PROC_ID` (`PROC_INST_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EVENT_SUBSCR`
--

LOCK TABLES `ACT_RU_EVENT_SUBSCR` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EXECUTION`
--

DROP TABLE IF EXISTS `ACT_RU_EXECUTION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EXECUTION` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EXECUTION`
--

LOCK TABLES `ACT_RU_EXECUTION` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EXTERNAL_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_EXTERNAL_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EXTERNAL_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EXTERNAL_JOB`
--

LOCK TABLES `ACT_RU_EXTERNAL_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_HISTORY_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_HISTORY_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_HISTORY_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_HISTORY_JOB`
--

LOCK TABLES `ACT_RU_HISTORY_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_IDENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_RU_IDENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `ACT_RU_TASK` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_IDENTITYLINK`
--

LOCK TABLES `ACT_RU_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_JOB`
--

LOCK TABLES `ACT_RU_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_SUSPENDED_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_SUSPENDED_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_SUSPENDED_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_SUSPENDED_JOB`
--

LOCK TABLES `ACT_RU_SUSPENDED_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_TASK`
--

DROP TABLE IF EXISTS `ACT_RU_TASK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_TASK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_TASK`
--

LOCK TABLES `ACT_RU_TASK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TASK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TASK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_TIMER_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_TIMER_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_TIMER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_TIMER_JOB`
--

LOCK TABLES `ACT_RU_TIMER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_VARIABLE`
--

DROP TABLE IF EXISTS `ACT_RU_VARIABLE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_VARIABLE` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_VARIABLE`
--

LOCK TABLES `ACT_RU_VARIABLE` WRITE;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_CHANNEL_DEFINITION`
--

DROP TABLE IF EXISTS `FLW_CHANNEL_DEFINITION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_CHANNEL_DEFINITION` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IMPLEMENTATION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_CHANNEL_DEFINITION`
--

LOCK TABLES `FLW_CHANNEL_DEFINITION` WRITE;
/*!40000 ALTER TABLE `FLW_CHANNEL_DEFINITION` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_CHANNEL_DEFINITION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_DEFINITION`
--

DROP TABLE IF EXISTS `FLW_EVENT_DEFINITION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_DEFINITION` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_EVENT_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_DEFINITION`
--

LOCK TABLES `FLW_EVENT_DEFINITION` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_DEFINITION` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_DEFINITION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_DEPLOYMENT`
--

DROP TABLE IF EXISTS `FLW_EVENT_DEPLOYMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_DEPLOYMENT` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_DEPLOYMENT`
--

LOCK TABLES `FLW_EVENT_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_DEPLOYMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_RESOURCE`
--

DROP TABLE IF EXISTS `FLW_EVENT_RESOURCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_RESOURCE` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_EVENT_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `FLW_FK_EVENT_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `FLW_EVENT_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_RESOURCE`
--

LOCK TABLES `FLW_EVENT_RESOURCE` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_RESOURCE` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_RESOURCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_RU_BATCH`
--

DROP TABLE IF EXISTS `FLW_RU_BATCH`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_RU_BATCH` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BATCH_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_RU_BATCH`
--

LOCK TABLES `FLW_RU_BATCH` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_RU_BATCH_PART`
--

DROP TABLE IF EXISTS `FLW_RU_BATCH_PART`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_RU_BATCH_PART` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `BATCH_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESULT_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_BATCH_PART` (`BATCH_ID_`),
  CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `FLW_RU_BATCH` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_RU_BATCH_PART`
--

LOCK TABLES `FLW_RU_BATCH_PART` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-08 14:27:29
