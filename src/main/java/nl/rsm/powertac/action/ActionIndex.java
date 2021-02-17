package nl.rsm.powertac.action;

import nl.rsm.powertac.Exporter;
import nl.rsm.powertac.endpoints.AdminResource;
import nl.rsm.powertac.model.Event;
import nl.rsm.powertac.model.EventService;
import nl.rsm.powertac.model.Forecast;
import nl.rsm.powertac.model.Game;
import nl.rsm.powertac.model.Hedge;
import nl.rsm.powertac.model.Market;
import nl.rsm.powertac.model.Metric;
import nl.rsm.powertac.model.Plant;
import nl.rsm.powertac.model.PlantType;
import nl.rsm.powertac.model.Position;
import nl.rsm.powertac.model.User;
import nl.rsm.powertac.util.Constants;
import nl.rsm.powertac.util.Utils;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
//import javax.faces.bean.SessionScoped;
//import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ViewScoped;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ManagedBean
@RequestScoped
public class ActionIndex
{
  private Game activeGame;
  
  private Set<Plant> plants;
  private Map<Integer, Metric> metrics;
  
  private String typeKeys;
  
  private Map<Integer, Map<String, Forecast>> forecasts;
  private Map<String, Map<String, Hedge>> hedges;
  private Map<Integer, Map<String, Market>> markets;
  private Map<Integer, Map<String, Position>> positions;
  private Map<Integer, Boolean> variableOutputs;
  
  private int numSpreads = 0;
  private int powerMetricId = -1;
  
  public ActionIndex ()
  {
    activeGame = Game.getActiveGame();
    
    User user = User.getCurrentUser();
    if (user == null ) {
      return;
    }
    
    if ( ! user.isAdmin() && user.getGameId() > 0
    && user.getGameId() != activeGame.getGameId() ) {
      Utils.growlMessage("Warning", "A new game has started; please log in again.");
      user.logout();
      return;
    }
    
    long inactive = user.inactiveSeconds();
    if ( inactive > Constants.MAX_INACTIVE_SECONDS ) {
      Utils.growlMessage("Warning", "Inactive for too long; please log in again.");
      user.logout();
      return;
    }
    
    plants = user.getPlants();
    variableOutputs = new HashMap<Integer, Boolean>( plants.size(), 1F );
    for ( Plant plant : plants ) {
      variableOutputs.put( plant.getPlantId(), plant.getType().isVariableOutput() );
    }
    metrics = new LinkedHashMap<Integer, Metric>();
    metrics.put( 1, Metric.getMetricById( 1 ) );
    
    if ( user.isUser() ) {
      
      for ( Plant plant : plants ) {
        PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
        for ( Metric metric : type.getMetrics() ) {
          if ( ! metrics.containsKey( metric.getMetricId() ) ) {
            metrics.put( metric.getMetricId(), metric );
            if ( metric.getSpreadName() == null ) {
              // TODO this is ugly... detect Power metric by NULL spreadName
              powerMetricId = metric.getMetricId();
            } else {
              numSpreads++;
            }
          }
        }
      }
      
      typeKeys = "";
      for ( Plant plant : plants ) {
        PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
        for ( Metric metric : type.getMetrics() ) {
          String k = "p" + plant.getPlantId() + "_m" + metric.getMetricId() + "";
          typeKeys += k + ",";
        }
      }
      if ( typeKeys.length() > 0 ) {
        typeKeys = typeKeys.substring( 0, typeKeys.length() - 1 );
      }
      
      int round = activeGame.getCurrentRound();
      int period = activeGame.getCurrentPeriod();
      
      forecasts = Forecast.getForecasts( activeGame, user, round, period );
      hedges = Hedge.getHedges( activeGame, user, round, period );
      
      /*for ( Map.Entry<String, Map<String, Hedge>> entry : hedges.entrySet() ) {
        System.out.println( entry.getKey() );
        for ( Map.Entry<String, Hedge> entry2 : entry.getValue().entrySet() ) {
          System.out.println( "\t " + entry2.getKey() + " \t => " + entry2.getValue() );
        }
      }*/
      
      positions = Position.getPositions( activeGame, user, round, period );
      markets = Market.getMarkets( activeGame, round, Math.max( period, 1 ) );
    }
    
  }
  
