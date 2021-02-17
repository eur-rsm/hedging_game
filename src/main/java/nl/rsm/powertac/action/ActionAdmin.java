package nl.rsm.powertac.action;

import nl.rsm.powertac.Exporter;
import nl.rsm.powertac.endpoints.NotifyResource;
import nl.rsm.powertac.endpoints.RefreshResource;
import nl.rsm.powertac.model.Event;
import nl.rsm.powertac.model.EventService;
import nl.rsm.powertac.model.Game;
import nl.rsm.powertac.model.Market;
import nl.rsm.powertac.model.Metric;
import nl.rsm.powertac.model.Parameter;
import nl.rsm.powertac.model.Plant;
import nl.rsm.powertac.model.PlantType;
import nl.rsm.powertac.model.Position;
import nl.rsm.powertac.model.User;
import nl.rsm.powertac.util.Constants;
import nl.rsm.powertac.util.PlantDef;
import nl.rsm.powertac.util.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;


@ManagedBean
@RequestScoped
public class ActionAdmin
{
  
  Game activeGame;
  
  String typesJSON;
  
  Map<Integer, Boolean> activeGames;
  Map<Integer, Boolean> marketInfoGames;
  Map<Integer, Boolean> costInfoGames;
  List<PlantDef> plantSetup;
  
  Game newGame;
  HashMap<Integer, String> newGamePlantsCount;
  HashMap<Integer, String> newGamePlantsCapacity;
  HashMap<Integer, List<String>> newGamePlantsParams;
  Part newGameMarket;
  
  String customEventTitle;
  String customEvent;
  int customDelay;
  
  static final String CSV_PRE = "price";
  static final String[] CSV_COLS = new String[] {"load", "forecast", "sigma" };
  
  public ActionAdmin () {
    User user = User.getCurrentUser();
    
    activeGame = Game.getActiveGame();
    
    if (user == null || !user.isAdmin()) {
      return;
    }
    
    newGamePlantsCount = new HashMap<Integer, String>();
    newGamePlantsCapacity = new HashMap<Integer, String>();
    newGamePlantsParams = new HashMap<Integer, List<String>>();
    initNewGame();
    
    activeGames = new HashMap<Integer, Boolean>();
    marketInfoGames = new HashMap<Integer, Boolean>();
    costInfoGames = new HashMap<Integer, Boolean>();
    for ( Game game : Game.getAllGames() ) {
      activeGames.put( game.getGameId(), game.getActive() );
      marketInfoGames.put( game.getGameId(), game.getShowMarketInfo() );
      costInfoGames.put( game.getGameId(), game.getShowCostInfo() );
    }
    plantSetup = new LinkedList<PlantDef>();
    
   
    
    long inactive = user.inactiveSeconds();
    if ( inactive > Constants.MAX_INACTIVE_SECONDS ) {
      Utils.growlMessage("Warning", "Inactive for too long, please log in again.");
      user.logout();
      return;
    }
    
    List<PlantType> plants = PlantType.getAllPlantTypes();
    
    typesJSON = "{";
    for ( PlantType plant : plants ) {
      typesJSON += "\"p" + plant.getTypeId() + "\":{"
          + "\"name\":\"" + plant.getName() + "\""
          + ",\"metrics\":{";
      for ( Metric metric : plant.getMetrics() ) {
        typesJSON += "\"m" + metric.getMetricId() + "\":{"
            + "\"name\":\"" + metric.getName() + "\""
            + ",\"unit\":\"" + metric.getUnit() + "\""
            + ",\"sign\":\"" + metric.getMultiplier() + "\""
            + "},";
      }
      typesJSON = typesJSON.substring( 0, typesJSON.length() - 1 ) + "}},";
    }
    typesJSON = typesJSON.substring( 0, typesJSON.length() - 1 ) + "}";
    
  }
  
