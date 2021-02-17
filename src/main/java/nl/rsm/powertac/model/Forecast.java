package nl.rsm.powertac.model;

import nl.rsm.powertac.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class Forecast
{
  private int gameId;
  private int userId;
  private int plantId;
  private int metricId;
  private int round;
  private int period;
  private int value;
  
  public Forecast() {}
  
  public Forecast( int gameId, int userId, int plantId, int metricId, int round, int period ) {
    this.gameId = gameId;
    this.userId = userId;
    this.plantId = plantId;
    this.metricId = metricId;
    this.round = round;
    this.period = period;
    this.value = 0;
  }
  
  public Forecast (ResultSet resultSet) throws SQLException
  {
    this.gameId = resultSet.getInt("gameId");
    this.userId = resultSet.getInt("userId");
    this.plantId = resultSet.getInt("plantId");
    this.metricId = resultSet.getInt("metricId");
    this.round = resultSet.getInt("round");
    this.period = resultSet.getInt("period");
    this.value = resultSet.getInt("value");
  }
  
  
  private static Map<Integer, Map<String, Forecast>> bootstrapForecasts
  = new HashMap<Integer, Map<String, Forecast>>();
  
 
  public static Map<String, Forecast> getForecastsBootstrap( int gameId ) {
    Map<String, Forecast> forecasts = bootstrapForecasts.get( gameId );
    
    if ( forecasts == null ) {
      
      Database db = new Database();
      try {
        forecasts = db.getForecastsBootstrap( gameId );
        for ( Forecast f : forecasts.values() ) {
          f.setValue( (int) Math.round( (double) f.getValue() ) );
        }
        bootstrapForecasts.put( gameId, forecasts );
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      finally {
        db.close();
      }
    }
    return forecasts;
  }
  
  public static Map<Integer, Map<String, Forecast>> getForecasts( Game game, User user, int round, int period ) {
    Map<Integer, Map<String, Forecast>> forecasts = null;
    
    Database db = new Database();
    try {
      forecasts = db.getForecasts( game, user, round, period );
      forecasts.put( 0, getForecastsBootstrap( game.getGameId() ) );
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    finally {
      db.close();
    }

    for ( int p : game.getValidPeriods()) {
      if ( ! forecasts.containsKey( p ) ) {
        Map<String, Forecast> map = new HashMap<String, Forecast>();
        for ( Plant plant : user.getPlants() ) {
          PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
          for ( Metric metric : type.getMetrics() ) {
            Forecast f = new Forecast(
                game.getGameId()
                , user.getUserId()
                , plant.getPlantId()
                , metric.getMetricId()
                , round
                , p
            );
            map.put( "p" + plant.getPlantId() + "_m" + metric.getMetricId(), f );
          }
        }
        forecasts.put( p, map );
      }
    }
    
    return forecasts;
  }

  public static boolean saveForecast (Forecast forecast)
  {
    Database db = new Database();
    try {
      db.saveForecast(forecast);
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
  
  
}