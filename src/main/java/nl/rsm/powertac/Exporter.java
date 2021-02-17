package nl.rsm.powertac;

import nl.rsm.powertac.model.Forecast;
import nl.rsm.powertac.model.Game;
import nl.rsm.powertac.model.Hedge;
import nl.rsm.powertac.model.Market;
import nl.rsm.powertac.model.Metric;
import nl.rsm.powertac.model.Parameter;
import nl.rsm.powertac.model.Plant;
import nl.rsm.powertac.model.PlantType;
import nl.rsm.powertac.model.User;
import nl.rsm.powertac.util.Constants;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Exporter
{
  private User user;
  private Game game;
  private HSSFWorkbook workbook;
  private HSSFSheet sheet;

  private static String resourceUser = "template.user.xls";
  private static String resourceAdmin = "template.admin.xls";
  private static String name = "hedging_game.xls";
  private static String temp = "hedging_game";
  
  private static final int ROW_GAME = 1;
  private static final int ROW_USER = 2;
  private static final int ROW_EMPTY = 3;
  private static final int ROW_ROUND = 4;
  private static final int ROW_TITLE = 5;
  private static final int ROW_DATA = 6;
  private static final int ROW_TOTAL = 7;
  private static final int ROW_TEMPLATE_END = 7;
  
  
  public Exporter (User user, Game game ) {
    this.user = user;
    this.game = game;
  }

  public void createWorkbook ()
  {
    try {
      InputStream input =
          Exporter.class.getClassLoader().getResourceAsStream(
              user.isAdmin() ? resourceAdmin : resourceUser);
      workbook = new HSSFWorkbook(input);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    sheet = workbook.getSheet("Game");

    if (user.isAdmin()) {
      fillDataAdmin();
    }
    else {
      fillDataUser();
    }
  }

  private void fillDataUser ()
  {
    if ( true ) {
      throw new RuntimeException( "Not implemented" );
    }
    if (workbook == null || sheet == null) {
      return;
    }
    
    Set<Plant> plants = user.getPlants();
    Map<Integer, List<Metric>> plantMetrics = new HashMap<Integer, List<Metric>>();
    Set<Metric> metrics = new LinkedHashSet<Metric>();
    
    int numcells = 5 + game.getNumPeriods();
    
    // For all template rows, insert cells to match the number of periods;
    // also copy formatting
    for ( int i = 0; i <= ROW_TEMPLATE_END; i++ ) {
      for ( int j = 5; j < numcells; j++ ) {
        HSSFCell src = sheet.getRow( i ).getCell( 4 );
        HSSFCell dst = sheet.getRow( i ).createCell( j );
        dst.setCellStyle( src.getCellStyle() );
      }
    }
    
    // Set the name of the game
    sheet.getRow( ROW_GAME ).getCell( 0 ).setCellValue( game.getName() );
    
    // Set the date of the name
    sheet.getRow( ROW_GAME ).getCell( 2 ).setCellValue( game.getStamp() );
    
    // Set the ID of the user
    sheet.getRow( ROW_USER ).getCell( 0 ).setCellValue( "UID: " + user.getUserId() );
    
    // Set the name of the user
    sheet.getRow( ROW_USER ).getCell( 2 ).setCellValue( user.getUsername() );
    
    // Fill in period labels in title template row
    for ( int j = 5; j < numcells; j++ ) {
      sheet.getRow( ROW_TITLE ).getCell( j ).setCellValue( "T" + (j-4) );
    }
    
    int index = ROW_EMPTY;
    int shift = 0;
    int numplants = plants.size();
    int numrows = plants.size(); // 1 empty row after each plant
    for ( Plant plant : plants ) {
      PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
      List<Metric> metrix = type.getMetrics();
      plantMetrics.put( plant.getTypeId(), metrix );
      numrows += metrix.size(); // for each plant: 1 row for each metric
      metrics.addAll( metrix );
    }
    int nummetrics = metrics.size();
    if ( plants.size() > 1 ) {
      numrows++; // 1 more empty row after totals
      numrows += metrics.size(); // 1 row for each metric 
    }
    
    for ( int round = 0; round <= game.getCurrentRound(); round++ ) {
    
      // Shift everything down by 1 row for round row
      sheet.shiftRows( index, ROW_TEMPLATE_END, 1 );
      shift +=1;
      // Create round row from template
      HSSFRow srcrow = sheet.getRow( ROW_ROUND + shift );
      HSSFRow dstrow = sheet.createRow( index );
      dstrow.setHeight( srcrow.getHeight() );
      
      for ( int j = 0; j < numcells; j++ ) {
        HSSFCell src = srcrow.getCell( j );
        HSSFCell dst = dstrow.createCell( j );
        dst.setCellStyle( src.getCellStyle() );
      }
      sheet.getRow( index ).getCell( 0 ).setCellValue( "Round #" + (round + 1) );
      index++;
      
      
      // ------ Forecasting section ------
      
      
      if ( Constants.SHOW_FORECASTING ) {
        
        // Shift everything down by numrows
        sheet.shiftRows( index, ROW_TEMPLATE_END + shift, numrows );
        shift += numrows;
        
        // Create title row from template
        sheet.createRow( index );
        for ( int j = 0; j < numcells; j++ ) {
          HSSFCell src = sheet.getRow( ROW_TITLE + shift ).getCell( j );
          HSSFCell dst = sheet.getRow( index ).createCell( j );  
          dst.setCellStyle( src.getCellStyle() );
        }
        sheet.getRow( index ).getCell( 0 ).setCellValue( "Forecasting" );
        index++;
        
        // Get the data
        Map<Integer, Map<String, Forecast>> forecasts
        = Forecast.getForecasts( game, user, round, game.getNumPeriods() );
        
        
        // For each plant:
        for ( Plant plant : plants ) {
          boolean first = true;
          PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
          // For each metric
          for ( Metric metric : type.getMetrics() ) {
            // Create data row from template
            srcrow = sheet.getRow( ROW_DATA + shift );
            dstrow = sheet.createRow( index );
            dstrow.setHeight( srcrow.getHeight() );
            
            for ( int j = 0; j < numcells; j++ ) {
              HSSFCell src = srcrow.getCell( j );
              HSSFCell dst = dstrow.createCell( j );
              dst.setCellStyle( src.getCellStyle() );
            }
            
            // For first row of each plant show its name 
            // TODO and capacity!
            if ( first ) {
              dstrow.getCell( 0 ).setCellValue( type.getName() );
              first = false;
            }
            // Set metric name and units
            dstrow.getCell( 2 ).setCellValue( metric.getName() );
            dstrow.getCell( 3 ).setCellValue( metric.getUnit() );
            
            // Now fill in the data
            String key = "p" + plant.getTypeId() + "_m" + metric.getMetricId();
            for ( int j = 0; j <= game.getNumPeriods(); j++ ) {
              Map<String, Forecast> map = forecasts.get( j );
              if ( map != null ) {
                Forecast forecast = map.get( key );
                if ( forecast != null ) {
                  dstrow.getCell( 4 + j ).setCellValue( forecast.getValue() );
                }
              }
            }
          }
        }
      }
    
    }
    
    /* TODO rewrite this to reflect single plant per user
    for (Forecast forecast : Forecast.getForecasts(user, period)) {
      if (forecast.getPeriod().equals(T5)) {
        continue;
      }

      int number = 4 + Integer.valueOf(forecast.getPeriod().name().substring(1, 2));

      
      sheet.getRow(2).getCell(number).setCellValue(forecast.getaPower());
      sheet.getRow(4).getCell(number).setCellValue(forecast.getbPower());
      sheet.getRow(6).getCell(number).setCellValue(forecast.getcPower());
      sheet.getRow(7).getCell(number).setCellValue(forecast.getcCoal());
      sheet.getRow(9).getCell(number).setCellValue(forecast.getdPower());
      sheet.getRow(10).getCell(number).setCellValue(forecast.getdGas());
      
    }
    
    for (Hedge hedge : Hedge.getHedges(userId, period).values()) {
      if (hedge.getPeriod().equals(T5)) {
        continue;
      }

      int number = 4 + Integer.valueOf(hedge.getPeriod().substring(1, 2));

      sheet.getRow(17).getCell(number).setCellValue(hedge.getaPower());
      sheet.getRow(19).getCell(number).setCellValue(hedge.getbPower());
      sheet.getRow(21).getCell(number).setCellValue(hedge.getcPower());
      sheet.getRow(22).getCell(number).setCellValue(hedge.getcCoal());
      sheet.getRow(24).getCell(number).setCellValue(hedge.getdPower());
      sheet.getRow(25).getCell(number).setCellValue(hedge.getdGas());
    }

    for (Market market : Market.getMarkets(userId, period).values()) {
      int number = 4 + Integer.valueOf(market.getPeriod().substring(1, 2));

      sheet.getRow(30).getCell(number).setCellValue(market.getPower());
      sheet.getRow(31).getCell(number).setCellValue(market.getCoal());
      sheet.getRow(32).getCell(number).setCellValue(market.getGas());
    }
    */
    
  }

  private void fillDataAdmin() {
    
    
    Map<User, List<Plant>> userPlants = game.getUserPlants();
    List<User> users = new LinkedList<User>();
    users.addAll( userPlants.keySet() );
    Map<PlantType, List<Plant>> plants = Plant.getPlantsByGame( game );
    
    final HashMap<User, Long> profits = new HashMap<User, Long>();
    for ( User user : users ) {
      profits.put( user, 0L );
    }
    
    int r = 0;
    Row[] rows;
    Row row;
    int c = 0;
    Cell cell;
    
    
    // Dump some game metadata
    row = sheet.createRow( r++ );
    cell = row.createCell( 0 );
    cell.setCellValue( "Name of Game" );
    cell = row.createCell( 2 );
    cell.setCellValue( game.getName() );
    
    row = sheet.createRow( r++ );
    cell = row.createCell( 0 );
    cell.setCellValue( "Number of players" );
    cell = row.createCell( 2 );
    cell.setCellValue( userPlants.size() );
    
    // Empty row
    row = sheet.createRow( r++ );
    
    // Dump plant setup
    int q = 0;
    rows = new Row[ 4 ];
    for ( int i = 0; i < 4; i++ ) {
      rows[ i ] = sheet.createRow( r++ );
    }
    for ( Map.Entry<PlantType, List<Plant>> e : plants.entrySet() ) {
      PlantType t = e.getKey();
      List<Plant> l = e.getValue();
      Plant a = l.get( 0 );
      rows[ 0 ].createCell( q + 0 ).setCellValue( t.getName() );
      rows[ 0 ].createCell( q + 1 ).setCellValue( "Count" );
      rows[ 0 ].createCell( q + 2 ).setCellValue( l.size() );
      rows[ 1 ].createCell( q + 0 );
      if ( t.isVariableOutput() ) {
        rows[ 1 ].createCell( q + 1 ).setCellValue( "Expected Output" );
        rows[ 1 ].createCell( q + 2 ).setCellValue( a.getExpectedOutput() );
        rows[ 2 ].createCell( q + 1 ).setCellValue( "Output Std.Dev." );
        rows[ 2 ].createCell( q + 2 ).setCellValue( a.getOutputStandardDeviation() );
      } else {
        rows[ 1 ].createCell( q + 1 ).setCellValue( "Maximum Output" );
        rows[ 1 ].createCell( q + 2 ).setCellValue( a.getExpectedOutput() );
        rows[ 2 ].createCell( q + 1 ).setCellValue( "Marginal Cost" );
        rows[ 2 ].createCell( q + 2 ).setCellValue( a.getMarginalCostPrice() );
        rows[ 3 ].createCell( q + 1 ).setCellValue( "Ramping Cost" );
        rows[ 3 ].createCell( q + 2 ).setCellValue( a.getRampUpCost() );
      }
      q += 4;
    }
    
    // Empty row
    row = sheet.createRow( r++ );
    int last = game.getCurrentRound();
    if ( last > 0 && game.getCurrentPeriod() == 0 ) {
      last--;
    }
    
    
    // ---- Headers ----
    
    row = sheet.createRow( r++ );
    c = 0;
    int roundCol = c;
    row.createCell( c++ ).setCellValue( "Round" );
    
    // Empty column
    row.createCell( c++ );
    
    int playerCol = c;
    row.createCell( c++ ).setCellValue( "Player" );
    
    // Empty column
    row.createCell( c++ );
    
    int plantCol = c;
    row.createCell( c++ ).setCellValue( "Plant" );
    
    // Empty column
    row.createCell( c++ );
    
    HashMap<Integer,Integer> marketCols = new HashMap<Integer, Integer>();
    HashMap<Integer,Integer> clearCols = new HashMap<Integer, Integer>();
    
    for ( int period : game.getHedgePeriods() ) {
      String pname = game.getHedgePeriodName( period );
      
      if ( game.isInitialPeriod( period ) || game.isFinalPeriod( period ) ) {
        continue;
      }
      marketCols.put( period, c );
      row.createCell( c++ ).setCellValue( "Bid Q-" + pname );
      row.createCell( c++ ).setCellValue( "Bid P-" + pname );
      row.createCell( c++ ).setCellValue( "Cleared Q-" + pname );
      row.createCell( c++ ).setCellValue( "Profit-" + pname );
      clearCols.put( period, c );
      row.createCell( c++ ).setCellValue( "Cleared P-" + pname );
      row.createCell( c++ ).setCellValue( "Demand-" + pname );
      // Empty column
      row.createCell( c++ );
    }
    int actualCol = c;
    row.createCell( c++ ).setCellValue( "Actual Output" );
    
    
    // ---- Data ----
    
    for ( int round = 0; round <= last; round++ ) {
      Map<Integer, Map<String, Market>> markets = Market.getMarkets( game, round, game.getNumPeriods() );
      
      row = sheet.createRow( r++ );
      row.createCell( roundCol ).setCellValue( round + 1 );
      
      for ( int period : game.getHedgePeriods() ) {
        if ( game.isInitialPeriod( period ) || game.isFinalPeriod( period ) ) {
          continue;
        }
        int abs = Math.abs( period );
        Map<String, Market> sub = markets.get( abs );
        Market market = sub == null ? null : sub.get( "m1" );
        String val = "N/A";
        String val2 = "N/A";
        if ( market != null && market.getLoad() * period >= 0 ) {
          val = "" + (int) Math.round( market.getPrice() );
          val2 = "" + market.getLoad();
        }
        row.createCell( clearCols.get( period ) ).setCellValue( val );
        row.createCell( clearCols.get( period ) + 1 ).setCellValue( val2 );
      }
      
      for ( User user : users ) {
        Map<String, Map<String, Hedge>> hedges = Hedge.getHedges( game, user, round, game.getNumPeriods() );
        
        for ( Plant plant : userPlants.get( user ) ) {
          row = sheet.createRow( r++ );
          
          row.createCell( roundCol ).setCellValue( round + 1 );
          
          row.createCell( playerCol ).setCellValue( user.getUserId() );
          
          row.createCell( plantCol ).setCellValue( plant.getName() );
          
          for ( int period : game.getHedgePeriods() ) {
            if ( game.isInitialPeriod( period ) || game.isFinalPeriod( period ) ) {
              continue;
            }
            String k = round + ":" + period;
            Map<String, Hedge> sub = hedges.get( k );
            Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
            
            c = marketCols.get( period );
            row.createCell( c++ ).setCellValue( hedge == null ? "N/A" : ""+hedge.getBidQuantity() );
            row.createCell( c++ ).setCellValue( hedge == null || hedge.getFixed() ? "N/A" : ""+hedge.getBidPrice() );
            row.createCell( c++ ).setCellValue( hedge == null ? "N/A" : ""+hedge.getClearingQuantity() );
            
            int profit = 0;
            if ( hedge != null ) {
              int h = -1 * hedge.getClearingPrice() * hedge.getClearingQuantity();
              int cm = (int) Math.round( hedge.getClearingQuantity() * plant.getMarginalCostPrice() );
              int cr = 0;
              if ( game.isPenultimatePeriod( period ) ) {
                cr = -1 * (int) Math.round( Math.abs( hedge.getClearingQuantity() * plant.getRampUpCost() ) );
              }
              profit = h + cm + cr;
            }
            row.createCell( c++ ).setCellValue( profit );
            profits.put( user, profits.get( user ) + profit );
          }
          
          // Actual output
          if ( plant.getType().isVariableOutput() ) {
            int sum = 0;
            for ( int period : game.getHedgePeriods() ) {
              String k = round + ":" + period ;
              Map<String, Hedge> sub = hedges.get( k );
              Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
              sum += hedge == null ? 0 : hedge.getClearingQuantity();
            }
            row.createCell( actualCol ).setCellValue( - sum );
          } else {
            row.createCell( actualCol ).setCellValue( plant.getExpectedOutput() );
          }
          
        }
        
      }
      
    }
    
    // Empty row
    row = sheet.createRow( r++ );
    
    // Dump user profits, order descending
    row = sheet.createRow( r++ );
    Collections.sort( users, new Comparator<User>() {
      @Override
      public int compare( User u1, User u2 ) {
        long diff = profits.get( u2 ) - profits.get( u1 );
        return diff < 0 ? -1 : diff > 0 ? 1 : 0;
      }
    });
    
    row.createCell( 0 ).setCellValue( "Player" );
    row.createCell( 1 ).setCellValue( "Total Profit" );
    for ( User user : users ) {
      row = sheet.createRow( r++ );
      row.createCell( 0 ).setCellValue( user.getUsername() + " (#" + user.getUserId() + ")" );
      row.createCell( 1 ).setCellValue( profits.get(  user ) );
    }
  }
  
  /*
  private void fillDataAdminOld ()
  {
    Map<User, List<Plant>> userPlants = game.getUserPlants();
    int rowCount = 2;
    int cellCount = 0;
    
    // dump plant setup
    
    Map<PlantType, List<Plant>> plants = Plant.getPlantsByGame( game );
    
    Row row;
    Cell cell;
    
    for ( Map.Entry<PlantType, List<Plant>> entry : plants.entrySet() ) {
      row = sheet.createRow(  rowCount++  );
      cellCount = 0;
      PlantType type = entry.getKey();
      Plant plant = entry.getValue().get( 0 );
      cell = row.createCell( cellCount++ );
      cell.setCellValue( type.getName() );
      for ( Parameter param : plant.getParameters( false ) ) {
        row = sheet.createRow(  rowCount++  );
        cellCount = 0;
        row.createCell( cellCount++ );
        cell = row.createCell( cellCount++ );
        cell.setCellValue( param.getName() );
        cell = row.createCell( cellCount++ );
        cell.setCellValue( Integer.parseInt( param.getValue() ) );
      }
    }
    sheet.createRow( rowCount++ );
    cellCount = 0;
    
    // For each round, for each period ...
    for ( int r = 0; r <= game.getCurrentRound(); r++ ) {
      
      // empty row
      sheet.createRow( rowCount++ );
      cellCount = 0;
      
      int p = game.getNumPeriods();
      row = sheet.createRow( rowCount++ );
      cell = row.createCell( cellCount++ );
      cell.setCellValue( "Round " + (r+1) );
      cell = row.createCell( cellCount++ );
      
      // Period headers
      for ( int t = 1; t <= p; t++ ) {
        cell = row.createCell( cellCount++ );
        cell.setCellValue( "T" + t );
      }
      
      // Empty row
      row = sheet.createRow( rowCount++ );
      cellCount = 0;
      
      // Market info
      
      Map<Integer, Map<String, Market>> markets = Market.getMarkets( game, r, p );
      
      
      row = sheet.createRow( rowCount++ );
      cellCount = 0;
      cell = row.createCell( cellCount++ );
      cell.setCellValue( "Forecast Demand" );
      cell = row.createCell( cellCount++ );
      cell = row.createCell( cellCount++ );
      for ( int t = 1; t <= p; t++ ) {
        Map<String, Market> sub = markets.get( t );
        Market market = sub == null ? null : sub.get( "m1" );
        if ( market != null ) {
          cell.setCellValue( market.getForecast() );
        }
        cell = row.createCell( cellCount++ );
      }
      
      row = sheet.createRow( rowCount++ );
      cellCount = 0;
      cell = row.createCell( cellCount++ );
      cell.setCellValue( "Demand StdDev" );
      cell = row.createCell( cellCount++ );
      cell = row.createCell( cellCount++ );
      for ( int t = 1; t <= p; t++ ) {
        Map<String, Market> sub = markets.get( t );
        Market market = sub == null ? null : sub.get( "m1" );
        if ( market != null ) {
          cell.setCellValue( market.getSigma() );
        }
        cell = row.createCell( cellCount++ );
      }
      
      row = sheet.createRow( rowCount++ );
      cellCount = 0;
      cell = row.createCell( cellCount++ );
      cell.setCellValue( "Actual Demand" );
      cell = row.createCell( cellCount++ );
      cell = row.createCell( cellCount++ );
      for ( int t = 1; t <= p; t++ ) {
        Map<String, Market> sub = markets.get( t );
        Market market = sub == null ? null : sub.get( "m1" );
        if ( market != null ) {
          cell.setCellValue( market.getLoad() );
        }
        cell = row.createCell( cellCount++ );
      }
      
      row = sheet.createRow( rowCount++ );
      cellCount = 0;
      cell = row.createCell( cellCount++ );
      cell.setCellValue( "Cleared Price" );
      cell = row.createCell( cellCount++ );
      cell = row.createCell( cellCount++ );
      for ( int t = 1; t <= p; t++ ) {
        Map<String, Market> sub = markets.get( t );
        Market market = sub == null ? null : sub.get( "m1" );
        if ( market != null ) {
          cell.setCellValue( market.getPrice() );
        }
        cell = row.createCell( cellCount++ );
      }
      
      
      // Empty row
      row = sheet.createRow( rowCount++ );
      cellCount = 0;
      
      // Empty row
      row = sheet.createRow( rowCount++ );
      cellCount = 0;
      
      // For each user, for each plant ...
      for ( Map.Entry<User, List<Plant>> entry : userPlants.entrySet() ) {
        User user = entry.getKey();
        
        cell = row.createCell( cellCount++ );
        cell.setCellValue(user.getUsername() + " (#" + user.getUserId() + ")" );
        row = sheet.createRow( rowCount++ );
        cellCount = 0;
        
        row = sheet.createRow( rowCount++ );
        cellCount = 0;
        cell = row.createCell( cellCount++ );
        cell = row.createCell( cellCount++ );
        cell.setCellValue( "Actual Output" );
        
        cell = row.createCell( cellCount++ );
        for ( Plant plant : entry.getValue() ) {
          row = sheet.createRow( rowCount++ );
          cellCount = 0;
          cell = row.createCell( cellCount++ );
          cell.setCellValue( plant.getName() );
          cell = row.createCell( cellCount++ );
          
          if ( plant.getType().isVariableOutput() ) {
            Map<String, Map<String, Hedge>> hedges = Hedge.getHedges( game, user, r, p );
            int sum = 0;
            for ( int t = 1; t <= p; t++ ) {
              String k = r + ":" + t ;
              Map<String, Hedge> sub = hedges.get( k );
              Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
              sum += hedge == null ? 0 : hedge.getBidQuantity();
            }
            cell.setCellValue( - sum );
          } else {
            cell.setCellValue( plant.getExpectedOutput() );
          }
        }
        
        row = sheet.createRow( rowCount++ );
        cellCount = 0;
        
        for ( Plant plant : entry.getValue() ) {
          Map<String, Map<String, Hedge>> hedges = Hedge.getHedges( game, user, r, p );
          
          row = sheet.createRow( rowCount++ );
          cellCount = 0;
          cell = row.createCell( cellCount++ );
          cell.setCellValue( plant.getName() );
          cell = row.createCell( cellCount++ );
          cell.setCellValue( "Bid Q" );
          for ( int t = 1; t <= p; t++ ) {
            String k = r + ":" + t ;
            Map<String, Hedge> sub = hedges.get( k );
            Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
            if ( hedge == null ) {
              cell = row.createCell( cellCount++ );
              cell.setCellValue( "" );
            } else {
              cell = row.createCell( cellCount++ );
              cell.setCellValue( hedge.getBidQuantity() );
            }
          }
          
          row = sheet.createRow( rowCount++ );
          cellCount = 0;
          cell = row.createCell( cellCount++ );
          cell = row.createCell( cellCount++ );
          cell.setCellValue( "Bid P" );
          for ( int t = 1; t <= p; t++ ) {
            String k = r + ":" + t ;
            Map<String, Hedge> sub = hedges.get( k );
            Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
            if ( hedge == null ) {
              cell = row.createCell( cellCount++ );
              cell.setCellValue( "" );
            } else {
              cell = row.createCell( cellCount++ );
              if ( hedge.getSkip() ) {
                cell.setCellValue( "N/A" );
              } else { 
                cell.setCellValue( hedge.getBidPrice() );
              }
            }
          }
          
          row = sheet.createRow( rowCount++ );
          cellCount = 0;
          cell = row.createCell( cellCount++ );
          cell = row.createCell( cellCount++ );
          cell.setCellValue( "Cleared Q" );
          for ( int t = 1; t <= p; t++ ) {
            String k = r + ":" + t ;
            Map<String, Hedge> sub = hedges.get( k );
            Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
            if ( hedge == null ) {
              cell = row.createCell( cellCount++ );
              cell.setCellValue( "" );
            } else {
              cell = row.createCell( cellCount++ );
              cell.setCellValue( hedge.getClearingQuantity() );
            }
          }
        }
        
        // empty row
        sheet.createRow(  rowCount++  );
        
        // dump mtm stuff
        for ( Plant plant : entry.getValue() ) {
          Map<String, Map<String, Hedge>> hedges = Hedge.getHedges( game, user, r, p );
          
          double[] profit = new double[p];
          
          row = sheet.createRow( rowCount++ );
          cellCount = 0;
          cell = row.createCell( cellCount++ );
          cell.setCellValue( plant.getName() );
          cell = row.createCell( cellCount++ );
          cell.setCellValue( "Hedges" );
          for ( int t = 1; t <= p; t++ ) {
            String k = r + ":" + t ;
            Map<String, Hedge> sub = hedges.get( k );
            Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
            if ( hedge == null ) {
              cell = row.createCell( cellCount++ );
              cell.setCellValue( "" );
            } else {
              profit[t-1] = -1 * hedge.getClearingQuantity() * hedge.getClearingPrice();
              cell = row.createCell( cellCount++ );
              cell.setCellValue( profit[t-1] );
            }
          }
          
          if ( ! plant.getType().isVariableOutput() ) {
            
            
            row = sheet.createRow( rowCount++ );
            cellCount = 0;
            cell = row.createCell( cellCount++ );
            cell = row.createCell( cellCount++ );
            cell.setCellValue( "Marginal Cost" );
            for ( int t = 1; t <= p; t++ ) {
              String k = r + ":" + t ;
              Map<String, Hedge> sub = hedges.get( k );
              Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
              if ( hedge == null ) {
                cell = row.createCell( cellCount++ );
                cell.setCellValue( "" );
              } else {
                double c = hedge.getClearingQuantity() * plant.getMarginalCostPrice();
                cell = row.createCell( cellCount++ );
                cell.setCellValue( c );
                profit[t-1] += c;
              }
            }
            
            row = sheet.createRow( rowCount++ );
            cellCount = 0;
            cell = row.createCell( cellCount++ );
            cell = row.createCell( cellCount++ );
            cell.setCellValue( "Ramping Cost" );
            for ( int t = 1; t <= p; t++ ) {
              String k = r + ":" + t ;
              Map<String, Hedge> sub = hedges.get( k );
              Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
              if ( hedge == null ) {
                cell = row.createCell( cellCount++ );
                cell.setCellValue( "" );
              } else if ( game.isPenultimatePeriod( t ) ) {
                double c = -1 * Math.abs( hedge.getClearingQuantity() * plant.getRampUpCost() );
                cell = row.createCell( cellCount++ );
                cell.setCellValue( c );
                profit[t-1] += c;
              } else {
                cell = row.createCell( cellCount++ );
                cell.setCellValue( 0 );
              }
            }
            
            row = sheet.createRow( rowCount++ );
            cellCount = 0;
            cell = row.createCell( cellCount++ );
            cell = row.createCell( cellCount++ );
            cell.setCellValue( "Profit" );
            for ( int t = 1; t <= p; t++ ) {
              String k = r + ":" + t ;
              Map<String, Hedge> sub = hedges.get( k );
              Hedge hedge = sub == null ? null : sub.get( "p" + plant.getPlantId() + "_m1" );
              cell = row.createCell( cellCount++ );
              cell.setCellValue( profit[t-1] );
            }
          }
        }
        
        // empty row
        sheet.createRow(  rowCount++  );
        
      }
      
      
      
      
      // empty row
      sheet.createRow(  rowCount++  );
    }
  }
  */
  
   public void serveWorkbook ()
  {
    if (workbook == null || sheet == null) {
      return;
    }

    File tempFile;
    try {
      tempFile = File.createTempFile(temp, ".tmp");
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

    writeToFile(tempFile);
    
    FacesContext facesContext = FacesContext.getCurrentInstance();
    HttpServletResponse response = (HttpServletResponse)
        facesContext.getExternalContext().getResponse();
    response.reset();
    response.setHeader("Content-Disposition", "attachment;filename=" + name);
    response.setContentLength((int) tempFile.length());
    response.setContentType("application/vnd.ms-excel");

    ServletOutputStream out = null;
    try {
      FileInputStream input = new FileInputStream(tempFile);
      byte[] buffer = new byte[1024];
      out = response.getOutputStream();
      while (input.read(buffer) != -1) {
        out.write(buffer);
        out.flush();
      }
      FacesContext.getCurrentInstance().getResponseComplete();
    }
    catch (IOException err) {
      err.printStackTrace();
    }
    finally {
      try {
        if (out != null) {
          out.close();
        }
      }
      catch (IOException err) {
        err.printStackTrace();
      }
      facesContext.responseComplete();
    }
  }

  public void writeToFile (File file)
  {
    try {
      FileOutputStream out = new FileOutputStream(file);
      workbook.write(out);
      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}