  private void initNewGame() {
    newGame = new Game();
    for ( PlantType type : getPlantTypes() ) {
      newGamePlantsCount.put( type.getTypeId(), "1" );
      newGamePlantsCapacity.put( type.getTypeId(), "1000" );
      ArrayList<String> params = new ArrayList<String>();
      for ( Parameter param : type.getParameters() ) {
        params.add( param.getValue() );
      }
      newGamePlantsParams.put( type.getTypeId(), params );
      newGameMarket = null;
    }
  }
  
  
  public String getPlantTypesJSON() {
    return typesJSON;
  }
  
  public List<PlantType> getPlantTypes() {
    return PlantType.getAllPlantTypes();
  }
  
  public Game getNewGame() {
    return newGame;
  }
  
  public Map<Integer, Boolean> getActiveGames() {
    return activeGames;
  }
  
  public Map<Integer, Boolean> getMarketInfoGames() {
    return marketInfoGames;
  }
  
  public Map<Integer, Boolean> getCostInfoGames() {
    return costInfoGames;
  }
  
  public void activateGame( int gameId ) {
    activeGames.put( gameId, true );
    for ( int id : activeGames.keySet() ) {
      activeGames.put( id, id == gameId );
    }
    activeGame = Game.getGameById( gameId );
    User.getCurrentUser().setGameId( gameId );
    Game.setActiveGame( activeGame );
  }
  
  public void exportGame( int gameId ) {
    User user = User.getCurrentUser();
    if (user == null) {
      return;
    }
    
    Exporter exporter = new Exporter( user, Game.getGameById( gameId ) );
    exporter.createWorkbook();
    exporter.serveWorkbook();
    
  }
  
  public void exportCurrentGame() {
    exportGame( activeGame.getGameId() );
  }
  
  public String getPlantSetup() {
    return "";
  }
  
  public void setPlantSetup( String string ) {
    if ( string == null || string.isEmpty() ) {
      Utils.growlMessage( "Error", "You should add at least one plant" );
      return;
    }
    String[] split1 = string.split( ";" );
    
    for ( String s : split1 ) {
      plantSetup.add( new PlantDef( s ) );
    }
  }
  