  public boolean showForecasting() {
    return Constants.SHOW_FORECASTING;
  }

  public boolean isNegativeMarket() {
    Market market = getMarket( getPeriod(), 1 );
    return market == null ? false : market.isNegativeMarket();
  }
  
  public String getMarketMessage() {
    if ( activeGame.isInitialPeriod() || activeGame.isFinalPeriod() ) {
      return "";
    }
    return isNegativeMarket()
        ? "There is a surplus on the electricity market, making total demand negative. You have to place positive bids in order to buy electricity."
        : "There is a shortage on the electricity market, making total demand positive. You have to place negative bids in order to sell electricity."
    ;
  }
  
  public boolean showMarketInfo() {
    return activeGame.getShowMarketInfo();
  }
  
  public boolean showCostInfo() {
    return activeGame.getShowCostInfo();
  }
  
  public Game getActiveGame() {
    return activeGame;
  }

  public boolean disabled( int period ) {
    return activeGame.isInitialPeriod() || activeGame.getCurrentPeriod() != period;
  }

  public int getPeriod () {
    return activeGame.getCurrentPeriod();
  }
  
  public boolean isPeriod( int period ) {
    return Math.abs( period ) == getPeriod();
  }
  
  public boolean isPeriodGTE( int period ) {
    return Math.abs( period ) >= getPeriod();
  }
  
  public boolean isFinalPeriod() {
    return Game.getActiveGame().isFinalPeriod();
  }
  
  public boolean isPenultimatePeriod() {
    return isPenultimatePeriod( getPeriod() );
  }
  
  public boolean isPenultimatePeriod( int period ) {
    return Game.getActiveGame().isPenultimatePeriod( period );
  }
  
  public boolean isPenultimatePeriodPlus() {
    Game game = Game.getActiveGame();
    return game.isPenultimatePeriod() || game.isFinalPeriod();
  }

  public List<Integer> getPeriods ()
  {
    return activeGame.getAllPeriods();
  }
  
  public List<Integer> getHedgePeriods() {
    return activeGame.getHedgePeriods();
  }
  
  public String getPeriodName( int period ) {
    return activeGame.getPeriodName( period );
  }

  public String getHedgePeriodName( int period ) {
    return activeGame.getHedgePeriodName( period );
  }
  
  public int getRound () {
    return activeGame.getCurrentRound();
  }

  public List<Integer> getRounds() {
    return getRounds( -1 );
  }
  
  public List<Integer> getRounds( int limit ) {
    return activeGame.getValidRounds( limit );
  }
  
  public int getNumPeriods() {
    return activeGame.getNumPeriods();
  }
  
  public int getNumColumns() {
    return 5 + getNumPeriods(); // TODO ugly hardcoded term 5...
  }
  
  public Set<Plant> getPlants() {
    return plants;
  }
  
  public Map<PlantType, Map<String, Integer>> getMarketInfo() {
    Map<PlantType, List<Plant>> plants = activeGame.getPlants();
    if ( plants == null ) {
      return null;
    }
    Map<PlantType, Map<String, Integer>> result = new LinkedHashMap<PlantType, Map<String,Integer>>();
    for ( Map.Entry<PlantType, List<Plant>> entry : plants.entrySet() ) {
      PlantType type = entry.getKey();
      List<Plant> list = entry.getValue();
      Map<String, Integer> info = new LinkedHashMap<String, Integer>();
      info.put( "#Plants", list.size() );
      if ( list.size() > 0 ) {
        Plant plant = list.get( 0 );
        if ( type.isVariableOutput() ) {
          info.put( "Expected Output (MWh)", plant.getExpectedOutput() );
          info.put( "Output Std.Dev. (MWh)", (int) Math.round( plant.getOutputStandardDeviation() ) );
        } else {
          info.put( "Maximum Output (MWh)", plant.getExpectedOutput() );
        }
      }
      result.put( type,  info );
    }
    return result;
  }
  
