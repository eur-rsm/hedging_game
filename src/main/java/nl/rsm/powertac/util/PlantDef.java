package nl.rsm.powertac.util;

import java.util.HashMap;
import java.util.Map;

import nl.rsm.powertac.model.Metric;
import nl.rsm.powertac.model.PlantType;

public class PlantDef {
  
  private PlantType type;
  private Map<Metric, Integer> values;
  
  public PlantDef( PlantType type, Map<Metric, Integer> values ) {
    this.type = type;
    this.values = values;
  }
  
  public PlantDef( String string ) {
    String[] split = string.split( "#" );
    int type = -1;
    values = new HashMap<Metric, Integer>();
    for ( String s : split ) {
      int index = s.indexOf( ':' );
      String key = s.substring( 0, index );
      String[] t = key.split( "_" );
      int y = Integer.parseInt( t[0].substring( 1 ) );
      if ( type != -1 && type != y ) {
        throw new IllegalArgumentException( "can't have several types in one string" );
      }
      type = y;
      Metric m = Metric.getMetricById( Integer.parseInt( t[1].substring( 1 ) ) );
      Integer v = Integer.parseInt( s.substring(  index + 1 ) );
      values.put( m, v );
    }
    this.type = PlantType.getPlantTypeById( type );
  }
  
  public PlantType getType() {
    return type;
  }
  
  public Map<Metric, Integer> getValues() {
    return values;
  }
  
}
