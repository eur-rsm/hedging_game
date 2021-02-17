package nl.rsm.powertac.util;

public class Constants
{
  public class DB
  {
    public static final String SELECT_USER =
        "SELECT * FROM users WHERE username = ? ;";

    public static final String SELECT_USERS =
        "SELECT * FROM users ORDER BY username ;";

    public static final String INSERT_USER =
        "INSERT INTO users (username, password, salt) VALUES (?, ?, ?) ;";

    public static final String INSERT_USER_PLANT =
        "INSERT INTO userPlants (userId, plantId) VALUES (?, ?);";
    
    public static final String CLEAR_USER_PLANTS =
        "DELETE FROM userPlants WHERE userId = ?";
    
    public static final String SELECT_PLANT_TYPE =
        "SELECT * FROM plantTypes WHERE typeId = ?;";

    public static final String SELECT_PLANT_TYPES =
        "SELECT * FROM plantTypes WHERE active=1";
    
    public static final String SELECT_USER_PLANTS =
        "SELECT * FROM userPlants"
        + " JOIN plants ON userPlants.plantId = plants.plantId"
        + " WHERE userId = ? AND gameId = ?;";
    
    public static final String SELECT_USER_PLANTS_BY_GAME =
        "SELECT * FROM `plants`"
        + " JOIN userPlants ON plants.plantId = userPlants.plantId"
        + " JOIN users ON userPlants.userId = users.userId"
        + " WHERE plants.gameId = ?";
    
    public static final String ASSIGN_FREE_PLANTS =
        "INSERT INTO userPlants"
        + " SELECT ?, plants.plantId FROM plants"
        + " LEFT JOIN userPlants ON plants.plantId = userPlants.plantId"
        + " WHERE gameId = ? AND userPlants.plantId IS NULL"
        + " GROUP BY typeId"
        + " LIMIT ?";
    
    public static final String SELECT_PLANT =
        "SELECT * FROM plants WHERE plantId = ?";
        
    public static final String SELECT_GAME_PLANTS =
        "SELECT * FROM plants WHERE gameId = ?";
    
    public static final String DELETE_PLANTS =
        "DELETE FROM plants WHERE gameId = ?";
    
    public static final String INSERT_PLANT =
        "INSERT INTO plants ( gameId, typeId ) VALUES ( ?, ? );";
    
    public static final String UPDATE_PLANT =
        "UPDATE plants SET gameId = ?, typeId = ? WHERE plantId = ?;";
    
    public static final String SELECT_METRIC =
        "SELECT * FROM metrics WHERE metricId = ?;";

    public static final String SELECT_METRICS =
        "SELECT * FROM metrics;";
    
    public static final String SELECT_PLANTMETRICS =
        "SELECT * FROM plantMetrics"
        + " JOIN metrics ON plantMetrics.metricId = metrics.metricId"
        + " WHERE typeId = ?;";
        
    public static final String SELECT_FORECASTS =
        "SELECT * FROM forecasts " +
            "WHERE gameId = ? AND userId = ? AND round = ? AND period <= ?;";

    public static final String INSERT_FORECAST =
        "INSERT INTO forecasts (gameId, userId, plantId, metricId, round, period, value)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE value = ?;";


    public static final String SELECT_HEDGES =
        "SELECT * FROM hedges " +
            "WHERE gameId = ? AND userId = ? AND (round < ? OR round = ? AND period <= ?);";
    
    public static final String SELECT_HEDGES_PER_METRIC =
        "SELECT * FROM hedges JOIN metrics ON hedges.metricId = metrics.metricId"
            + " WHERE gameId = ? AND hedges.metricId = ? AND round = ? AND ABS(period) = ?"
            + " ORDER BY multiplier*bidP*? ASC;";

    public static final String INSERT_HEDGE =
        "INSERT INTO hedges (gameId, userId, plantId, metricId, round, period"
        + ", bidQ, bidP, clearQ, clearP, skip)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE bidQ = ?, bidP = ?, clearQ = ?, clearP = ?, skip = ?";

    public static final String SELECT_POSITIONS =
        "SELECT * FROM positions " +
            "WHERE gameId = ? AND userId = ? AND round = ? AND period < ?;";
    
    public static final String SELECT_POSITION =
        "SELECT * FROM positions " +
            "WHERE gameId = ? AND userId = ? AND plantId = ? AND metricId = ? " +
            "AND round = ? AND period = ?;";
    
    public static final String UPDATE_POSITIONS =
        "INSERT positions"
        + " SELECT gameId, userId, plantId, metricId, ?, ?, SUM(clearQ)"
        + " FROM hedges WHERE gameId = ? AND round = ? AND period <= ?"
        + " GROUP BY userId, plantId, metricId";
    
    // TODO need to check per metric
    public static final String MISSING_USERS =
        "SELECT * FROM users " +
            "WHERE users.gameId = ? AND (users.userId NOT IN (" +
            "SELECT forecasts.userId FROM forecasts " +
            "WHERE gameId = ? AND round = ? AND period = ?) " +
            "OR users.userId NOT IN (" +
            "SELECT hedges.userId FROM hedges WHERE gameId = ? AND round = ? AND period = ?)) ;" ;
    