  public Map<PlantType, Map<String, Integer>> getCostInfo() {
    Map<PlantType, List<Plant>> plants = activeGame.getPlants();
    if ( plants == null ) {
      return null;
    }
    Map<PlantType, Map<String, Integer>> result = new LinkedHashMap<PlantType, Map<String,Integer>>();
    for ( Map.Entry<PlantType, List<Plant>> entry : plants.entrySet() ) {
      PlantType type = entry.getKey();
      List<Plant> list = entry.getValue();
      Map<String, Integer> info = new LinkedHashMap<String, Integer>();
      if ( list.size() > 0 ) {
        Plant plant = list.get( 0 );
        if ( type.isVariableOutput() ) {
          info.put( "Marginal Cost (€ / MWh)", 0 );
        } else {
          info.put( "Marginal Cost (€ / MWh)", (int) Math.round( plant.getMarginalCostPrice() ) );
          info.put( "Ramping Cost (€ / MWh)", (int) Math.round( plant.getRampUpCost() ) );
        }
      }
      result.put( type,  info );
    }
    return result;
  }
  
  public int getNumPlants() {
    return plants == null ? 0 : plants.size();
  }
  
  public Collection<Metric> getMetrics() {
    return metrics == null ? null : metrics.values();
  }
  
  public boolean showMetricNames() {
    return metrics != null && metrics.size() > 1;
  }
  
  /** not used after all
  public String getTypeMap() {
    return typeMap;
  }
  */
  
  public String getTypeKeys() {
    return typeKeys;
  }
  
  public Map<Integer, Map<String, Forecast>> getForecasts()
  {
    return forecasts;
  }
  
  public Forecast getForecast( int period, int plantId, int metricId ) {
    String type = "p" + plantId + "_m" + metricId;
    Map<String, Forecast> map = forecasts.get( period ); 
    Forecast f = map == null ? null : map.get( type );
    return f;
  }

  public Integer getForecastsTotal( int period, int metricId ) {
    Map<String, Forecast> map = forecasts.get( period ); 
    if ( map == null || period > 0 && period >= getPeriod() ) {
      return null;
    }
    int total = 0;
    for ( Plant plant : plants ) {
      String key = "p" + plant.getPlantId() + "_m" + metricId;
      if ( map.containsKey( key ) ) {
        total += map.get( key ).getValue();
      }
    }
    return total;
  }
  
  public boolean isHedgeQuantityDisabled( int period, int plantId, int metricId ) {
    return getFixedHedgeQuantity( period, plantId, metricId ) != null;
  }
  
  public boolean isHedgePriceDisabled( int period, int plantId, int metricId ) {
    return getFixedHedgeQuantity( period, plantId, metricId ) != null;
  }
  
  private Integer getFixedHedgeQuantity( int period, int plantId, int metricId ) {
    period = Math.abs( period );
    if ( period != getPeriod() || period != activeGame.getNumPeriods() ) {
      return null;
    }
    for ( Plant plant : plants ) {
      if ( plant.getPlantId() == plantId ) {
        Integer finalPos = getFinalPositionNet( plantId, metricId );
        if ( plant.getType().isVariableOutput() ) {
          return - finalPos;
        }
        int pos = finalPos == null ? 0 : finalPos.intValue();
        int maxhedge = plant.getExpectedOutput() - pos;
        int minhedge = - pos;
        boolean negmarket = isNegativeMarket();
        if ( negmarket && maxhedge < 0  ||  !negmarket && minhedge > 0 ) {
          return - finalPos;
        }
        return null;
      }
    }
    return null;
  }
  
  public Map<String, Map<String, Hedge>> getHedges ()
  {
    return hedges;
  }
  
  public Hedge getHedge( int period, int plantId, int metricId ) {
    return getHedge( getRound(), period, plantId, metricId );
  }
  
