package nl.rsm.powertac.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import nl.rsm.powertac.Database;

public class Market
{
  private int gameId;
  private int metricId;
  private int round;
  private int period;
  private double price;
  private double load;
  private int cleared;
  private double forecast;
  private double sigma;
  
  public Market( ResultSet rs ) throws SQLException {
    gameId = rs.getInt( "gameId" );
    metricId = rs.getInt( "metricId" );
    round = rs.getInt( "round" );
    period = rs.getInt( "period" );
    price = rs.getDouble( "price" );
    load = rs.getDouble( "load" );
    cleared = rs.getInt( "cleared" );
    forecast = rs.getDouble( "forecast" );
    sigma = rs.getDouble( "sigma" );
  }
  
  public Market( int gameId, int metricId, int round, int period, double price
      , double load, double forecast, double sigma ) {
    this.gameId = gameId;
    this.metricId = metricId;
    this.round = round;
    this.period = period;
    this.price = price;
    this.load = load;
    this.forecast = forecast;
    this.sigma = sigma;
  }

  public int getGameId() {
    return gameId;
  }
  
  public void setGameId( int gameId ) {
    this.gameId = gameId;
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
  
  public double getPrice() {
    return price;
  }
  
  public void setPrice( double price ) {
    this.price = price;
  }
  
  public double getLoad() {
    // Ugh.. If sigma is zero, return forecast instead of load.
    return sigma == 0 ? forecast : load;
  }
  
  public void setLoad( double load ) {
    this.load = load;
  }
  
  public int getCleared() {
    return cleared;
  }
  
  public void setCleared(int cleared) {
    this.cleared = cleared;
  }
  
  public double getForecast() {
    return forecast;
  }

  public void setForecast( double forecast ) {
    this.forecast = forecast;
  }

  public double getSigma() {
    return sigma;
  }

  public void setSigma( double mad ) {
    this.sigma = mad;
  }
  
  public boolean isNegativeMarket() {
    return this.load < 0D;
  }
  
  
  public static Map<String, Market> getMarketsBootstrap( Game game ) {
    Map<String, Market> map = null;
    
    Database db = new Database();
    try {
      map = db.getMarketsBootstrap( game );
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    finally {
      db.close();
    }
    
    return map;
  }
  
  public static Map<Integer, Map<String, Market>> getMarkets( Game game, int round, int period ) {
    
    Map<Integer, Map<String, Market>> markets = null;

    Database db = new Database();
    try {
      markets = db.getMarkets( game, round, period ); // -1 ?
      markets.put( 0, getMarketsBootstrap( game ) );
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    finally {
      db.close();
    }
    
    return markets;
  }
  
  public static List<Market> getMarkets( Game game ) {
    Database db = new Database();
    List<Market> markets = null;
    try {
      markets = db.getMarkets( game );
    } catch ( SQLException x ) {
      x.printStackTrace();
    } finally {
      db.close();
    }
    return markets;
  }
  
  
  public static void saveMarket( Market market ) {
    Database db = new Database();
    try {
      db.saveMarket( market );
    } catch ( SQLException x ) {
      x.printStackTrace();
      throw new RuntimeException( x );
    } finally {
      db.close();
    }
  }
  
  public static void saveMarkets( List<Market> markets ) {
    Database db = new Database();
    try {
      db.saveMarkets( markets );
    } catch ( SQLException x ) {
      x.printStackTrace();
      throw new RuntimeException( x );
    } finally {
      db.close();
    }
  }
  
  public static void emulateMarkets( Game game, int round, int period ) {
    System.out.println( "Emulating markets for round #" + round + ", period #" + period );
    Database db = new Database();
    try {
      
      List<Metric> metrics = db.getMetrics();
      for ( Metric metric : metrics ) {
        //System.out.println( "Metric " + metric.getName() );
        
        // HACK we're only using metricId==1 from now on, so ignore the others...
        if ( metric.getMetricId() != 1 ) {
          continue;
        }
        
        Map<Integer, Map<String, Market>> markets = db.getMarkets( game, round, period, period );
        if ( markets == null || ! markets.containsKey( period ) ) {
          throw new RuntimeException( "Unexpected missing market for round " + round + " period " + period );
        }
        Market market = markets.get( period ).get( "m" + metric.getMetricId() );
        if ( market == null ) {
          throw new RuntimeException( "Unexpected missing market for round " + round + " period " + period + " metric " +  metric.getMetricId() );
        }
        
        double quantity = market.getLoad();
        int sign = quantity < 0D ? -1 : 1;
        quantity = Math.abs( quantity );
        List<Hedge> hedges = db.getHedgesPerMetric( game, metric, round, period, sign );
        
        NavigableMap<Double, Double> plot = new TreeMap<Double, Double>();
        
        double cumq = 0;
        for ( Hedge h : hedges ) {
          if ( h.getSkip() ) {
            // This bid is excluded from the price determination
            continue;
          }
          
          double q = Math.abs( h.getBidQuantity() );
          double p = h.getBidPrice();
          if ( q > 0 ) {
            plot.put( cumq, p );
            cumq += q;
          }
        }
        
        if ( plot.size() == 0 ) {
          System.out.println( "no valid bids, no market !!!" );
          // Need to set clearing quantity for fixed hedges ?
          /*
          for ( Hedge h : hedges ) {
            if ( h.getSkip() ) {
              h.setClearingQuantity( h.getBidQuantity() );
              db.saveHedge( h );
            }
          }
          */
          continue;
        }
        
        /*
        for ( Map.Entry<Double, Double> entry : plot.entrySet() ) {
          System.out.println( entry.getKey() + " \t => \t" + entry.getValue() );
        }
        */
        
        Map.Entry<Double, Double> x = plot.lowerEntry( quantity );
        double price = x.getValue();
        Double x1 = null, x2 = null;
        for ( Map.Entry<Double, Double> xx : plot.entrySet() ) {
          if ( xx.getValue() == price ) {
            if (x1 == null) {
              x1 = xx.getKey();
            }
          } else if (xx.getValue() > price) {
            x2 = xx.getKey();
            break;
          }
        }
        if (x2 == null) {
          x2 = cumq;
        }
        double partial = (double)(quantity - x1) / (double)(x2 - x1);
        if ( partial < 0D ) {
          partial = 0D;
        } else if ( partial > 1D ) {
          partial = 1D;
        }
        
        /*
        System.out.println( "price \t" + price );
        System.out.println( "quantity \t" + quantity );
        System.out.println( "start \t" + x1 );
        System.out.println( "end \t" + x2 );
        System.out.println( "partial \t" + partial );
        */
        
        // save market
        market.setPrice( price );
        
        int cleared = 0;
        
        // update hedges
        for ( Hedge h : hedges ) {
          double p, q;
          
          // multiplier > 0: we've got supply and are assuming fixed demand
          // multiplier < 0: we've got demand and are assuming fixed supply
          
          if ( h.getSkip() ) {
            p = price;
            if ( sign < 0 && h.getPeriod() > 0   ||  sign > 0 && h.getPeriod() < 0 ) {
              q = 0;
            } else {
              q = h.getBidQuantity();
            }
          } else if ( sign > 0 && price < h.getBidPrice()
              || sign < 0 && price > h.getBidPrice() ) {
            // Bid rejected entirely
            p = 0;
            q = 0;
          } else if ( sign > 0 && price > h.getBidPrice()
              || sign < 0 && price < h.getBidPrice() ) {
            // Bid accepted entirely
            p = price;
            q = h.getBidQuantity();
          } else {
            // exact price match: set Q proportionally
            p = price;
            q = (int) Math.round( (double) h.getBidQuantity() * partial );
          }
          
          // TODO change all other tables to use Doubles too? Or revert market P/Q to integers?
          h.setClearingPrice( (int) Math.round( p ) );
          h.setClearingQuantity( (int) Math.round( q ) );
          db.saveHedge( h );
          
          cleared += q;
        }
        
        market.setCleared( Math.abs( cleared ) ); 
        db.saveMarket( market );
        
      }
      
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      db.close();
    }
    
    
  }

}