    public static final String SELECT_MARKET =
        "SELECT * FROM market WHERE metricId = ? AND period = ?;"; 
    
    public static final String SELECT_MARKETS =
        "SELECT * FROM market WHERE gameId = ? AND round = ? AND period >= ? AND period <= ?;";
    
    public static final String SELECT_MARKETS_BY_GAME =
        "SELECT * FROM market WHERE gameId = ?"
        + " ORDER BY round ASC, period ASC;";
    
    public static final String INSERT_MARKETS =
        "INSERT INTO market ( gameId, metricId, round, period, price, `load`, `cleared`, forecast, sigma )"
        + " VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ? )"
        + " ON DUPLICATE KEY UPDATE price = ?, `load` = ?, `cleared` = ?, forecast = ?, sigma = ?;";
    
    public static final String INSERT_GAME =
        "INSERT INTO `games` ( `name`, `stamp`, `rounds`, `periods`, `plants` "
        + ", `currentRound`, `currentPeriod`, `active`, `showMarketInfo`, `showCostInfo`, `minPrice`, `maxPrice` ) VALUES"
        + " ( ?, CURRENT_TIMESTAMP(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
    
    public static final String UPDATE_GAME =
        "UPDATE `games` SET `currentRound` = ?, `currentPeriod` = ? WHERE `gameId` = ?;";
    
    public static final String SELECT_GAME =
        "SELECT * FROM games WHERE gameId = ?";
    
    public static final String SELECT_ACTIVE_GAME =
        "SELECT * FROM games WHERE active = 1 ORDER BY stamp DESC LIMIT 1";
    
    public static final String SET_ACTIVE_GAME =
        "UPDATE games SET active = IF(gameId = ?, 1, 0)";
    
    public static final String SELECT_ALL_GAMES =
        "SELECT * FROM games ORDER BY gameId DESC";
    
    public static final String SELECT_GAME_USERS =
        "SELECT * FROM users WHERE gameId = ?";
    
    public static final String SELECT_GAME_PERIODS =
        "SELECT * FROM periods WHERE gameId = ?";

    public static final String INSERT_GAME_PERIODS =
        "INSERT periods ( gameId, period, name) VALUES (?, ?, ?)";
    
    public static final String UPDATE_USER_GAME =
        "UPDATE users SET gameId = ? WHERE userId = ?";
    
    public static final String TOUCH_USER =
        "UPDATE users SET stamp = CURRENT_TIMESTAMP() WHERE userId = ?";
    
    public static final String INACTIVE_TIME =
        "SELECT UNIX_TIMESTAMP() - UNIX_TIMESTAMP( stamp ) FROM users WHERE userId = ?";
    
    public static final String SELECT_EVENTS =
        "SELECT * FROM events WHERE gameId = ? AND enabled = 1";
    
    public static final String INSERT_EVENT =
        "INSERT events ( gameId, round, period, delay, title, content ) VALUES ( ?, ?, ?, ?, ?, ? )";
    
    public static final String UPDATE_EVENT =
        "UPDATE events SET gameId = ?, round = ?, period = ?, delay = ?, content = ? WHERE eventId = ?";
    
    public static final String SELECT_PLANT_TYPE_PARAMS =
        "SELECT * FROM typeParams WHERE typeId = ? ORDER BY paramId ASC";
    
    public static final String SELECT_PLANT_PARAMS =
        "SELECT * FROM plantParams"
        + " JOIN typeParams ON plantParams.paramId=typeParams.paramId"
        + " WHERE plantId = ? ORDER BY plantParams.paramId ASC";
    
    public static final String INSERT_PLANT_PARAMS =
        "INSERT `plantParams` (`plantId`, `paramId`, `value`) VALUES ( ?, ?, ? )"
        + " ON DUPLICATE KEY UPDATE `value` = ?";
  }
  
  private static final Properties properties = new Properties();
  
  public static final int MAX_INACTIVE_SECONDS = properties.getPropertyInt( "max_inactive_seconds", 60*60 ); 
  
  public static final boolean SHOW_FORECASTING = properties.getPropertyBool( "show_forecasting", false );
  
  public static final boolean LIMIT_FORECASTING_ONOFF = properties.getPropertyBool( "limit_forecasting_onoff", true );
  
  public static final boolean LIMIT_HEDGING_POSITION = properties.getPropertyBool( "limit_hedging_position", false );
  
  public static final boolean LIMIT_HEDGING_SIGN = properties.getPropertyBool( "limit_hedging_sign", false );
  
  public static final boolean LIMIT_HEDGING_PRICE = true;
  
  public static final int MINIMUM_HEDGING_PRICE = -300;
  public static final int MAXIMUM_HEDGING_PRICE = 3000;
  
  public static final String[] PERIOD_NAMES = { "T0", "Future", "Spot", "T3" };
  
  
}