  public void createGame() {
    if ( newGame.getName().trim().isEmpty() ) {
      Utils.growlMessage( "Error", "Please enter a non-empty name" );
      return;
    }
    
    // Process CSV upload (market)
    List<Market> marketSetup = new LinkedList<Market>();
    try {
      if ( newGameMarket.getSize() <= 0 ) {
        throw new IllegalArgumentException( "Empty or missing market file" );
      }
      
      
      InputStream input = newGameMarket.getInputStream();
      CsvSchema schema = CsvSchema.emptySchema().withHeader();
      CsvMapper mapper = new CsvMapper();
      ObjectReader reader = mapper.readerFor( Map.class ).with( schema );
      MappingIterator<Map<String, String>> iterator = reader.readValues( input );
      int rounds = 0;
      int periods = 0;
      while ( iterator.hasNext() ) {
        periods = 0;
        Map<String,String> row = iterator.next();
        Market market = null;
        rounds++;
        for ( Map.Entry<String, String> entry : row.entrySet() ) {
          String key = entry.getKey().toLowerCase().trim();
          if ( key.equals( CSV_PRE ) ) {
            double price = Double.parseDouble( entry.getValue() );
            marketSetup.add( new Market( -1, 1, rounds - 1, 0, price, 0, 0, 0 ) );
            continue;
          }
          int period = 0;
          for ( String col : CSV_COLS ) {
            if ( key.startsWith( col ) ) {
              period = Integer.parseInt( key.substring( col.length() ).trim() );
              key = col;
              break;
            }
          }
          if ( period == 0 ) {
            throw new IllegalArgumentException( "Unexpected column " + key );
          }
          if ( period > periods ) {
            periods = period;
            if ( market != null ) {
              marketSetup.add( market );
            }
            market = new Market( -1, 1, rounds - 1, period, 0D, 0D, 0D, 0D ); 
          }
          double value = Double.parseDouble( entry.getValue() );
          if ( key.equals( "load" ) ) {
            market.setLoad( (int) Math.round( value ) );
          } else if ( key.equals( "forecast" ) ) {
            market.setForecast( (int) Math.round( value ) );
          } else {
            // sigma
            if ( market.getPeriod() == 1 ) {
              // Force to 0 for T1
              value = 0;
            }
            market.setSigma( (int) Math.round( value ) );
          }
        }
        if ( market != null ) {
          marketSetup.add( market );
        }
      }
      newGame.setNumPeriods( periods );
      newGame.setNumRounds( rounds );
    } catch ( Exception x ) {
      Utils.growlMessage( "Error", "Failed to process CSV: " + x.getMessage() );
      x.printStackTrace();
      return;
    }
    
    // Process plant setup (types and counts) and associated parameters
    Random random = new Random();
    int total = 0;
    int enabled = 0;
    List<PlantDef> plantSetup = new LinkedList<PlantDef>();
    Map<Integer, List<List<Parameter>>> plantParams = new HashMap<Integer, List<List<Parameter>>>();
    try {
      for ( Map.Entry<Integer, String> entry : newGamePlantsCount.entrySet() ) {
        // Map input to plantSetup (create plants of given count and types),
        int typeId = entry.getKey();
        PlantType type = PlantType.getPlantTypeById( typeId );
        Integer count = Integer.parseInt( entry.getValue() );
        Integer capacity = Integer.parseInt( newGamePlantsCapacity.get( type.getTypeId() ) );
        if ( count > 0 && capacity > 0 ) {
          enabled++;
        } else {
          continue;
        }
        
        Map<Metric, Integer> values = new HashMap<Metric, Integer>();
        values.put( type.getMetrics().get(0), capacity );
        for ( int i = 0; i < count; i++ ) {
          plantSetup.add( new PlantDef( type, values ) );
        }
        total += count;
        
        List<List<Parameter>> list = new ArrayList<List<Parameter>>( count );
        // Map input to plantParams (depending on plant type)
        
        Integer out = null;
        
        for ( int j = 0; j < count; j++ ) {
          List<Parameter> params = new ArrayList<Parameter>();
          List<Parameter> typeParams = type.getParameters();
          Parameter typeParam;
          Parameter plantParam;
          double dev = Double.NaN;
          for ( int i = 0; i < typeParams.size(); i++ ) {
            typeParam = typeParams.get( i );
            plantParam = new Parameter( typeParam );
            plantParam.setValue( newGamePlantsParams.get( type.getTypeId() ).get( i ) );
            params.add( plantParam );
            if ( plantParam.getName().toLowerCase().indexOf( "dev" ) >= 0 ) {
              dev = Double.parseDouble( plantParam.getValue() );
            }
          }
          if ( ! Double.isNaN( dev ) ) {
            for ( int i = 0; i < params.size(); i++ ) {
              plantParam = params.get( i );
              if ( plantParam.getName().toLowerCase().indexOf( "real" ) >= 0 ) {
                if ( out == null ) {
                  out = (int) Math.round( Math.round( capacity + random.nextGaussian() * dev ) );
                }
                plantParam.setValue( "" + out );
              }
            }
          }
          list.add( params );
        }
        plantParams.put( type.getTypeId(), list );
      }
    } catch ( NumberFormatException x ) {
      Utils.growlMessage( "Error", "Please enter numerical values for plant setup fields" );
      return;
    } catch ( IllegalArgumentException x ) {
      Utils.growlMessage( "Error", "Illegal parameter value: " + x.getMessage() );
      return;
    }
    if ( total == 0 ) {
      Utils.growlMessage( "Error", "You'll need one or more plants, surely?" );
      return;
    }
    if ( newGamePlantsCapacity.size() < newGame.getNumPlants() ) {
      Utils.growlMessage( "Error", "You can't have more plants/user than you have enabled types of plants!" );
      return;
    }
    
    try {
      newGame.save();
    } catch ( Exception x ) {
      Utils.growlMessage( "Error", "Failed to insert game: " + x.getMessage() );
      return;
    }
    
    newGame.setPlantSetup( plantSetup, plantParams );
    
    for ( Market market : marketSetup ) {
      market.setGameId( newGame.getGameId() );
    }
    Market.saveMarkets( marketSetup );
    
    if ( newGame.getActive() ) {
      activateGame( newGame.getGameId() );
    }
    
    initNewGame();
    
    // TODO this redirect is necessary because otherwise the html generated
    // by this call contains dupe IDs for some weird reason...
    Utils.redirect( "admin.xhtml" );
  }