  public Hedge getHedge( int round, int period, int plantId, int metricId ) {
    String type = "p" + plantId + "_m" + metricId;
    Map<String, Hedge> map = hedges.get( round + ":" + period );
    Hedge h = map == null ? null : map.get( type );
    if ( h != null && round == getRound() ) {
      for ( Plant plant : plants ) {
        if ( plant.getPlantId() == plantId ) {
          Integer fix = getFixedHedgeQuantity( period, plantId, metricId );
          if ( fix != null ) {
            h.setBidQuantity( fix );
            h.setSkip( true );
            Hedge.saveHedge( h );
          }
          break;
        }
      }
    }
    return h;
  }
  
  public String getHedgesClearingTotal( int period, int metricId ) {
    return getHedgesClearingTotal( getRound(), period, metricId );
  }
  
  public String getHedgesClearingTotal( int round, int period, int metricId ) {
    Map<String, Hedge> map = hedges.get( round + ":" + period ); 
    if ( map == null || round == getRound() && period > 0 && period >= getPeriod() ) {
      return "";
    }
    int total = 0;
    for ( Plant plant : plants ) {
      String key = "p" + plant.getPlantId() + "_m" + metricId;
      if ( map.containsKey( key ) ) {
        total += map.get( key ).getClearingQuantity();
      }
    }
    return "" + total;
  }
  
  public String getHedgesBidTotal( int period, int metricId ) {
    return getHedgesBidTotal( getRound(), period, metricId );
  }
  
  public String getHedgesBidTotal( int round, int period, int metricId ) {
    Map<String, Hedge> map = hedges.get( round + ":" + period ); 
    if ( map == null || round == getRound() && period > 0 && period > getPeriod() ) {
      return "";
    }
    int total = 0;
    for ( Plant plant : plants ) {
      String key = "p" + plant.getPlantId() + "_m" + metricId;
      if ( map.containsKey( key ) ) {
        total += map.get( key ).getBidQuantity();
      }
    }
    return "" + total;
  }
  
  public Integer getFinalPositionNet( int plantId, int metricId ) {
    int last = activeGame.getNumPeriods();
    Integer netpos = getPositionNet( last - 1, plantId, metricId );
    Integer delta = getPositionDelta( last, plantId, metricId );
    return netpos == null || delta == null ? null : netpos + delta;
  }
  
  private Position getPosition( int period, int plantId, int metricId ) {
    String type = "p" + plantId + "_m" + metricId;
    Map<String, Position> map = positions.get( period ); 
    if ( map == null || period > 0 && period >= this.getPeriod() ) {
      return null;
    }
    
    return map.get( type );
  }
  
  public Integer getPositionNet( int period, int plantId, int metricId ) {
    Position pos = getPosition( period, plantId, metricId ); 
    return pos == null ? null : pos.getNetValue() + getPositionDelta( period, plantId, metricId );
  }
  
  public Integer getPositionValue( int period, int plantId, int metricId ) {
    Position pos = getPosition( period, plantId, metricId );
    return pos == null ? null : pos.getValue();
  }
  
  public Integer getPositionDelta( int period, int plantId, int metricId ) {
    int delta = 0;;
    if ( metricId == 1 && period == getActiveGame().getNumPeriods() ) {
      for ( Plant plant : plants ) {
        if ( plant.getPlantId() == plantId && plant.getType().isVariableOutput() ) {
          delta = plant.getRealOutput() - plant.getExpectedOutput();
          break;
        }
      }
      //System.out.println(" period " + period + "; plant " + plantId + " metric " + metricId + "  delta " + delta );
    }
    return delta;
  }

  public Integer getPositionsNetTotal( int period, int metricId ) {
    Map<String, Position> map = positions.get( period );
    if ( map == null || period > 0 && period >= this.getPeriod() ) {
      return null;
    }
    int total = 0;
    for ( Plant plant : plants ) {
      String key = "p" + plant.getPlantId() + "_m" + metricId;
      if ( map.containsKey( key ) ) {
        total += map.get( key ).getNetValue();
        total += getPositionDelta( period, plant.getPlantId(), metricId );
      }
    }
    return total;
  }
  
