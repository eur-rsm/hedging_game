-- phpMyAdmin SQL Dump
-- version 4.0.10deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Nov 28, 2014 at 05:59 PM
-- Server version: 5.5.40-0ubuntu0.14.04.1
-- PHP Version: 5.5.9-1ubuntu4.5

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


-- --------------------------------------------------------

--
-- Table structure for table `forecasts`
--

CREATE TABLE IF NOT EXISTS `forecasts` (
  `gameId` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `plantId` int(5) NOT NULL,
  `metricId` int(5) NOT NULL,
  `round` int(2) NOT NULL,
  `period` int(2) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY ( `gameId`, `userId`, `plantId`, `metricId`, `round`, `period` )
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `forecasts` ( `gameId`, `userId`, `plantId`, `metricId`, `round`
, `period`,`value` ) VALUES
  ( 0, 0, 1, 1, 0, 0, 500 ),
  ( 0, 0, 2, 1, 0, 0, 2000 ),
  ( 0, 0, 3, 1, 0, 0, 5000 ),
  ( 0, 0, 3, 2, 0, 0, -2000 ),
  ( 0, 0, 4, 1, 0, 0, 5000 ),
  ( 0, 0, 4, 3, 0, 0, -10000 ),
  ( 0, 0, 5, 1, 0, 0, 2500 ),
  ( 0, 0, 5, 2, 0, 0, -5000 ),
  ( 0, 0, 5, 3, 0, 0, -5000 )
;


-- --------------------------------------------------------

--
-- Table structure for table `hedges`
--

CREATE TABLE IF NOT EXISTS `hedges` (
  `gameId` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `plantId` int(5) NOT NULL,
  `metricId` int(5) NOT NULL,
  `round` int(2) NOT NULL,
  `period` int(2) NOT NULL,
  `bidQ` int(11) NOT NULL,
  `bidP` int(11) NOT NULL,
  `clearQ` int(11) NOT NULL,
  `clearP` int(11) NOT NULL,
  `skip` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY ( `gameId`, `userId`, `plantId`, `metricId`, `round`, `period` )
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



-- --------------------------------------------------------

--
-- Table structure for table `positions`
--

CREATE TABLE IF NOT EXISTS `positions` (
  `gameId` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `plantId` int(5) NOT NULL,
  `metricId` int(5) NOT NULL,
  `round` int(2) NOT NULL,
  `period` int(2) NOT NULL,
  `value` int(11) NOT NULL,
  PRIMARY KEY ( `gameId`, `userId`, `plantId`, `metricId`, `round`, `period` )
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `userId` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(45) NOT NULL,
  `password` varchar(45) NOT NULL,
  `salt` varchar(45) NOT NULL,
  `stamp` timestamp NOT NULL DEFAULT 0,
  `gameId` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=6;


INSERT INTO `users` (`userId`, `username`, `password`, `salt` ) VALUES
(1, 'admin', 'c2fd758a4bb19676af65d089bbd3ba03', 'c4e96e5dbd713847f8516addef4698aa' ),
(2, 'observer', '3456293e7d414714153662ad0914b627', '0ca25f5f5bc2a09036c3e2e1f21549da'),
(3, 'guest', 'd7269c4cba088db7085e1b4b0cdbfa79', 'ff6f303a434148ba768b6f230714429e' ),
(4, 'gbuijs', 'c0144b63ad834e71b98ef55ba6afa11a', '554e0efd5e14e18963227cb87aa8a15f' ),
(5, 'dkoolen', 'd7269c4cba088db7085e1b4b0cdbfa79', 'ff6f303a434148ba768b6f230714429e' ),
(6, 'ekemperman', '3c9eb9f0106997e6843eb5eb9cfc1e78', 'fd5b3626e6503691d8934d10079adf54'),
(7, 'erikk', 'f1b2802cb7773c0b45ce30587e14d79c', '3091a1f11379e91e02d9e4cb15b73e7'),
(8, 'derckk', '9f3aabe26750a185a2311e35899fdcc7', 'cee5995e3b470d9f0c58102fd5ae7444');

-- --------------------------------------------------------

--
-- Table structure for table `userPlants`
--

CREATE TABLE IF NOT EXISTS `userPlants` (
  `userId` int(11) NOT NULL,
  `plantId` int(5) NOT NULL,
  UNIQUE KEY( `userId`, `plantId` )
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;


-- --------------------------------------------------------

--
-- Table structure for table `plantTypes`
--

CREATE TABLE IF NOT EXISTS `plantTypes` (
  `typeId` int(5) NOT NULL AUTO_INCREMENT,
  `typeName` varchar(15) NOT NULL,
  `active` int(1) NOT NULL,
  PRIMARY KEY (`typeId`),
  UNIQUE KEY (`typeName`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=5;

INSERT INTO `plantTypes` ( `typeId`, `typeName`, `active` ) VALUES
( 1, 'Solar Plant', 1 ),
( 2, 'Wind Plant', 1 ),
( 3, 'Coal Plant', 1 ),
( 4, 'Gas Plant', 1 ),
( 5, 'Hybrid Plant', 0 )
;

CREATE TABLE IF NOT EXISTS `plants` (
  `plantId` int(5) NOT NULL AUTO_INCREMENT,
  `typeId` int(5) NOT NULL,
  `gameId` int(5) NOT NULL,
  PRIMARY KEY( `plantId` )
);

INSERT INTO `plants` ( `plantId`, `typeId`, `gameId` ) VALUES
  ( 1, 1, 1 )
  , (2, 2, 1 )
  , (3, 3, 1 )
  , (4, 4, 1 )
;


-- --------------------------------------------------------

--
-- Table structure for table `metrics`
--


CREATE TABLE IF NOT EXISTS `metrics` (
  `metricId` int(5) NOT NULL AUTO_INCREMENT,
  `metricName` varchar(15) NOT NULL,
  `unit` varchar(15) NOT NULL,
  `multiplier` int(5) NOT NULL,
  `priceUnit` varchar(15),
  `priceMultiplier` int(10),
  `spreadName` varchar(15),
  `spreadMultiplier` real(2,1),
  `spreadUnit` varchar(15),
  PRIMARY KEY ( `metricId` ),
  UNIQUE KEY ( `metricName` )
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=3;

INSERT INTO `metrics` ( `metricId`, `metricName`, `unit`, `multiplier`
, `priceUnit`, `priceMultiplier`
, `spreadName`, `spreadMultiplier`, `spreadUnit` ) VALUES
  ( 1, 'Power', 'MWh', 1, '€ / MWh', 1, NULL, 0, '€ / MWh' ),
  ( 2, 'Coal', 'kTon', -1, '€ / kTon', 1, 'Dark Spread', 0.4, '€ / MWh' ),
  ( 3, 'Gas', 'MWh', -1, '€ / MWh', 1, 'Spark Spread', 2.0, '€ / MWh' )
;
-- TODO Units for gas should be volume? Or maybe all units should be MWh ? --



-- --------------------------------------------------------

--
-- Table structure for table `plantMetrics`
--

CREATE TABLE IF NOT EXISTS `plantMetrics` (
  `typeId` int(5) NOT NULL,
  `metricId` int(5) NOT NULL,
  UNIQUE KEY ( `typeId`, `metricId` )
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=9;

INSERT INTO `plantMetrics` ( `typeId`, `metricId` ) VALUES
  ( 1, 1 ),
  ( 2, 1 ),
  ( 3, 1 ),
  ( 4, 1 ),
  ( 5, 1 )
;


-- --------------------------------------------------------

--
-- Table structure for table `market`
--

CREATE TABLE IF NOT EXISTS `market` (
  `gameId` int(11) NOT NULL,
  `metricId` int(5) NOT NULL,
  `round` int(2) NOT NULL,
  `period` int(2) NOT NULL,
  `price` double NOT NULL,
  `load` double NOT NULL,
  `cleared` int(11) NOT NULL,
  `forecast` double NOT NULL,
  `sigma` double NOT NULL,
  PRIMARY KEY( `gameId`, `metricId`, `round`, `period` )
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=3;


INSERT INTO `market` ( `gameId`, `metricId`, `round`, `period`, `price`, `load`, `forecast`, `sigma` ) VALUES
  ( 1, 1, 1, 1, -1, 8765, 9876, 7.3 ),
  ( 1, 1, 1, 2, -1, 8764, 9875, 7.3 ),
  ( 1, 1, 2, 1, -1, 8763, 9874, 7.3 ),
  ( 1, 1, 2, 2, -1, 8762, 9873, 7.3 ),
  ( 1, 1, 3, 1, -1, 8761, 9872, 7.4 ),
  ( 1, 1, 3, 2, -1, 8760, 9871, 7.5 ),
  ( 1, 1, 4, 1, -1, 8759, 9870, 7.6 ),
  ( 1, 1, 4, 2, -1, 8758, 9869, 7.7 )
;



--
-- Table structure for table `typeParams`
--

CREATE TABLE IF NOT EXISTS `typeParams` (
  `paramId` int(11) NOT NULL AUTO_INCREMENT,
  `typeId` int(5) NOT NULL,
  `name` varchar(31) NOT NULL,
  `type` tinyint NOT NULL,
  `value` varchar(31) NOT NULL,
  `auto` bit(1) NOT NULL,
  PRIMARY KEY( `paramId` )
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;


INSERT INTO `typeParams` ( `typeId`, `name`, `type`, `value`, `auto` ) VALUES
  ( 1, 'Output Std.Dev. (MWh)', 1, 50, 0 ),
  ( 2, 'Output Std.Dev. (MWh)', 1, 50, 0 ),
  ( 1, 'Real Output (MWh)', 1, 0, 1 ),
  ( 2, 'Real Output (MWh)', 1, 0, 1 ),
  ( 3, 'Marginal Cost (€ / MWh)', 1, 50, 0 ),
  ( 4, 'Marginal Cost (€ / MWh)', 1, 50, 0 ),
  ( 3, 'Ramping Cost (€ / MWh)', 1, 50, 0 ),
  ( 4, 'Ramping Cost (€ / MWh)', 1, 50, 0 )
;


--
-- Table structure for table `plantParams`
--

CREATE TABLE IF NOT EXISTS `plantParams` (
  `plantId` int(11) NOT NULL,
  `paramId` int(11) NOT NULL,
  `value` varchar(31) NOT NULL,
  PRIMARY KEY ( `plantId`, `paramId` )
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

INSERT INTO `plantParams` ( `plantId`, `paramId`, `value` ) VALUES
  ( 1, 1, '50' ),
  ( 2, 2, '50' ),
  ( 3, 5, '50' ),
  ( 4, 6, '50' )
;


-- --------------------------------------------------------

--
-- Table structure for table `games`
--

-- Patch to split `showPlantInfo` into two components:

-- ALTER TABLE  `games` ADD  `showMarketInfo` INT( 1 ) NOT NULL ;
-- ALTER TABLE  `games` ADD  `showCostInfo` INT( 1 ) NOT NULL ;
-- UPDATE `games` SET `showMarketInfo` = `showPlantInfo`, `showCostInfo` = `showPlantInfo` ;
-- ALTER TABLE  `games` DROP  `showPlantInfo` ;


CREATE TABLE IF NOT EXISTS `games` (
  `gameId` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(63) NOT NULL,
  `stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `rounds` int(2) NOT NULL,
  `periods` int(2) NOT NULL,
  `plants` int(2) NOT NULL,
  `active` int(1) NOT NULL,
  `currentRound` int(2) NOT NULL,
  `currentPeriod` int(2) NOT NULL,
  `showMarketInfo` int(1) NOT NULL,
  `showCostInfo` int(1) NOT NULL,
  `minPrice` int(11) NOT NULL,
  `maxPrice` int(11) NOT NULL,
  PRIMARY KEY (`gameId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;

INSERT INTO `games` ( `gameId`, `name`, `stamp`, `rounds`, `periods`, `plants`
, `active`, `currentRound`, `currentPeriod`, `showMarketInfo`, showCostInfo, `minPrice`, `maxPrice` ) VALUES
  ( 1, 'First Game', CURRENT_TIMESTAMP(), 4, 2, 4, 1, 0, 0, 0, 0, -300, 3000 )
;

CREATE TABLE IF NOT EXISTS `periods` (
  `gameId` int(11) NOT NULL,
  `period` int(2) NOT NULL,
  `name` varchar(31) NOT NULL,
  PRIMARY KEY( `gameId`, `period` )
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- --------------------------------------------------------

--
-- Table structure for table `events`
--

CREATE TABLE IF NOT EXISTS `events` (
  `eventId` int(11) NOT NULL AUTO_INCREMENT,
  `gameId` int(11) NOT NULL,
  `round` int(2) NOT NULL,
  `period` int(2) NOT NULL,
  `delay` int(5) NOT NULL,
  `title` varchar(31),
  `content` varchar(255),
  `enabled` int(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`eventId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=7;

INSERT INTO `events` ( `eventId`, `gameId`, `round`, `period`, `delay`, `title`, `content`, `enabled` ) VALUES
  ( 1, 0, 0, 0, 0, 'event', 'Dummy Event', 0 ),
  ( 2, 0, 0, 1, 180, 'event', 'Break through in concentrated solar power technology', 0 ),
  ( 3, 0, 0, 1, 360, 'event', 'The merge of GDF Suez with International Power', 0 ),
  ( 4, 0, 0, 2, 180, 'event', 'Expected 2014 Chinese GDP revised from 8% to 2%', 0 ),
  ( 5, 0, 0, 2, 360, 'event', 'Brand new wind blade technology', 0 ),
  ( 6, 0, 0, 3, 100, 'event', 'Heavy rain falling without interruption in Indonesia', 0 ),
  ( 7, 0, 0, 3, 200, 'event', 'Coming winter to be expected the coldest one in 50 years', 0 )
;