  public List<Game> getGames() {
    return Game.getAllGames();
  }
  
  public Map<PlantType,List<Plant>> getGamePlants( Game game ) {
    return Plant.getPlantsByGame( game );
  }
  
  public Integer getGamePlantValue( Game game, Plant plant, Metric metric ) {
    return Plant.getGamePlantValue( game, plant, metric );
  }
  
  public String getRound() {
    return "" + activeGame.getCurrentRound();
  }

  public void setRound( String round ) {
    if ( round == null ) {
      return;
    }
    int nextRound = Integer.parseInt( round );
    int currentRound = activeGame.getCurrentRound();
    Random random = new Random();
    while ( nextRound > currentRound ) {
      while ( ! activeGame.isFinalPeriod() ) {
        setPeriod( "" + activeGame.getCurrentPeriod() + 1, false );
      }
      currentRound++;
      activeGame.setCurrentRound( currentRound );
      activeGame.setCurrentPeriod( 0 );
      try {
        activeGame.save();
      } catch ( Exception x ) {
        x.printStackTrace( System.err );
        throw new RuntimeException( "Unhandled exception: " + x.getMessage() );
      }
      
      Map<PlantType, Integer> argh = new HashMap<PlantType, Integer>();
      for ( Map.Entry<PlantType, List<Plant>> entry : activeGame.getPlants().entrySet() ) {
        if ( entry.getKey().isVariableOutput() ) {
          for ( Plant plant : entry.getValue() ) {
            Integer out = argh.get( plant.getType() );
            if ( out == null ) {
              out = (int) Math.round( random.nextGaussian() * plant.getOutputStandardDeviation() + plant.getExpectedOutput() );
              argh.put( plant.getType(), out );
            }
            plant.setRealOutput( out );
          }
        }
      }
      
      
      if ( currentRound == nextRound ) {
        RefreshResource.sendMessage( "all" );
        if ( activeGame.isFinalRound() ) {
          NotifyResource.sendMessage("*", "Finished!", "The game has ended");
        } else {
          NotifyResource.sendMessage("*", "New round!", "Started : R" + (nextRound + 1) );
        }
      }
    }
  }
  
  public boolean isFinalPeriod() {
    return activeGame.isFinalPeriod();
  }
  
  public String getPeriod() {
    return "" + activeGame.getCurrentPeriod();
  }

  public void setPeriod( String period ) {
    setPeriod( period, true );
  }
  