  public Integer getPositionsCumTotal( int period, int metricId ) {
    Map<String, Position> map = positions.get( period ); 
    if ( map == null || period > 0 && period >= this.getPeriod() ) {
      return null;
    }
    int total = 0;
    for ( Plant plant : plants ) {
      String key = "p" + plant.getPlantId() + "_m" + metricId;
      if ( map.containsKey( key ) ) {
        total += map.get( key ).getValue();
      }
    }
    return total;
  }
  
  
  public Map<Integer, Map<String, Market>> getMarkets ()
  {
    return markets;
  }
  
  public Market getMarket( int period, int metricId ) {
    Map<String, Market> map = markets.get( period );
    if ( map != null && period > getPeriod() ) {
      map = null;
    }
    return map == null ? null : map.get( "m" + metricId );
  }
  
  public Market getMarket( int round, int period, int metricId ) {
    Map<Integer, Map<String, Market>> markets = Market.getMarkets( activeGame, round, period );
    Map<String, Market> map = markets.get( period );
    if ( map != null && round == getRound() && period > getPeriod() ) {
      map = null;
    }
    return map == null ? null : map.get( "m" + metricId );
  }
  
  public Integer getMarketSpread( int period, int metricId ) {
    if ( period > 0 && period >= this.getPeriod() ) {
      return null;
    }
    Metric metric = metrics.get( metricId );
    Market m0 = getMarket( period, powerMetricId );
    Market m1 = getMarket( period, metricId );
    if ( m0 == null || m1 == null ) {
      return null;
    }
    return (int) Math.round( m0.getPrice() - metric.getSpreadMultiplier() * m1.getPrice() );
  }
  
  public int getNumSpreads() {
    return numSpreads;
  }
  
  
  public Integer getHedgeMtM( int period, int plantId ) {
    return getHedgeMtM( getRound(), period, plantId );
  }
  
  public Integer getHedgeMtM( int round, int period, int plantId ) {
    Market m = getMarket( round, period, 1 );
    Hedge h = m == null ? null : getHedge( round, m.getLoad() > 0 ? period : -period, plantId, 1 );
    if ( h != null && round == getRound() && period >= getPeriod() ) {
      h = null;
    }
    return h == null ? null : -1 * h.getClearingPrice() * h.getClearingQuantity();
  }
  
  public Integer getCostMarginalMtM( int period, int plantId ) {
    return getCostMarginalMtM( getRound(), period, plantId );
  }
  
  public Integer getCostMarginalMtM( int round, int period, int plantId ) {
    Plant p = Plant.getPlantById( plantId );
    Market m = getMarket( round, period, 1 );
    Hedge h = p == null || m == null ? null : getHedge( round, m.getLoad() > 0 ? period : -period, plantId, 1 );
    if ( h != null && round == getRound() && period >= getPeriod() ) {
      h = null;
    }
    return h == null ? null : (int) Math.round( h.getClearingQuantity() * p.getMarginalCostPrice() );
  }
  
  public Integer getCostRampUpMtM( int period, int plantId ) {
    return getCostRampUpMtM( getRound(), period, plantId );
  }
  
  public Integer getCostRampUpMtM( int round, int period, int plantId ) {
    Plant p = Plant.getPlantById( plantId );
    Market m = getMarket( round, period, 1 );
    Hedge h = p == null || m == null ? null : getHedge( round, m.getLoad() > 0 ? period : -period, plantId, 1 );
    if ( h != null && round == getRound() && period >= getPeriod() ) {
      h = null;
    }
    return h == null ? null : ! isPenultimatePeriod( period ) ? 0 : -1 * (int) Math.round( Math.abs( h.getClearingQuantity() * p.getRampUpCost() ) );
  }

  public Integer getProfitMtM( int period, int plantId ) {
    return getProfitMtM( getRound(), period, plantId );
  }
  
  public Integer getProfitMtM( int round, int period, int plantId ) {
    Integer h = getHedgeMtM( round, period, plantId );
    Integer cm = getCostMarginalMtM( round, period, plantId );
    Integer cr = getCostRampUpMtM( round, period, plantId );
    return h == null ? null : cm == null || cr == null ? h : h + cm + cr;
  }
  
