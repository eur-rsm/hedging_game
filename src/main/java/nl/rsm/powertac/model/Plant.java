package nl.rsm.powertac.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nl.rsm.powertac.Database;
import nl.rsm.powertac.util.PlantDef;

public class Plant
implements Serializable, Comparable<Plant> {
  
  private static final long serialVersionUID = -2140798256933957559L;
  
  private int plantId = -1;
  private int typeId;
  private int gameId;
  
  public Plant( int typeId, int gameId ) {
    this.typeId = typeId;
    this.gameId = gameId;
  }
  
  public Plant( ResultSet rs ) throws SQLException {
    this.plantId = rs.getInt( "plantId" );
    this.typeId = rs.getInt( "typeId" );
    this.gameId = rs.getInt( "gameId" );
  }
  
  @Override
  public int hashCode() {
    return new Integer( plantId ).hashCode();
  }
  
  @Override
  public boolean equals( Object other ) {
    return other instanceof Plant && ((Plant) other).plantId == this.plantId;
  }
  
  @Override
  public int compareTo( Plant plant ) {
    return this.plantId - plant.plantId;
  }
  
  public int getPlantId() {
    return plantId;
  }

  public void setPlantId( int plantId ) {
    this.plantId = plantId;
  }

  public int getTypeId() {
    return typeId;
  }

  public void setTypeId( int typeId ) {
    this.typeId = typeId;
  }

  public int getGameId() {
    return gameId;
  }

  public void setGameId( int gameId ) {
    this.gameId = gameId;
  }
  
  public PlantType getType() {
    return PlantType.getPlantTypeById( typeId );
  }
  
  public String getName() {
    return getType().getName();
  }
  
  public List<Metric> getMetrics() {
    return getType().getMetrics();
  }
  
  public List<Parameter> getParameters() {
    return getParameters( true );
  }
  
  public List<Parameter> getParameters( boolean includeAuto ) {
    List<Parameter> params = null;
    Database db = new Database();
    
    try {
      params = db.getPlantParams( plantId );
      
      for ( Iterator<Parameter> it = params.iterator(); it.hasNext() ; ) {
        if ( it.next().getAuto() && ! includeAuto ) {
          it.remove();
        }
      }
      
      // Hack... Add bogus parameter equal to capacity
      String label = (getType().isVariableOutput() ? "Expected" : "Maximum") + " Output (MWh)";
      int capacity = Forecast.getForecastsBootstrap( gameId ).get( "p" + plantId + "_m1" ).getValue();
      Parameter param = new Parameter( label, capacity );
      param.setAuto( false );
      params.add( 0, param );
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      db.close();
    }
    return params;
  }
  
  public String getParameter( String key ) {
    return getParameter( key, "N/A" );
  }
      
  public String getParameter( String key, String defval ) {
    for ( Parameter param : getParameters() ) {
      if ( param.getName().startsWith( key ) ) {
        return param.getValue();
      }
    }
    return defval;
  }
  
  public void saveParameters() {
    Plant.saveParameters( this, getParameters() );
  }
  
  public double getMarginalCostPrice() {
    return Double.parseDouble( getParameter( "Marginal Cost", "0" ) );
  }
  
  public double getRampUpCost() {
    return Double.parseDouble( getParameter( "Ramping Cost", "0" ) );
  }
  
  public int getExpectedOutput() {
    return Forecast.getForecastsBootstrap( gameId ).get( "p" + plantId + "_m1" ).getValue();
  } 
  
  public int getRealOutput() {
    return (int) Math.round( Double.parseDouble( getParameter( "Real Output", "0" ) ) );
  }
  
  public void setRealOutput( int output ) {
    List<Parameter> params = getParameters();
    for ( Parameter param : params) {
      if ( param.getName().startsWith( "Real Output" ) ) {
        param.setValue( "" + output );
        break;
      }
    }
    saveParameters( this, params );
  }
  
  public double getOutputStandardDeviation() {
    return Double.parseDouble( getParameter( "Output Std.Dev.", "0" ) );
  }
  
  public static Set<Plant> getPlantsByUser( User user ) {
    Set<Plant> plants = null;
    
    Database db = new Database();
    try {
      plants = db.getPlantsByUser( user );
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }
    
    return plants;
    
  }
  
  public static Map<PlantType,List<Plant>> getPlantsByGame( Game game ) {
   Map<PlantType,List<Plant>> result = new LinkedHashMap<PlantType,List<Plant>>();

   Database db = new Database();
   try {
     Set<Plant> plants = db.getPlantsByGame( game );
     for ( Plant plant : plants ) {
       List<Plant> list = result.get( plant.getType() );
       if ( list == null ) {
         list = new LinkedList<Plant>();
         result.put( plant.getType(), list );
       }
       list.add( plant );
     }
   }
   catch ( Exception e ) {
     e.printStackTrace();
   }
   finally {
     db.close();
   }
   
   return result;
 }
 
 public static Plant getPlantById( int plantId ) {
   Plant plant = null;
   Database db = new Database();
   try {
     plant = db.getPlantById( plantId );
   }
   catch ( Exception e ) {
     e.printStackTrace();
   }
   finally {
     db.close();
   }
   return plant;
 }
 
 // TODO FIX THESE
 public static Integer getGamePlantValue( Game game, Plant plant, Metric metric ) {
   Map<String, Forecast> bs = Forecast.getForecastsBootstrap( game.getGameId() );
   Forecast f = bs.get( "p" + plant.getPlantId() + "_m" + metric.getMetricId() );
   return f == null ? 0 : f.getValue();
 }
 
 public static void saveParameters( Plant plant, List<Parameter> params ) {
   Database db = new Database();
   try {
     db.setPlantParams( plant.getPlantId(), params );
   } catch ( Exception x ) {
     x.printStackTrace();
   } finally {
     db.close();
   }
 }
 
 public static void setPlantSetup( Game game, List<PlantDef> defs, Map<Integer, List<List<Parameter>>> params ) {
   Database db = new Database();
   try {
     db.setPlantSetup( game, defs, params );
   }
   catch ( Exception e ) {
     e.printStackTrace();
   }
   finally {
     db.close();
   }
 }

 
}
