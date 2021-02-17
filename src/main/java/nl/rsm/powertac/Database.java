package nl.rsm.powertac;

import nl.rsm.powertac.model.Event;
import nl.rsm.powertac.model.Forecast;
import nl.rsm.powertac.model.Game;
import nl.rsm.powertac.model.Hedge;
import nl.rsm.powertac.model.Market;
import nl.rsm.powertac.model.Metric;
import nl.rsm.powertac.model.Parameter;
import nl.rsm.powertac.model.Plant;
import nl.rsm.powertac.model.PlantType;
import nl.rsm.powertac.model.Position;
import nl.rsm.powertac.model.User;
import nl.rsm.powertac.util.Constants;
import nl.rsm.powertac.util.PlantDef;
import nl.rsm.powertac.util.Properties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import com.mysql.jdbc.Statement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


@ManagedBean
@RequestScoped
public class Database
{
  private Connection conn = null;

  private PreparedStatement selectUser = null;
  private PreparedStatement selectUsers = null;
  private PreparedStatement insertUser = null;
  private PreparedStatement clearUserPlants = null;
  private PreparedStatement insertUserPlant = null;
  private PreparedStatement assignUser = null;
  private PreparedStatement touchUser = null;
  private PreparedStatement inactiveTime = null;
  private PreparedStatement selectPlant = null;
  private PreparedStatement insertPlant = null;
  private PreparedStatement updatePlant = null;
  private PreparedStatement selectUserPlants = null;
  private PreparedStatement selectUserPlantsByGame = null;
  private PreparedStatement assignFreePlants = null;
  private PreparedStatement selectGamePlants = null;
  private PreparedStatement clearGamePlants = null;
  private PreparedStatement selectAllPlants = null;
  private PreparedStatement selectMetric = null;
  private PreparedStatement selectMetrics = null;
  private PreparedStatement selectPlantMetrics = null;
  private PreparedStatement insertForecast = null;
  private PreparedStatement selectForecasts = null;
  private PreparedStatement insertHedge = null;
  private PreparedStatement selectHedges = null;
  private PreparedStatement selectHedgesPerMetric = null;
  private PreparedStatement selectPositions = null;
  private PreparedStatement selectPosition = null;
  private PreparedStatement updatePositions = null;
  private PreparedStatement selectMissing = null;
  private PreparedStatement selectMarkets = null;
  private PreparedStatement selectMarketsByGame = null;
  private PreparedStatement insertMarkets = null;
  private PreparedStatement selectGame = null;
  private PreparedStatement updateGame = null;
  private PreparedStatement selectActiveGame = null;
  private PreparedStatement setActiveGame = null;
  private PreparedStatement selectAllGames = null;
  private PreparedStatement selectGameUsers = null;
  private PreparedStatement selectGamePeriods = null;
  private PreparedStatement insertGamePeriods = null;
  private PreparedStatement insertGame = null;
  private PreparedStatement selectEvents = null;
  private PreparedStatement insertEvent = null;
  private PreparedStatement updateEvent = null;
  private PreparedStatement selectPlantTypeParams = null;
  private PreparedStatement selectPlantParams = null;
  private PreparedStatement insertPlantParams = null;
  
  private Properties properties = new Properties();

  
  public Database ()
  {
    checkDb();
  }

