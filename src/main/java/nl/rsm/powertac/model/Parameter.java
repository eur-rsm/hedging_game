package nl.rsm.powertac.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public class Parameter {
  
  public static enum Type {
    STRING, INTEGER, FLOAT
  }
  
  private int paramId;
  private String name;
  private Type type;
  private String value;
  private boolean auto = true;
  
  public Parameter( String name, int value ) {
    this.paramId = 0;
    this.name = name;
    this.type = Type.INTEGER;
    this.value = "" + value;
  }
  
  public Parameter( String name, float value ) {
    this.paramId = 0;
    this.name = name;
    this.type = Type.FLOAT;
    this.value = "" + value;
  }
  
  public Parameter( String name, String value ) {
    this.paramId = 0;
    this.name = name;
    this.type = Type.STRING;
    this.value = "";
  }
  
  public Parameter( ResultSet rs )
  throws SQLException {
    paramId = rs.getInt( "paramId" );
    name = rs.getString( "name" );
    type = Type.values()[ rs.getInt( "type" ) ];
    value = rs.getString( "value" );
    auto = rs.getInt( "auto" ) != 0;
  }
  
  public Parameter( Parameter pa ) {
    paramId = pa.getParamId();
    name = pa.getName();
    type = pa.getType();
    value = pa.getValue();
    auto = pa.getAuto();
  }
  
  public int getParamId() {
    return paramId;
  }
  
  public void setParamId( int paramId ) {
    this.paramId = paramId;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName( String name ) {
    this.name = name;
  }
  
  public Type getType() {
    return type;
  }
  
  public void setType( Type type ) {
    this.type = type;
  }
  
  public void setType( int typeIndex ) {
    this.type = Type.values()[ typeIndex ];
  }
  
  public String getValue() {
    return value;
  }
  
  public String getStringValue() {
    if ( ! type.equals( Type.STRING ) ) {
      throw new RuntimeException( "This is not a string parameter" );
    }
    return value;
  }
  
  public Integer getIntegerValue() {
    if ( ! type.equals( Type.INTEGER ) ) {
      throw new RuntimeException( "This is not an integer parameter" );
    }
    return Integer.parseInt( value );
  }
  
  public Float getFloatValue() {
    if ( ! type.equals( Type.FLOAT ) ) {
      throw new RuntimeException( "This is not a float parameter" );
    }
    return Float.parseFloat( value );
  }
  
  public void setValue( String value ) {
    try {
      if ( type.equals( Type.INTEGER ) ) {
        Integer.parseInt( value );
      } else if ( type.equals( Type.FLOAT ) ) {
        Float.parseFloat( value );
      }
    } catch ( NumberFormatException x ) {
      throw new IllegalArgumentException( "Illegal argument, expected " + type.name() );
    }
    this.value = value;
  }
  
  public boolean getAuto() {
    return auto;
  }
  
  public void setAuto( boolean auto ) {
    this.auto = auto;
  }
  
}
