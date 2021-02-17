package nl.rsm.powertac.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import nl.rsm.powertac.Database;

public class Position {

  
  private int gameId;
  private int userId;
  private int plantId;
  private int metricId;
  private int round;
  private int period;
  private int value;
  
  public Position() {}
  
  public Position( int gameId, int userId, int plantId, int metricId, int round, int period ) {
    this.gameId = gameId;
    this.userId = userId;
    this.plantId = plantId;
    this.metricId = metricId;
    this.round = round;
    this.period = period;
    this.value = 0;
  }
  
  public Position (ResultSet resultSet) throws SQLException
  {
    this.gameId = resultSet.getInt("gameId");
    this.userId = resultSet.getInt("userId");
    this.plantId = resultSet.getInt("plantId");
    this.metricId = resultSet.getInt("metricId");
    this.round = resultSet.getInt("round");
    this.period = resultSet.getInt("period");
    this.value = resultSet.getInt("value");
  }
  
  public int getGameId() {
    return gameId;
  }

  public void setGameId( int gameId ) {
    this.gameId = gameId;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId( int userId ) {
    this.userId = userId;
  }

  public int getPlantId() {
    return plantId;
  }

  public void setPlantId( int plantId ) {
    this.plantId = plantId;
  }

  public int getMetricId() {
    return metricId;
  }

  public void setMetricId( int metricId ) {
    this.metricId = metricId;
  }

  public int getRound() {
    return round;
  }

  public void setRound( int round ) {
    this.round = round;
  }
  
  public int getPeriod() {
    return period;
  }

  public void setPeriod( int period ) {
    this.period = period;
  }

  public int getValue() {
    return value;
  }

  public void setValue( int value ) {
    this.value = value;
  }
  
  public int getNetValue() {
    Map<String, Forecast> forecasts = Forecast.getForecastsBootstrap( gameId );
    String key = "p" + plantId + "_m" + metricId;
    Forecast forecast = forecasts == null ? null : forecasts.get( key );
    return value + (forecast == null ? 0 : forecast.getValue() );
  }
  
  
  public static Map<Integer, Map<String, Position>> getPositions( Game game, User user, int round, int period ) {
    Map<Integer, Map<String, Position>> positions = new HashMap<Integer, Map<String, Position>>();

    Database db = new Database();
    try {
      positions = db.getPositions( game, user, round, period );
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }

    for ( int p : game.getValidPeriods()) {
      if ( ! positions.containsKey( p ) ) {
        Map<String, Position> map = new HashMap<String, Position>();
        for ( Plant plant : user.getPlants() ) {
          PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
          for ( Metric metric : type.getMetrics() ) {
            Position s = new Position( game.getGameId()
                , user.getUserId()
                , plant.getPlantId()
                , metric.getMetricId()
                , round
                , p
            );
            map.put( "p" + plant.getPlantId() + "_m" + metric.getMetricId(), s );
          }
        }
        positions.put( p, map );
      }
    }
    
    return positions;
  }

  public static Position getPosition( Game game, User user, Plant plant,
      Metric metric, int round, int period ) {
    Position position = null;
    
    Database db = new Database();
    try {
      position = db.getPosition( game, user, plant, metric, round, period );
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }
    return position;
  }
  
  public static void updatePositions( Game game, int round, int period ) {
    Database db = new Database();
    try {
      db.updatePositions( game, round, period );
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }
  }
  
}
