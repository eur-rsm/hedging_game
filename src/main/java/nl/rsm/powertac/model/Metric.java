package nl.rsm.powertac.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.rsm.powertac.Database;

public class Metric
implements Comparable<Metric>
, Serializable {
  
  /**
   * 
   */
  private static final long serialVersionUID = 1383233071492329885L;
  private int metricId;
  private String name;
  private String unit;
  private int multiplier;
  private String priceUnit;
  private int priceMultiplier;
  private String spreadName;
  private double spreadMultiplier;
  private String spreadUnit;
  
  public Metric() {}
  
  public Metric( ResultSet rs ) throws SQLException {
    metricId = rs.getInt( "metricId" );
    name = rs.getString( "metricName" );
    unit = rs.getString( "unit" );
    multiplier = rs.getInt( "multiplier" );
    priceUnit = rs.getString( "priceUnit" );
    priceMultiplier = rs.getInt( "priceMultiplier" );
    spreadName = rs.getString( "spreadName" );
    spreadMultiplier = rs.getDouble( "spreadMultiplier" );
    spreadUnit= rs.getString( "spreadUnit" );
  }
  
  @Override
  public int hashCode() {
    return new Integer( metricId ).hashCode();
  }
  
  @Override
  public boolean equals( Object other ) {
    return other instanceof Metric && ((Metric) other).metricId == this.metricId;
  }
  
  @Override
  public int compareTo( Metric metric ) {
    return this.metricId - metric.metricId;
  }
  
  public void setMetricId(int metricId) {
    this.metricId = metricId;
  }

  public int getMetricId() {
    return metricId;
  }
  
  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
  
  public void setUnit(String unit) {
    this.unit = unit;
  }

  public String getUnit() {
    return unit;
  }
  
  public void setMultiplier(int multiplier) {
    this.multiplier = multiplier;
  }

  public int getMultiplier() {
    return multiplier;
  }
  
  public String getPriceUnit() {
    return priceUnit;
  }
  
  public void setPriceUnit( String priceUnit ) {
    this.priceUnit = priceUnit;
  }
  
  public int getPriceMultiplier() {
    return priceMultiplier;
  }
  
  public void setPriceMultiplier( int priceMultiplier ) {
    this.priceMultiplier = priceMultiplier;
  }
  
  public String getSpreadName() {
    return spreadName;
  }
  
  public void setSpreadName( String spreadName ) {
    this.spreadName = spreadName;
  }
  
  public double getSpreadMultiplier() {
    return spreadMultiplier;
  }
  
  public void setSpreadMultiplier( double spreadMultiplier ) {
    this.spreadMultiplier = spreadMultiplier;
  }
  
  public void setSpreadUnit( String spreadUnit ) {
    this.spreadUnit = spreadUnit;
  }

  public String getSpreadUnit() {
    return spreadUnit;
  }
  
  public static Metric getMetricById( int metricId )
  {
    Metric metric = null;
    if ( metricId <= 0 ) {
      return null;
    }

    Database db = new Database();
    try {
      metric = db.getMetric( metricId );
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }
    
    return metric;
  }
  
}
