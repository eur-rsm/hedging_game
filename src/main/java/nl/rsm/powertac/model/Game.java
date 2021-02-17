package nl.rsm.powertac.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.rsm.powertac.Database;
import nl.rsm.powertac.util.PlantDef;
import nl.rsm.powertac.util.Constants;

public class Game {
  
  private int gameId;
  private String name;
  private String stamp;
  private int rounds;
  private int periods;
  private int plants;
  private boolean active;
  private boolean showMarketInfo;
  private boolean showCostInfo;
  private int currentRound;
  private int currentPeriod;
  private int minPrice = Constants.MINIMUM_HEDGING_PRICE;
  private int maxPrice = Constants.MAXIMUM_HEDGING_PRICE;
  
  public Game() {
    this( "New Game", 3, 2, 5, 1 );
  }
  
  public Game( String name, int rounds, int periods, int users, int plants ) {
    this.gameId = -1;
    this.name = name;
    this.stamp = null;
    this.rounds = rounds;
    this.periods = periods;
    this.plants = plants;
    this.active = false;
    this.currentRound = 0;
    this.currentPeriod = 0;
    this.showMarketInfo = false;
    this.showCostInfo = false;
  }
  
  public Game( ResultSet rs ) throws SQLException {
    gameId = rs.getInt( "gameId" );
    name = rs.getString( "name" );
    stamp = rs.getString( "stamp" );
    rounds = rs.getInt( "rounds" );
    periods = rs.getInt( "periods" );
    plants = rs.getInt( "plants" );
    active = rs.getBoolean( "active" );
    currentRound = rs.getInt( "currentRound" );
    currentPeriod = rs.getInt( "currentPeriod" );
    showMarketInfo = rs.getBoolean( "showMarketInfo" );
    showCostInfo = rs.getBoolean( "showCostInfo" );
    minPrice = rs.getInt( "minPrice" );
    maxPrice = rs.getInt( "maxPrice" );
  }
  
  public int getGameId() {
    return gameId;
  }
  
