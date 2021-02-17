package nl.rsm.powertac.model;

import nl.rsm.powertac.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class Hedge
{
  private int gameId;
  private int userId;
  private int plantId;
  private int metricId;
  private int round;
  private int period;
  private Integer bidQuantity;
  private Integer bidPrice;
  private int clearingQuantity;
  private int clearingPrice;
  private boolean skip;
  
  public Hedge() {}

  public Hedge ( int gameId, int userId, int plantId, int metricId, int round, int period )
  {
    this.gameId = gameId;
    this.userId = userId;
    this.plantId = plantId;
    this.metricId = metricId;
    this.round = round;
    this.period = period;
    this.bidQuantity = 0;
    this.bidPrice = 0;
    this.clearingQuantity = 0;
    this.clearingPrice = 0;
    this.skip = false;
  }

  public Hedge (ResultSet resultSet) throws SQLException
  {
    this.gameId = resultSet.getInt("gameId");
    this.userId = resultSet.getInt("userId");
    this.plantId = resultSet.getInt("plantId");
    this.metricId = resultSet.getInt("metricId");
    this.round = resultSet.getInt("round");
    this.period = resultSet.getInt("period");
    this.bidQuantity = resultSet.getInt("bidQ");
    this.bidPrice = resultSet.getInt("bidP");
    this.clearingQuantity = resultSet.getInt("clearQ");
    this.clearingPrice = resultSet.getInt("clearP");
    this.skip = resultSet.getInt( "skip" ) != 0;
  }

  public static Map<String, Hedge> getHedgesBootstrap( Game game, User user ) {
    Map<String, Hedge> hedges = new HashMap<String, Hedge>();
    for ( Plant plant : user.getPlants() ) {
      for ( Metric metric : plant.getMetrics() ) {
        Hedge h = new Hedge( game.getGameId(), user.getUserId(), plant.getPlantId()
            , metric.getMetricId(), game.getCurrentRound(), 0 );
        hedges.put( "p" + plant.getPlantId() + "_m" + metric.getMetricId(), h );
      }
    }
    return hedges;
  }
  
  public static Map<String, Map<String, Hedge>> getHedges( Game game, User user, int round, int period ) {
    Map<String, Map<String, Hedge>> hedges = new HashMap<String, Map<String, Hedge>>();

    Database db = new Database();
    try {
      hedges = db.getHedges( game, user, round, period );
      for ( int r : game.getValidRounds() ) {
        hedges.put( r + ":0", getHedgesBootstrap( game, user ) );
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }

    for ( int r : game.getValidRounds() ) {
      for ( int p : game.getValidHedgePeriods() ) {
        String k = r + ":" + p;
        Map<String, Hedge> map = hedges.get( k );
        if ( map == null ) {
          map = new HashMap<String, Hedge>();
          hedges.put( k, map );
        }
        for ( Plant plant : user.getPlants() ) {
          PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
          for ( Metric metric : type.getMetrics() ) {
            k = "p" + plant.getPlantId() + "_m" + metric.getMetricId();
            if ( ! map.containsKey( k ) ) {
              Hedge f = new Hedge( game.getGameId()
                  , user.getUserId()
                  , plant.getPlantId()
                  , metric.getMetricId()
                  , round
                  , p
              );
              map.put( k, f );
            }
          }
        }
      }
    }

    return hedges;
  }

  public static boolean saveHedge (Hedge hedge)
  {
    Database db = new Database();
    try {
      db.saveHedge(hedge);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    finally {
      db.close();
    }

    return true;
  }

  //<editor-fold desc="Getters and Setters">
  
  public int getGameId ()
  {
    return gameId;
  }

  public void setGameId (int gameId)
  {
    this.gameId = gameId;
  }
  
  public int getUserId ()
  {
    return userId;
  }

  public void setUserId (int userid)
  {
    this.userId = userid;
  }
  
  public int getPlantId ()
  {
    return plantId;
  }

  public void setPlantId (int plantId)
  {
    this.plantId = plantId;
  }
  
  public int getMetricId ()
  {
    return metricId;
  }

  public void setMetricId (int metricId)
  {
    this.metricId = metricId;
  }

  public int getRound() {
    return round;
  }

  public void setRound( int round ) {
    this.round = round;
  }
  
  public int getPeriod ()
  {
    return period;
  }

  public void setPeriod ( int period )
  {
    this.period = period;
  }
  
  public Integer getBidQuantity() {
    return bidQuantity;
  }
  
  public Integer getBidQuantityAbs() {
    if (getFixed()) {
      if (period < 0) {
        return bidQuantity >= 0 ? bidQuantity : null;
      }
      return bidQuantity <= 0 ? -bidQuantity : null;
    }
    return Math.abs( bidQuantity );
  }
  
  public void setBidQuantity( Integer quantity ) {
    this.bidQuantity = quantity == null ? 0 : quantity;
  }
  
  public void setBidQuantityAbs( Integer quantity ) {
    this.bidQuantity = Math.abs( quantity );
    if ( getPeriod() == 1 || getPeriod() == 2 ) {
      this.bidQuantity *= -1;
    }
  }
  
  public Integer getBidPrice() {
    return bidPrice;
  }
  
  public void setBidPrice( Integer price ) {
    this.bidPrice = price == null ? 0 : price;
  }
  
  public int getClearingQuantity() {
    return clearingQuantity;
  }
  
  public int getClearingQuantityAbs() {
    return Math.abs( clearingQuantity );
  }
  
  public void setClearingQuantity( int quantity ) {
    this.clearingQuantity = quantity;
  }
  
  public int getClearingPrice() {
    return getClearingQuantity() == 0 ? 0 : clearingPrice;
  }
  
  public void setClearingPrice( int price ) {
    this.clearingPrice = price;
  }
  
  public boolean getSkip() {
    return skip;
  }
  
  public void setSkip( boolean skip ) {
    this.skip = skip;
  }
  
  public boolean getFixed() {
    return skip && bidPrice == 0;
  }
  
  //</editor-fold>
  
  public String toString() {
    return "userId: " + userId + " gameId " + gameId + " round " + round + " period " + period; 
  }
  
}