package nl.rsm.powertac.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.rsm.powertac.Database;

public class PlantType
implements Comparable<PlantType>, Serializable {
  
  private static final long serialVersionUID = -1688921211153850773L;
  
  private int typeId;
  private String typeName;
  
  private List<Metric> metrics = null;
  private List<Parameter> params = null;

  static private Map<Integer, PlantType> TYPES = new HashMap<Integer, PlantType>();
  static {
    Database db = new Database();
    try {
      for ( PlantType type : db.getAllPlantTypes() ) {
        TYPES.put(  type.typeId, type );
      }
    } catch ( SQLException x ) {
      x.printStackTrace();
    } finally {
      db.close();
    }
  }
  
  public PlantType() {
    
  }
  
  public PlantType( ResultSet rs )
  throws SQLException {
    this.typeId = rs.getInt( "typeId" );
    this.typeName = rs.getString( "typeName" );
  }
  
  @Override
  public int hashCode() {
    return new Integer( typeId ).hashCode();
  }
  
  @Override
  public boolean equals( Object other ) {
    return other instanceof PlantType && ((PlantType) other).typeId == this.typeId;
  }
  
  @Override
  public int compareTo( PlantType plant ) {
    return this.typeId - plant.typeId;
  }
  
  public void setTypeId( int typeId ) {
    this.typeId = typeId;
  }
  
  public int getTypeId() {
    return typeId;
  }
  
  public void setPlantName( String plantName ) {
    this.typeName = plantName;
  }
  
  public String getName() {
    return typeName;
  }
  
  public List<Metric> getMetrics() {
    
    if ( metrics != null ) {
      return metrics;
    }
    
    Database db = new Database();
    
    try {
      metrics = db.getPlantMetrics( typeId );
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }
    
    return metrics;
  }
  
  public List<Parameter> getParameters() {
    if ( params != null ) {
      return params;
    }
    
    Database db = new Database();
    
    try {
      params = db.getPlantTypeParams( typeId );
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      db.close();
    }
    
    return params;
  }
  
  
  
  
  // TODO put this in DB
  public boolean isVariableOutput() {
    return typeId >= 1 && typeId <= 2;
  }
  
  
  public static PlantType getPlantTypeById( int typeId ) {
    return TYPES.get( typeId );
  }
  
  public static List<PlantType> getAllPlantTypes() {
    List<PlantType> types = new LinkedList<PlantType>();
    for ( PlantType type : TYPES.values() ) {
      types.add( type );
    }
    return types;
  }
  
  
}