  private void setPeriod( String period, boolean notify ) {
    if ( period == null ) {
      return;
    }
    int nextPeriod = Integer.parseInt( period );
    int currentRound = activeGame.getCurrentRound();
    int currentPeriod = activeGame.getCurrentPeriod();
    while ( nextPeriod > currentPeriod ) {
      
      Market.emulateMarkets( activeGame, currentRound, currentPeriod );
      
      Position.updatePositions( activeGame, currentRound, currentPeriod );
      
      currentPeriod++;
      
      activeGame.setCurrentPeriod( nextPeriod );
      try {
        activeGame.save();
      } catch ( Exception x ) {
        x.printStackTrace( System.err );
        throw new RuntimeException( "Unhandled exception: " + x.getMessage() );
      }
      
      // In penultimate period, subtract positions of variable output plants from
      // total market demand
      // And add unmet demand from previous period.
      if ( activeGame.isPenultimatePeriod() ) {
        
        Map<Integer, Map<String, Market>> markets = Market.getMarkets( activeGame, currentRound, nextPeriod );
        if ( markets == null || ! markets.containsKey( currentPeriod ) ) {
          throw new RuntimeException( "Unexpected missing market for round " + currentRound + " period " + nextPeriod );
        }
        Market market = markets.get( currentPeriod ).get( "m1" );
        if ( market == null ) {
          throw new RuntimeException( "Unexpected missing market for round " + currentRound + " period " + nextPeriod + " metric 1");
        }
        
        Market prev = markets.get( currentPeriod - 1 ).get( "m1" );
        
        int delta = prev.getCleared() - (int)Math.round( prev.getLoad() );
        market.setForecast( market.getForecast() - delta );
        for ( User user : activeGame.getUsers() ) {
          for ( Plant plant : user.getPlants() ) {
            if ( plant.getType().isVariableOutput() ) {
              Map<Integer, Map<String, Position>> positions = Position.getPositions( activeGame, user, currentRound, currentPeriod );
              if ( positions == null || ! positions.containsKey( currentPeriod ) ) {
                throw new RuntimeException( "Unexpected missing position for user " + user.getUserId() + " : round/period " + currentRound + " / " + currentPeriod );
              }
              Position pos = positions.get( currentPeriod - 1 ).get( "p" + plant.getPlantId() + "_m1" );
              int diff = pos.getNetValue() + plant.getRealOutput() - plant.getExpectedOutput();
              delta += diff;
            }
          }
        }
        double load = market.getLoad();
        // System.out.println( "set market load " + load + " --> " + (load - delta) );
        market.setLoad( load - delta );
        Market.saveMarket( market );
      }
      
      if ( currentPeriod == nextPeriod && notify ) {
        RefreshResource.sendMessage( "all" );
        if ( activeGame.isFinalPeriod() ) {
          NotifyResource.sendMessage("*", "Finished!", "The round has ended");
        } else {
          NotifyResource.sendMessage("*", "New period!", "Started : T" + nextPeriod );
        }
      }
      EventService.resetCounter();
    }
  }
  
  public List<Integer> getPeriods ()
  {
    return activeGame.getAllPeriods();
  }
  
  public int getNumRounds() {
    return activeGame.getNumRounds();
  }
  
  public int getNumPeriods() {
    return activeGame.getNumPeriods();
  }
  
  public HashMap<Integer, String> getNewGamePlantsCapacity() {
    return newGamePlantsCapacity;
  }

  public HashMap<Integer, String> getNewGamePlantsCount() {
    return newGamePlantsCount;
  }

  public HashMap<Integer, List<String>> getNewGamePlantsParams() {
    return newGamePlantsParams;
  }
  
  public void downloadMarket( Game game ) {
    
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ExternalContext externalContext = facesContext.getExternalContext();
    HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
    
    BufferedOutputStream output = null;
    SequenceWriter writer = null;
    try {
      output = new BufferedOutputStream( response.getOutputStream() );
      
      response.reset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
      response.setContentType( "text/csv" ); // Check http://www.iana.org/assignments/media-types for all types. Use if necessary ServletContext#getMimeType() for auto-detection based on filename.
      response.setHeader( "Content-disposition", "attachment; filename=\"" +
        "game" + game.getGameId() + "_market.csv\"" );
      
      CsvMapper mapper = new CsvMapper();
      CsvSchema.Builder builder = CsvSchema.builder();
      for ( int p = 1; p <= game.getNumPeriods(); p++ ) {
        builder.addNumberColumn( "Load" + p );
        builder.addNumberColumn( "Forecast" + p );
        builder.addNumberColumn( "Sigma" + p );
      }
      CsvSchema schema = builder.build().withHeader();
      writer = mapper.writer( schema ).writeValues( output );
      
      List<Market> markets = Market.getMarkets( game );
      Map<String, String> row = new HashMap<String, String>();
      int round = 0;
      for ( Market market : markets ) {
        if ( market.getRound() > round ) {
          if ( round > 0 ) {
            writer.write( row );
            row.clear();
          }
          round = market.getRound();
        }
        row.put( "Load" + market.getPeriod(), "" + market.getLoad() );
        row.put( "Forecast" + market.getPeriod(), "" + market.getForecast() );
        row.put( "Sigma" + market.getPeriod(), "" + market.getSigma() );
      }
      if ( round > 0 ) {
        writer.write( row );
        row.clear();
      }
      
      writer.close();
      output.close();
    } catch ( Throwable x ) {
      Utils.growlMessage( "Error", "Failed to prepare download: " + x.getMessage() );
      x.printStackTrace();
    }    
    facesContext.responseComplete();
  }
  