  private void checkDb ()
  {
    try {
      if (conn == null || conn.isClosed()) {
        if (properties.getProperty("db.dbms").equalsIgnoreCase("mysql")) {
          try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String url = String.format("jdbc:%s://%s/%s",
                properties.getProperty("db.dbms"),
                properties.getProperty("db.dbUrl"),
                properties.getProperty("db.database"));
            conn = DriverManager.getConnection(url,
                properties.getProperty("db.username"),
                properties.getProperty("db.password"));
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void close ()
  {
    try {
      if (conn != null && !conn.isClosed()) {
        conn.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public User getUser (String username) throws SQLException
  {
    if (selectUser == null || selectUser.isClosed()) {
      selectUser = conn.prepareStatement(Constants.DB.SELECT_USER);
    }

    selectUser.setString(1, username);

    ResultSet resultSet = selectUser.executeQuery();

    if (!resultSet.next()) {
      return null;
    }

    return new User(resultSet);
  }

  public List<User> getUsers () throws SQLException
  {
    if (selectUsers == null || selectUsers.isClosed()) {
      selectUsers = conn.prepareStatement(Constants.DB.SELECT_USERS);
    }

    ResultSet resultSet = selectUsers.executeQuery();

    List<User> users = new LinkedList<User>();
    while (resultSet.next()) {
      User user = new User(resultSet);
      users.add(user);
    }
    return users;
  }

  public void saveUser (User user) throws SQLException
  {
    if (insertUser == null || insertUser.isClosed()) {
      insertUser = conn.prepareStatement(Constants.DB.INSERT_USER, Statement.RETURN_GENERATED_KEYS );
    }

    insertUser.setString(1, user.getUsername());
    insertUser.setString(2, user.getPassword());
    insertUser.setString(3, user.getSalt());
    
    int rows = insertUser.executeUpdate();
    if ( rows == 0 ) {
      throw new SQLException( "user wasn't inserted!" );
    }
    
    ResultSet rs = insertUser.getGeneratedKeys();
    if ( rs.next() ) {
      user.setUserId( rs.getInt( 1 ) );
    } else {
      throw new SQLException( "failed to obtain id for inserted user!" );
    }
  }
  
  
  public void saveUserPlants( User user ) throws SQLException {
    if ( clearUserPlants == null || clearUserPlants.isClosed()) {
      clearUserPlants = conn.prepareStatement( Constants.DB.CLEAR_USER_PLANTS );
    }
    clearUserPlants.setInt( 1, user.getUserId() );
    
    clearUserPlants.executeUpdate();
    
    if (insertUserPlant == null || insertUserPlant.isClosed()) {
      insertUserPlant = conn.prepareStatement( Constants.DB.INSERT_USER_PLANT );
    }
    
    for ( Plant plant : user.getPlants() ) {
      insertUserPlant.setInt( 1, user.getUserId() );
      insertUserPlant.setInt( 2, plant.getPlantId() );
      insertUserPlant.executeUpdate();
    }
  }
  
  public void assignUser( User user, Game game )
  throws SQLException {
    if (assignUser == null || assignUser.isClosed()) {
      assignUser = conn.prepareStatement( Constants.DB.UPDATE_USER_GAME );
    }
    
    assignUser.setInt( 1, game == null ? 0 : game.getGameId() );
    assignUser.setInt( 2, user.getUserId() );
    
    assignUser.executeUpdate();
  }

  public void touchUser( User user )
  throws SQLException {
    if (touchUser == null || touchUser.isClosed()) {
      touchUser = conn.prepareStatement( Constants.DB.TOUCH_USER );
    }
    
    touchUser.setInt( 1, user.getUserId() );
    
    touchUser.executeUpdate();
  }
  
  public long inactiveSeconds( User user )
  throws SQLException {
    if (inactiveTime == null || inactiveTime.isClosed()) {
      inactiveTime = conn.prepareStatement( Constants.DB.INACTIVE_TIME );
    }
    
    inactiveTime.setInt( 1, user.getUserId() );
    
    ResultSet resultSet = inactiveTime.executeQuery();
    
    return resultSet.next() ? resultSet.getLong( 1 ) : Long.MAX_VALUE;
  }
  
  public PlantType getPlantType( int plantId ) throws SQLException {
    if (selectPlant == null || selectPlant.isClosed()) {
      selectPlant = conn.prepareStatement( Constants.DB.SELECT_PLANT_TYPE );
    }
    
    selectPlant.setInt( 1, plantId );
    
    ResultSet resultSet = selectPlant.executeQuery();

    if (!resultSet.next()) {
      return null;
    }

    return new PlantType( resultSet ); 
  }
  
  public List<PlantType> getAllPlantTypes() throws SQLException {
    if (selectAllPlants == null || selectAllPlants.isClosed()) {
      selectAllPlants = conn.prepareStatement( Constants.DB.SELECT_PLANT_TYPES );
    }
    
    ResultSet resultSet = selectAllPlants.executeQuery();

    List<PlantType> plants = new LinkedList<PlantType>();
    while (resultSet.next()) {
      PlantType plant = new PlantType(resultSet);
      plants.add( plant );
    }
    return plants;
  }
  
  public Plant getPlantById( int plantId )
  throws SQLException {
    if (selectPlant == null || selectPlant.isClosed()) {
      selectPlant = conn.prepareStatement( Constants.DB.SELECT_PLANT );
    }
    
    selectPlant.setInt( 1, plantId );
    
    ResultSet resultSet = selectPlant.executeQuery();

    Plant plant = null;
    if (resultSet.next()) {
      plant = new Plant(resultSet);
    }
    return plant;
  }

  
  public Set<Plant> getPlantsByUser( User user ) throws SQLException {
    if (selectUserPlants == null || selectUserPlants.isClosed()) {
      selectUserPlants = conn.prepareStatement( Constants.DB.SELECT_USER_PLANTS );
    }
    
    selectUserPlants.setInt( 1, user.getUserId() );
    selectUserPlants.setInt( 2, user.getGameId() );
    
    ResultSet resultSet = selectUserPlants.executeQuery();

    Set<Plant> plants = new TreeSet<Plant>();
    while (resultSet.next()) {
      Plant plant = new Plant(resultSet);
      plants.add( plant );
    }
    return plants;
  }
  
  public Map<User, List<Plant>> getUserPlantsByGame( Game game )
  throws SQLException {
    if ( selectUserPlantsByGame == null || selectUserPlantsByGame.isClosed() ) {
      selectUserPlantsByGame = conn.prepareStatement( Constants.DB.SELECT_USER_PLANTS_BY_GAME );
    }
    
    selectUserPlantsByGame.setInt(  1,  game.getGameId() );
    ResultSet rs = selectUserPlantsByGame.executeQuery();
    
    Map<User, List<Plant>> plants = new LinkedHashMap<User, List<Plant>>();
    
    while ( rs.next() ) {
      Plant plant = new Plant( rs );
      User user = new User( rs );
      List<Plant> list = plants.get( user );
      if ( list == null ) {
        list = new LinkedList<Plant>();
        plants.put( user, list );
      }
      list.add( plant );
    }
    
    return plants;
  }
  
  public int assignFreePlants( User user, Game game )
  throws SQLException {
    if ( assignFreePlants == null || assignFreePlants.isClosed() ) {
      assignFreePlants = conn.prepareStatement( Constants.DB.ASSIGN_FREE_PLANTS );
    }
    
    assignFreePlants.setInt( 1, user.getUserId() );
    assignFreePlants.setInt( 2, game.getGameId() );
    assignFreePlants.setInt( 3, game.getNumPlants() );
    
    int rows = assignFreePlants.executeUpdate();
    if ( rows != game.getNumPlants() ) {
      System.out.println( "Failed to assign " + game.getNumPlants() + " plants!" );
    }
    
    return rows;
  }
  
  public Set<Plant> getPlantsByGame( Game game )
  throws SQLException {
    if (selectGamePlants == null || selectGamePlants.isClosed()) {
      selectGamePlants = conn.prepareStatement( Constants.DB.SELECT_GAME_PLANTS );
    }
    
    selectGamePlants.setInt( 1, game.getGameId() );
    
    ResultSet resultSet = selectGamePlants.executeQuery();

    Set<Plant> plants = new TreeSet<Plant>();
    while ( resultSet.next() ) {
      Plant plant = new Plant(resultSet);
      plants.add( plant );
    }
    return plants;
  }
  
  public void clearPlantSetup( Game game )
  throws SQLException {
    if (clearGamePlants == null || clearGamePlants.isClosed()) {
      clearGamePlants = conn.prepareStatement(Constants.DB.DELETE_PLANTS );
    }
    
    clearGamePlants.setInt( 1, game.getGameId() );
    
    clearGamePlants.executeUpdate();
  }
  
  public void setPlantSetup( Game game, List<PlantDef> defs, Map<Integer, List<List<Parameter>>> plantParams  )
  throws SQLException {
    clearPlantSetup( game );
    
    if (insertPlant == null || insertPlant.isClosed()) {
      insertPlant = conn.prepareStatement( Constants.DB.INSERT_PLANT, Statement.RETURN_GENERATED_KEYS );
    }
    
    insertPlant.setInt( 1, game.getGameId() );
    
    Map<Integer, List<Parameter>> params = new HashMap<Integer, List<Parameter>>();
    
    for ( PlantDef def : defs ) {
      insertPlant.setInt( 2, def.getType().getTypeId() );
      
      int rows = insertPlant.executeUpdate();
      if ( rows == 0 ) {
        throw new SQLException( "plant wasn't inserted!" );
      }
      int plantId = -1;
      ResultSet rs = insertPlant.getGeneratedKeys();
      if ( rs.next() ) {
        plantId = rs.getInt( 1 );
      } else {
        throw new SQLException( "failed to obtain id for inserted plant!" );
      }
      
      for ( Map.Entry<Metric, Integer> entry : def.getValues().entrySet() ) {
        Forecast forecast = new Forecast( game.getGameId(), 0, plantId
            , entry.getKey().getMetricId(), 0, 0 );
        forecast.setValue( entry.getValue() );
        saveForecast( forecast );
      }
      
      params.put( plantId, plantParams.get( def.getType().getTypeId() ).remove( 0 ) );
    }
    
    for ( Map.Entry<Integer, List<Parameter>> entry : params.entrySet() ) {
      setPlantParams( entry.getKey(), entry.getValue() );
    }
  }
  
  public Metric getMetric( int metricId ) throws SQLException {
    if (selectMetric == null || selectMetric.isClosed()) {
      selectMetric = conn.prepareStatement(Constants.DB.SELECT_METRIC );
    }
    
    selectMetric.setInt( 1, metricId );
    
    ResultSet resultSet = selectMetric.executeQuery();

    if (!resultSet.next()) {
      return null;
    }

    return new Metric( resultSet );
  }
  
  public List<Metric> getMetrics() throws SQLException {
    if (selectMetrics == null || selectMetrics.isClosed()) {
      selectMetrics = conn.prepareStatement(Constants.DB.SELECT_METRICS );
    }
    
    ResultSet resultSet = selectMetrics.executeQuery();
    
    List<Metric> metrics = new LinkedList<Metric>();
    while (resultSet.next()) {
      Metric metric = new Metric(resultSet);
      metrics.add( metric );
    }
    return metrics;
    
  }
  
  public List<Metric> getPlantMetrics( int typeId ) throws SQLException {
    if (selectPlantMetrics == null || selectPlantMetrics.isClosed()) {
      selectPlantMetrics = conn.prepareStatement( Constants.DB.SELECT_PLANTMETRICS );
    }
    
    selectPlantMetrics.setInt( 1, typeId );
    
    ResultSet resultSet = selectPlantMetrics.executeQuery();
    
    List<Metric> metrics = new LinkedList<Metric>();
    while (resultSet.next()) {
      Metric metric = new Metric(resultSet);
      metrics.add( metric );
    }
    return metrics;
    
  }
  

  public void saveForecast (Forecast forecast)
      throws SQLException
  {
    if (insertForecast == null || insertForecast.isClosed()) {
      insertForecast = conn.prepareStatement(Constants.DB.INSERT_FORECAST);
    }

    insertForecast.setInt(1, forecast.getGameId());
    insertForecast.setInt(2, forecast.getUserId());
    insertForecast.setInt(3, forecast.getPlantId());
    insertForecast.setInt(4, forecast.getMetricId());
    insertForecast.setInt(5, forecast.getRound());
    insertForecast.setInt(6, forecast.getPeriod());
    insertForecast.setInt(7, forecast.getValue());
    insertForecast.setInt(8, forecast.getValue());
    
    insertForecast.executeUpdate();
  }

  
  public Map<String, Forecast> getForecastsBootstrap( int gameId )
  throws SQLException {
    if (selectForecasts == null || selectForecasts.isClosed()) {
      selectForecasts = conn.prepareStatement(Constants.DB.SELECT_FORECASTS);
    }
    
    Map<String, Forecast> forecasts = new HashMap<String, Forecast>();
    
    selectForecasts.setInt( 1, gameId );
    selectForecasts.setInt( 2, 0 );
    selectForecasts.setInt( 3, 0 );
    selectForecasts.setInt( 4, 0 );
    
    ResultSet resultSet = selectForecasts.executeQuery();
    
    if ( ! resultSet.isBeforeFirst() ) {
      selectForecasts.setInt( 1, 0 );
      resultSet = selectForecasts.executeQuery();
    }
    
    while ( resultSet.next() ) {
      Forecast f = new Forecast(resultSet);
      forecasts.put( "p" + f.getPlantId() + "_m" + f.getMetricId(), f );
    }
    return forecasts;
  }
  
  
  public Map<Integer, Map<String, Forecast>> getForecasts (Game game, User user, int round, int period)
      throws SQLException
  {
    if (selectForecasts == null || selectForecasts.isClosed()) {
      selectForecasts = conn.prepareStatement(Constants.DB.SELECT_FORECASTS);
    }
    selectForecasts.setInt( 1, game.getGameId() );
    selectForecasts.setInt( 2, user.getUserId() );
    selectForecasts.setInt( 3, round );
    selectForecasts.setInt( 4, period );
    
    ResultSet resultSet = selectForecasts.executeQuery();

    Map<Integer, Map<String, Forecast>> forecasts 
    = new HashMap<Integer, Map<String, Forecast>>();
    
    while (resultSet.next()) {
      Forecast f = new Forecast(resultSet);
      int p = f.getPeriod();
      Map<String, Forecast> map = forecasts.get( p );
      if ( map == null ) {
        map = new HashMap<String, Forecast>();
        forecasts.put( p, map );
      }
      map.put( "p" + f.getPlantId() + "_m" + f.getMetricId(), f );
    }
    return forecasts;
  }

  
  public void saveHedge (Hedge hedge)
      throws SQLException
  {
    if (insertHedge == null || insertHedge.isClosed()) {
      insertHedge = conn.prepareStatement(Constants.DB.INSERT_HEDGE);
    }
    insertHedge.setInt(1, hedge.getGameId());
    insertHedge.setInt(2, hedge.getUserId());
    insertHedge.setInt(3, hedge.getPlantId());
    insertHedge.setInt(4, hedge.getMetricId());
    insertHedge.setInt(5, hedge.getRound());
    insertHedge.setInt(6, hedge.getPeriod());
    insertHedge.setInt(7, hedge.getBidQuantity());
    insertHedge.setInt(8, hedge.getBidPrice());
    insertHedge.setInt(9, hedge.getClearingQuantity());
    insertHedge.setInt(10, hedge.getClearingPrice());
    insertHedge.setInt(11, hedge.getSkip() ? 1 : 0);
    insertHedge.setInt(12, hedge.getBidQuantity());
    insertHedge.setInt(13, hedge.getBidPrice());
    insertHedge.setInt(14, hedge.getClearingQuantity());
    insertHedge.setInt(15, hedge.getClearingPrice());
    insertHedge.setInt(16, hedge.getSkip() ? 1 : 0);
    
    
    insertHedge.executeUpdate();
  }

  public Map<String, Map<String, Hedge>> getHedges( Game game, User user, int round, int period )
  throws SQLException {
    if (selectHedges == null || selectHedges.isClosed()) {
      selectHedges = conn.prepareStatement(Constants.DB.SELECT_HEDGES);
    }

    selectHedges.setInt( 1, game.getGameId() );
    selectHedges.setInt( 2, user.getUserId() );
    selectHedges.setInt( 3, round );
    selectHedges.setInt( 4, round );
    selectHedges.setInt( 5, period );

    ResultSet resultSet = selectHedges.executeQuery();

    Map<String, Map<String, Hedge>> hedges
    = new HashMap<String, Map<String, Hedge>>();
    
    while (resultSet.next()) {
      Hedge h = new Hedge(resultSet);
      int r = h.getRound();
      int p = h.getPeriod();
      String k = r + ":" + p;
      Map<String, Hedge> map = hedges.get( k );
      if ( map == null ) {
        map = new HashMap<String, Hedge>();
        hedges.put( k, map );
      }
      map.put( "p" + h.getPlantId() + "_m" + h.getMetricId(), h );
    }
    return hedges;
  }
  
  public List<Hedge> getHedgesPerMetric ( Game game, Metric metric, int round, int period, int sign )
  throws SQLException {
    if (selectHedgesPerMetric == null || selectHedgesPerMetric.isClosed()) {
      selectHedgesPerMetric = conn.prepareStatement(Constants.DB.SELECT_HEDGES_PER_METRIC);
    }

    selectHedgesPerMetric.setInt( 1, game.getGameId() );
    selectHedgesPerMetric.setInt( 2, metric.getMetricId() );
    selectHedgesPerMetric.setInt( 3, round );
    selectHedgesPerMetric.setInt( 4, period );
    selectHedgesPerMetric.setInt( 5, sign );
    
    ResultSet resultSet = selectHedgesPerMetric.executeQuery();

    List<Hedge> hedges = new LinkedList<Hedge>();
    
    while (resultSet.next()) {
      Hedge hedge = new Hedge(resultSet);
      hedges.add( hedge );
    }
    return hedges;
  }
  
  
  public Map<Integer, Map<String, Position>> getPositions ( Game game, User user, int round, int period )
  throws SQLException {
    if (selectPositions == null || selectPositions.isClosed()) {
      selectPositions = conn.prepareStatement(Constants.DB.SELECT_POSITIONS);
    }

    selectPositions.setInt( 1, game.getGameId() );
    selectPositions.setInt( 2, user.getUserId() );
    selectPositions.setInt( 3, round );
    selectPositions.setInt( 4, period );

    ResultSet resultSet = selectPositions.executeQuery();

    Map<Integer, Map<String, Position>> positions
    = new HashMap<Integer, Map<String, Position>>();
    
    while (resultSet.next()) {
      Position s = new Position(resultSet);
      int p = s.getPeriod();
      Map<String, Position> map = positions.get( p );
      if ( map == null ) {
        map = new HashMap<String, Position>();
        positions.put( p, map );
      }
      map.put( "p" + s.getPlantId() + "_m" + s.getMetricId(), s );
    }
    return positions;
  }
  
  public Position getPosition( Game game, User user, Plant plant, Metric metric, int round, int period )
  throws SQLException {
    if ( period == 0 ) {
      return new Position( game.getGameId(), user.getUserId()
          , plant.getPlantId(), metric.getMetricId(), round, period );
    }
    
    if (selectPosition == null || selectPosition.isClosed()) {
      selectPosition = conn.prepareStatement(Constants.DB.SELECT_POSITION);
    }
    
    selectPosition.setInt( 1, game.getGameId() );
    selectPosition.setInt( 2, user.getUserId() );
    selectPosition.setInt( 3, plant.getPlantId() );
    selectPosition.setInt( 4, metric.getMetricId() );
    selectPosition.setInt( 5, round );
    selectPosition.setInt( 6, period );
    
    Position position = null;
    
    ResultSet resultSet = selectPosition.executeQuery();
    if ( resultSet.next() ) {
      position = new Position( resultSet );
    }
    return position;
  }
  
  public void updatePositions( Game game, int round, int period )
  throws SQLException {
    if (updatePositions == null || updatePositions.isClosed()) {
      updatePositions = conn.prepareStatement(Constants.DB.UPDATE_POSITIONS);
    }

    updatePositions.setInt( 1, round );
    updatePositions.setInt( 2, period );
    updatePositions.setInt( 3, game.getGameId() );
    updatePositions.setInt( 4, round );
    updatePositions.setInt( 5, period );
    
    updatePositions.executeUpdate();
  }
  
  public Map<String, Market> getMarketsBootstrap( Game game )
  throws SQLException {
    if (selectMarkets == null || selectMarkets.isClosed()) {
      selectMarkets = conn.prepareStatement(Constants.DB.SELECT_MARKETS );
    }
    
    Map<String, Market> markets = new HashMap<String, Market>();
    
    selectMarkets.setInt( 1, game.getGameId() );
    selectMarkets.setInt( 2, game.getCurrentRound() );
    selectMarkets.setInt( 3, 0 );
    selectMarkets.setInt( 4, 0 );
    
    ResultSet resultSet = selectMarkets.executeQuery();
    
    if ( ! resultSet.isBeforeFirst() ) {
      selectMarkets.setInt( 1, 0 );
      resultSet = selectMarkets.executeQuery();
    }
    
    while ( resultSet.next() ) {
      Market market = new Market( resultSet );
      markets.put( "m" + market.getMetricId(), market );
    }
    
    return markets;
  }
  
  public Map<Integer, Map<String, Market>> getMarkets( Game game, int round, int maxPeriod )
      throws SQLException {
    return getMarkets( game, round, 0, maxPeriod );
  }
  
  public Map<Integer, Map<String, Market>> getMarkets( Game game, int round, int minPeriod, int maxPeriod )
  throws SQLException {
    if (selectMarkets == null || selectMarkets.isClosed()) {
      selectMarkets = conn.prepareStatement(Constants.DB.SELECT_MARKETS );
    }
    
    selectMarkets.setInt( 1, game.getGameId() );
    selectMarkets.setInt( 2, round );
    selectMarkets.setInt( 3, minPeriod );
    selectMarkets.setInt( 4, maxPeriod );
    
    ResultSet resultSet = selectMarkets.executeQuery();
    
    if ( ! resultSet.isBeforeFirst() ) {
      selectMarkets.setInt( 1, 0 );
      resultSet = selectMarkets.executeQuery();
    }
    
    Map<Integer, Map<String, Market>> markets = new HashMap<Integer, Map<String, Market>>();
    
    while ( resultSet.next() ) {
      Market market = new Market( resultSet );
      int p = market.getPeriod();
      Map<String, Market> map = markets.get( p );
      if ( map == null ) {
        map = new HashMap<String, Market>();
        markets.put( p, map );
      }
      map.put( "m" + market.getMetricId(), market );
    }
    
    return markets;
  }
  
  public List<Market> getMarkets( Game game )
  throws SQLException {
    if (selectMarketsByGame == null || selectMarketsByGame.isClosed()) {
      selectMarketsByGame = conn.prepareStatement(Constants.DB.SELECT_MARKETS_BY_GAME );
    }
    
    selectMarketsByGame.setInt( 1, game.getGameId() );
    
    ResultSet resultSet = selectMarketsByGame.executeQuery();
    
    List<Market> markets = new LinkedList<Market>();
    
    while ( resultSet.next() ) {
      markets.add( new Market( resultSet ) );
    }
    
    return markets;
  }


  
  public void saveMarket( Market market )
  throws SQLException {
    if (insertMarkets == null || insertMarkets.isClosed()) {
      insertMarkets = conn.prepareStatement(Constants.DB.INSERT_MARKETS );
    }
    
    insertMarkets.setInt( 1, market.getGameId() );
    insertMarkets.setInt( 2, market.getMetricId() );
    insertMarkets.setInt( 3, market.getRound() );
    insertMarkets.setInt( 4, market.getPeriod() );
    insertMarkets.setDouble( 5, market.getPrice() );
    insertMarkets.setDouble( 6, market.getLoad() );
    insertMarkets.setDouble( 7, market.getCleared() );
    insertMarkets.setDouble( 8, market.getForecast() );
    insertMarkets.setDouble( 9, market.getSigma() );
    insertMarkets.setDouble( 10, market.getPrice() );
    insertMarkets.setDouble( 11, market.getLoad() );
    insertMarkets.setDouble( 12, market.getCleared() );
    insertMarkets.setDouble( 13, market.getForecast() );
    insertMarkets.setDouble( 14, market.getSigma() );
    
    insertMarkets.executeUpdate();
  }
  
  public void saveMarkets( List<Market> markets )
  throws SQLException {
    if (insertMarkets == null || insertMarkets.isClosed()) {
      insertMarkets = conn.prepareStatement(Constants.DB.INSERT_MARKETS );
    }
    for ( Market market : markets ) {
      insertMarkets.setInt( 1, market.getGameId() );
      insertMarkets.setInt( 2, market.getMetricId() );
      insertMarkets.setInt( 3, market.getRound() );
      insertMarkets.setInt( 4, market.getPeriod() );
      insertMarkets.setDouble( 5, market.getPrice() );
      insertMarkets.setDouble( 6, market.getLoad() );
      insertMarkets.setDouble( 7, market.getCleared() );
      insertMarkets.setDouble( 8, market.getForecast() );
      insertMarkets.setDouble( 9, market.getSigma() );
      insertMarkets.setDouble( 10, market.getPrice() );
      insertMarkets.setDouble( 11, market.getLoad() );
      insertMarkets.setDouble( 12, market.getCleared() );
      insertMarkets.setDouble( 13, market.getForecast() );
      insertMarkets.setDouble( 14, market.getSigma() );
      insertMarkets.addBatch();
    }
    
    insertMarkets.executeBatch();
    insertMarkets.clearBatch();
  }
  
  
  
  // TODO need to change query to account for varying metrics?
  public List<User> getMissingUsers ( Game game, int round, int period ) throws SQLException
  {
    if (selectMissing == null || selectMissing.isClosed()) {
      selectMissing = conn.prepareStatement(Constants.DB.MISSING_USERS);
    }

    selectMissing.setInt( 1, game.getGameId() );
    selectMissing.setInt( 2, game.getGameId() );
    selectMissing.setInt( 3, round );
    selectMissing.setInt( 4, period );
    selectMissing.setInt( 5, game.getGameId() );
    selectMissing.setInt( 6, round );
    selectMissing.setInt( 7, period );

    ResultSet resultSet = selectMissing.executeQuery();

    List<User> users = new LinkedList<User>();
    while (resultSet.next()) {
      User user = new User(resultSet);
      if (!user.isAdmin()) {
        users.add(user);
      }
    }
    return users;
  }
  
  public void insertGame( Game game )
  throws SQLException {
    if (insertGame == null || insertGame.isClosed()) {
      insertGame = conn.prepareStatement(Constants.DB.INSERT_GAME, Statement.RETURN_GENERATED_KEYS);
    }
    
    insertGame.setString( 1, game.getName() );
    insertGame.setInt( 2, game.getNumRounds() );
    insertGame.setInt( 3, game.getNumPeriods() );
    insertGame.setInt( 4, game.getNumPlants() );
    insertGame.setInt( 5, game.getCurrentRound() );
    insertGame.setInt( 6, game.getCurrentPeriod() );
    insertGame.setInt( 7, 0 );
    insertGame.setInt( 8, game.getShowMarketInfo() ? 1 : 0 );
    insertGame.setInt( 9, game.getShowCostInfo() ? 1 : 0 );
    insertGame.setInt( 10, game.getMinPrice() );
    insertGame.setInt( 11, game.getMaxPrice() );
    
    int rows = insertGame.executeUpdate();
    if ( rows == 0 ) {
      throw new SQLException( "game wasn't inserted!" );
    }
    
    ResultSet rs = insertGame.getGeneratedKeys();
    if ( rs.next() ) {
      game.setGameId( rs.getInt( 1 ) );
    } else {
      throw new SQLException( "failed to obtain id for inserted game!" );
    }
    
  }
  
  public void updateGame( Game game )
  throws SQLException {
    if (updateGame == null || updateGame.isClosed()) {
      updateGame = conn.prepareStatement(Constants.DB.UPDATE_GAME, Statement.RETURN_GENERATED_KEYS);
    }
    
    updateGame.setInt( 1, game.getCurrentRound() );
    updateGame.setInt( 2, game.getCurrentPeriod() );
    updateGame.setInt( 3, game.getGameId() );
    
    int rows = updateGame.executeUpdate();
    if ( rows != 1 ) {
      throw new SQLException( "game wasn't updated!" );
    }
  }

  public Game getGame( int gameId )
  throws SQLException {
    if (selectGame == null || selectGame.isClosed()) {
      selectGame = conn.prepareStatement(Constants.DB.SELECT_GAME);
    }
    selectGame.setInt( 1, gameId );
    Game game = null;
    ResultSet resultSet = selectGame.executeQuery();
    if ( resultSet.next() ) {
      game = new Game( resultSet );
    }
    return game;
  }

  
  public Game getActiveGame()
  throws SQLException {
    if (selectActiveGame == null || selectActiveGame.isClosed()) {
      selectActiveGame = conn.prepareStatement(Constants.DB.SELECT_ACTIVE_GAME);
    }
    Game game = null;
    ResultSet resultSet = selectActiveGame.executeQuery();
    if ( resultSet.next() ) {
      game = new Game( resultSet );
    }
    return game;
  }
  
  public void setActiveGame( Game game )
  throws SQLException {
    if (setActiveGame == null || setActiveGame.isClosed()) {
      setActiveGame = conn.prepareStatement(Constants.DB.SET_ACTIVE_GAME);
    }
    setActiveGame.setInt( 1, game.getGameId() );
    setActiveGame.executeUpdate();
  }
  
  public List<Game> getAllGames()
  throws SQLException {
    if (selectAllGames == null || selectAllGames.isClosed()) {
      selectAllGames = conn.prepareStatement(Constants.DB.SELECT_ALL_GAMES);
    }
    List<Game> games = new LinkedList<Game>();
    ResultSet resultSet = selectAllGames.executeQuery();
    while ( resultSet.next() ) {
      games.add( new Game( resultSet ) );
    }
    return games;
  }
  
  public List<User> getGameUsers( Game game )
  throws SQLException {
    if (selectGameUsers == null || selectGameUsers.isClosed()) {
      selectGameUsers = conn.prepareStatement(Constants.DB.SELECT_GAME_USERS);
    }
    List<User> users = new LinkedList<User>();
    selectGameUsers.setInt( 1, game.getGameId() );
    ResultSet resultSet = selectGameUsers.executeQuery();
    if ( resultSet.isBeforeFirst() ) {
      while ( resultSet.next() ) {
        users.add( new User( resultSet ) );
      }
    }
    return users;
  }
  
  public SortedSet<Event> getEvents( Game game )
  throws SQLException {
    if (selectEvents == null || selectEvents.isClosed()) {
      selectEvents = conn.prepareStatement(Constants.DB.SELECT_EVENTS);
    }
    SortedSet<Event> events = new TreeSet<Event>();
    selectEvents.setInt( 1, game == null ? 0 : game.getGameId() );
    ResultSet resultSet = selectEvents.executeQuery();
    while ( resultSet.next() ) {
      events.add( new Event( resultSet ) );
    }
    return events;
  }
  
  public void insertEvent( Event event ) 
  throws SQLException {
    if (insertEvent == null || insertEvent.isClosed()) {
      insertEvent = conn.prepareStatement(Constants.DB.INSERT_EVENT, Statement.RETURN_GENERATED_KEYS);
    }
    
    insertEvent.setInt( 1, event.getGameId() );
    insertEvent.setInt( 2, event.getRound() );
    insertEvent.setInt( 3, event.getPeriod() );
    insertEvent.setInt( 4, event.getDelay() );
    insertEvent.setString( 5, event.getTitle() );
    insertEvent.setString( 6, event.getContent() );
    
    int rows = insertEvent.executeUpdate();
    if ( rows == 0 ) {
      throw new SQLException( "event wasn't inserted!" );
    }
    
    ResultSet rs = insertEvent.getGeneratedKeys();
    if ( rs.next() ) {
      event.setEventId( rs.getInt( 1 ) );
    } else {
      throw new SQLException( "failed to obtain id for inserted event!" );
    }
  }
  
  
  public void updateEvent( Event event ) 
      throws SQLException {
        if (updateEvent == null || updateEvent.isClosed()) {
          updateEvent = conn.prepareStatement(Constants.DB.UPDATE_EVENT);
        }
        
        updateEvent.setInt( 1, event.getGameId() );
        updateEvent.setInt( 2, event.getRound() );
        updateEvent.setInt( 3, event.getPeriod() );
        updateEvent.setInt( 4, event.getDelay() );
        updateEvent.setString( 5, event.getContent() );
        updateEvent.setInt( 6, event.getEventId() );
        
        updateEvent.executeUpdate();
      }
  
  public List<Parameter> getPlantTypeParams( int typeId )
  throws SQLException {
    if ( selectPlantTypeParams == null || selectPlantTypeParams.isClosed()) {
      selectPlantTypeParams = conn.prepareStatement(Constants.DB.SELECT_PLANT_TYPE_PARAMS);
    }
    selectPlantTypeParams.setInt( 1, typeId );
    ResultSet rs = selectPlantTypeParams.executeQuery();
    List<Parameter> params = new LinkedList<Parameter>();
    while( rs.next() ) {
      params.add( new Parameter( rs ) );
    }
    return params;
  }
  
  public List<Parameter> getPlantParams( int plantId )
  throws SQLException {
    if ( selectPlantParams == null || selectPlantParams.isClosed()) {
      selectPlantParams = conn.prepareStatement(Constants.DB.SELECT_PLANT_PARAMS);
    }
    selectPlantParams.setInt( 1, plantId );
    ResultSet rs = selectPlantParams.executeQuery();
    List<Parameter> params = new LinkedList<Parameter>();
    while( rs.next() ) {
      params.add( new Parameter( rs ) );
    }
    return params;
  }
  
  public void setPlantParams( int plantId, List<Parameter> params )
  throws SQLException {
    if ( insertPlantParams == null || insertPlantParams.isClosed()) {
      insertPlantParams = conn.prepareStatement(Constants.DB.INSERT_PLANT_PARAMS);
    }
    for ( Parameter param : params ) {
      if ( param.getParamId() > 0 ) {
        insertPlantParams.setInt( 1, plantId );
        insertPlantParams.setInt( 2, param.getParamId() );
        insertPlantParams.setString( 3, param.getValue() );
        insertPlantParams.setString( 4, param.getValue() );
        insertPlantParams.addBatch();
      }
    }
    insertPlantParams.executeBatch();
    insertPlantParams.clearBatch();
  }
  
}