  public void setGameId( int gameId ) {
    this.gameId = gameId;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName( String name ) {
    if ( name != null && name.length() > 63 ) {
      name = name.substring( 0, 63 );
    }
    this.name = name;
  }
  
  public String getStamp() {
    return stamp;
  }
  
  public void setStamp( String stamp ) {
    this.stamp = stamp;
  }
  
  public int getNumRounds() {
    return rounds;
  }
  
  public void setNumRounds( int rounds ) {
    this.rounds = rounds;
  }
  
  public int getNumPeriods() {
    return periods;
  }
  
  public void setNumPeriods( int periods ) {
    this.periods = periods;
  }
  
  public int getNumPlants() {
    return plants;
  }
  
  public void setNumPlants( int plants ) {
    this.plants = plants;
  }
  
  public boolean getActive() {
    return active;
  }
  
  public void setActive( boolean active ) {
    this.active = active;
  }
  
  public int getCurrentRound() {
    return currentRound;
  }
  
  public void setCurrentRound( int currentRound ) {
    this.currentRound = currentRound;
  }
  
  public int getCurrentPeriod() {
    return currentPeriod;
  }
  
  public void setCurrentPeriod( int currentPeriod ) {
    this.currentPeriod = currentPeriod;
  }
  
  public boolean getShowMarketInfo() {
    return showMarketInfo;
  }
  
  public void setShowMarketInfo( boolean showMarketInfo ) {
    this.showMarketInfo = showMarketInfo;
  }
  
  public boolean getShowCostInfo() {
    return showCostInfo;
  }
  
  public void setShowCostInfo( boolean showCostInfo ) {
    this.showCostInfo = showCostInfo;
  }
  
  public int getMinPrice() {
    return minPrice;
  }
  
  public void setMinPrice( int price ) {
    minPrice = price;
  }
  
  public int getMaxPrice() {
    return maxPrice;
  }
  
  public void setMaxPrice( int price ) {
    maxPrice = price;
  }
  
  public Map<PlantType, List<Plant>> getPlants() {
    return Plant.getPlantsByGame( this );
  }
  
  public void setPlantSetup( List<PlantDef> setup, Map<Integer, List<List<Parameter>>> params ) {
    Plant.setPlantSetup( this, setup, params );
  }
  
  public void save()
  throws Exception {
    try {
      Database db = new Database();
      if ( gameId <= 0 ) {
        db.insertGame( this );
      } else {
        db.updateGame( this );
      }
    } catch ( SQLException x ) {
      x.printStackTrace();
      throw x;
    }
  }
  
  public List<User> getUsers() {
    List<User> users = null;
    try {
      Database db = new Database();
      users = db.getGameUsers( this );
    } catch ( SQLException x ) {
      x.printStackTrace();
    }
    return users;
  }
  
  public Map<User, List<Plant>> getUserPlants() {
    return Game.getUserPlantsByGame( this );
  }
  
  public static Game getGameById( int gameId ) {
    Game game = null;
    Database db = new Database();
    try {
      game = db.getGame( gameId );
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      db.close();
    }
    return game;
  }
  
  public static void setActiveGame( Game game ) {
    Game prev = Game.getActiveGame();
    if ( prev != null ) {
      if ( game.getGameId() == prev.getGameId() ) {
        return;
      }
      
      for ( User user : prev.getUsers() ) {
        if ( !user.isAdmin() ) {
          user.logout();
        }
      }
    }
    
    Database db = new Database();
    try {
      db.setActiveGame( game );
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      db.close();
    }
  }
  
  public static Game getActiveGame() {
    Game game = null;
    Database db = new Database();
    try {
      game = db.getActiveGame();
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      db.close();
    }
    return game;
  }
  
  public static List<Game> getAllGames() {
    List<Game> games = null;
    Database db = new Database();
    try {
      games = db.getAllGames();
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      db.close();
    }
    return games;
  }
  
  public static Map<User, List<Plant>> getUserPlantsByGame( Game game ) {
    Map<User, List<Plant>> plants = null;
    
    Database db = new Database();
    try {
      plants = db.getUserPlantsByGame( game );
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }
    return plants;
  }
  
  public boolean isInitialPeriod() {
    return isInitialPeriod( getCurrentPeriod() );
  }
  
  public boolean isInitialPeriod( int period ) {
    return period == 0;
  }
  
  public boolean isFinalPeriod() {
    return isFinalPeriod( getCurrentPeriod() );
  }
  
  public boolean isFinalPeriod( int period ) {
    return period > getNumPeriods();
  }
  
  public boolean isPenultimatePeriod() {
    return isPenultimatePeriod( getCurrentPeriod() );
  }
  
  public boolean isPenultimatePeriod( int period ) {
    return Math.abs( period ) == getNumPeriods();
  }
  
  public boolean isFinalRound() {
    return getCurrentRound() > getNumRounds();
  }
  
  public List<Integer> getAllPeriods() {
    List<Integer> list = new LinkedList<Integer>();
    for ( int i = 0; i <= getNumPeriods(); i++ ) {
      list.add( i );
    }
    return list;
  }
  
  public List<Integer> getValidPeriods() {
    List<Integer> list = new LinkedList<Integer>();
    for ( int i = 0; i <= getCurrentPeriod(); i++ ) {
      list.add( i );
    }
    return list;
  }
  
  public List<Integer> getValidHedgePeriods() {
    List<Integer> list = new LinkedList<Integer>();
    for ( int i = 0; i <= getCurrentPeriod(); i++ ) {
      list.add( i );
    }
    int last = list.get( list.size() - 1 );
    if ( isPenultimatePeriod( last ) ) {
      list.add( -last );
    }
    return list;
  }
  
  public List<Integer> getHedgePeriods() {
    List<Integer> periods = getAllPeriods();
    periods.add( - periods.get( periods.size() - 1 ) );
    return periods;
  }
  
  public String getPeriodName( int period ) {
    return Constants.PERIOD_NAMES[ period ];
  }
  
  public String getHedgePeriodName( int period ) {
    if ( isPenultimatePeriod( period ) ) {
      if ( period > 0 ){
        return Constants.PERIOD_NAMES[ period ] + " Sell";
      } else {
        return Constants.PERIOD_NAMES[ - period ] + " Buy";
      }
    }
    return Constants.PERIOD_NAMES[ period ];
  }
  
  public List<Integer> getValidRounds() {
    return getValidRounds( -1 );
  }
  
  public List<Integer> getValidRounds( int limit ) {
    int start = limit < 0 ? 0 : Math.max( 0, currentRound + 1 - limit );
    List<Integer> list = new LinkedList<Integer>();
    for ( int i = start; i <= currentRound; i++ ) {
      list.add( i );
    }
    return list;
  }
  
}