  public Part getNewGameMarket() {
    return newGameMarket;
  }
  
  public void setNewGameMarket( Part newGameMarket ) {
    this.newGameMarket = newGameMarket;
  }
  
  public String notSaved()
  {
    return notSavedString();
  }

  public static String notSavedString ()
  {
    Game game = Game.getActiveGame();
    if ( game.isInitialPeriod() ) {
      return "First period; nothing to save";
    }
    if ( game.isFinalPeriod() ) {
      return "Final period; nothing to save";
    }
    
    List<User> missingUsers = User.notSavedUsers( game
        , game.getCurrentRound()
        , game.getCurrentPeriod()
    );

    if (missingUsers == null) {
      return "Oops, something went wrong!";
    }
    
    // Filter out users that only have variable output plants (nothing to save)
    if ( game.getCurrentPeriod() == game.getNumPeriods() ) {
      for ( Iterator<User> it = missingUsers.iterator(); it.hasNext(); ) {
        User user = it.next();
        Set<Plant> plants = user.getPlants();
        int count = plants.size();
        for ( Plant plant : plants ) {
          if ( plant.getType().isVariableOutput() ) {
            count--;
          }
        }
        if ( count == 0 ) {
          it.remove();
        }
      }
    }
    
    
    if (missingUsers.isEmpty()) {
      return "All users saved their input";
    }

    StringBuilder builder = new StringBuilder("These users need to save : ");
    for (User user : missingUsers) {
      builder.append(user.getUsername()).append(", ");
    }
    builder.setLength(builder.length() - 2);
    return builder.toString();
  }

  public void encourage ()
  {
    for (User user : User.notSavedUsers( activeGame
        , activeGame.getCurrentRound()
        , activeGame.getCurrentPeriod() )
    ) {
      NotifyResource.sendMessage(user.getUsername(), "Please save!",
          "Your data needs to be saved so the game can proceed.");
    }
  }
  
  public boolean encourageDisabled()
  {
    if ( activeGame.isInitialPeriod() || activeGame.isFinalPeriod() ) {
      return true;
    }

    List<User> missingUsers = User.notSavedUsers( activeGame
        , activeGame.getCurrentRound()
        , activeGame.getCurrentPeriod()
    );

    if (missingUsers == null || missingUsers.isEmpty()) {
      return true;
    }

    return false;
  }

  public SortedSet<Event> getEvents () {
    return EventService.getEvents();
  }
  
  public String getCustomEvent() {
    return customEvent;
  }
  
  public void setCustomEvent( String customEvent ) {
    this.customEvent = customEvent;
  }
  
  public String getCustomEventTitle() {
    return customEventTitle;
  }
  
  public void setCustomEventTitle( String customEventTitle ) {
    this.customEventTitle = customEventTitle;
  }
  
  public String getCustomDelay() {
    return "" + customDelay;
  }
  
  public void setCustomDelay( String delay ) {
    this.customDelay = Integer.parseInt( delay );
  }
  
  public List<Event> getCustomEvents()
  {
    List<Event> customEvents = new LinkedList<Event>();
    
    for ( Event event : Event.getEvents( activeGame ) ) {
      customEvents.add( event );
    }
    
    if ( customEvents.isEmpty() ) {
      customEvents.add( new Event( 0, 0, 0, "event", "No events yet" ) );
    }
    
    return customEvents;
  }
  
  public void sendCustomEvent() {
    Event event = new Event( activeGame.getGameId(), activeGame.getCurrentRound()
        , activeGame.getCurrentPeriod(), customDelay, customEventTitle, customEvent );
    event.save();
    event.setShow( true );
    customDelay = 0;
    customEvent = "";
    
    // TODO this redirect is necessary because otherwise the html generated
    // by this call contains dupe IDs for some weird reason...
    Utils.redirect( "game.xhtml" );
  }
  
  
  
}