  public Integer getProfitPerPeriodMtMTotal( int round, int period ) {
    Integer total = null;
    for ( Plant plant : getPlants() ) {
      Integer delta = getProfitMtM( round, period, plant.getPlantId() );
      if ( delta == null ) {
        continue;
      }
      if ( total == null ) {
        total = delta;
      } else {
        total += delta;
      }
    }
    return total;
  }
  
  public Integer getProfitMtMTotal( int round, int period ) {
    Integer total = null;
    for ( int p = 0; p <= period; p++ ) {
      Integer delta = getProfitPerPeriodMtMTotal( round, p );
      if ( delta == null ) {
        continue;
      }
      if ( total == null ) {
        total = delta;
      } else {
        total += delta;
      }
    }
    return total;
  }
  
  public List<Event> getPublicEvents ()
  {
    List<Event> publicEvents = new LinkedList<Event>();

    for (Event event : EventService.getEvents()) {
      if (event.isShow()) {
        publicEvents.add(event);
      }
    }
    
    for ( Event event : Event.getEvents( activeGame ) ) {
      publicEvents.add( event );
    }

    if (publicEvents.isEmpty()) {
      publicEvents.add(new Event(0, 0, 0, "event", "No events yet"));
    }

    return publicEvents;
  }
  
  public void save ()
  {
    User user = User.getCurrentUser();
    
    int round = activeGame.getCurrentRound();
    int period = activeGame.getCurrentPeriod();
    Map<String, Market> markets = Market.getMarkets( activeGame, round, period ).get( period );
    Market market = markets.get( "m1" );
    
    user.touch();
    
    if ( user == null || activeGame.isInitialPeriod() ) {
      return;
    }
    
    boolean saved1 = true;
    Map<String, Forecast> fmap = forecasts.get( period );
    if ( fmap != null ) {
      for ( Forecast f : fmap.values() ) {
        saved1 = saved1 && Forecast.saveForecast( f );
      }
    }
    
    
    if (!saved1) {
      Utils.growlMessage("Error", "Saving forecasts failed!");
    }
    
    boolean saved2 = true;
    Map<String, Hedge> hmap = hedges.get( getRound() + ":" + period );
    if ( hmap != null ) {
      for ( Hedge h : hmap.values() ) {
        Integer fix = getFixedHedgeQuantity( h.getPeriod(), h.getPlantId(), h.getMetricId() );
        if ( fix != null ) {
          h.setBidPrice( 0 );
          h.setBidQuantity( fix );
          h.setSkip( true );
        } else if ( market.isNegativeMarket() ) {
          h.setSkip( true );
        }
        saved2 = saved2 && Hedge.saveHedge( h );
      }
    }
    hmap = hedges.get( getRound() + ":" + -period );
    if ( hmap != null ) {
      for ( Hedge h : hmap.values() ) {
        Integer fix = getFixedHedgeQuantity( h.getPeriod(), h.getPlantId(), h.getMetricId() );
        if ( fix != null ) {
          h.setBidPrice( 0 );
          h.setBidQuantity( fix );
          h.setSkip( true );
        } else if ( ! market.isNegativeMarket() ) {
          h.setSkip( true );
        }
        saved2 = saved2 && Hedge.saveHedge( h );
      }
    }
    if (!saved2) {
      Utils.growlMessage("Error", "Saving hedges failed!");
    }

    if (saved1 && saved2) {
      // Update in admin view
      AdminResource.sendMessage("MissingSaves:" + ActionAdmin.notSavedString());
      // Notify user of succesful save
      Utils.growlMessage( FacesMessage.SEVERITY_INFO, "Info", "All values were saved." );
    }
  }

  public void exportCurrentGame () {
    Utils.growlMessage( "Error", "Sorry, export not yet functional in this version..." );
    /*
    User user = User.getCurrentUser();
    if (user == null) {
      return;
    }

    Exporter exporter = new Exporter( user, activeGame );
    exporter.createWorkbook();
    exporter.serveWorkbook();
    */
  }
}
