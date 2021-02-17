package nl.rsm.powertac.util;

import java.util.Map;

import nl.rsm.powertac.model.Forecast;
import nl.rsm.powertac.model.Game;
import nl.rsm.powertac.model.Market;
import nl.rsm.powertac.model.Metric;
import nl.rsm.powertac.model.Plant;
import nl.rsm.powertac.model.PlantType;
import nl.rsm.powertac.model.Position;
import nl.rsm.powertac.model.User;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;


@FacesValidator(value = "inputValidator")
public class InputValidator implements Validator
{
  
  @Override
  public void validate (FacesContext context, UIComponent component,
                        Object value)
      throws ValidatorException
  {
    User user = User.getCurrentUser();
    if (user == null ) {
      return;
    }
    Game game = Game.getActiveGame();
    long input = (Long) value;
    String[] split = component.getId().split("_");
    int round = game.getCurrentRound();
    int period = Integer.parseInt( split[0].substring( split[0].lastIndexOf( "T" ) + 1 ) );
    int abs = Math.abs( period );
    Plant plant = Plant.getPlantById( Integer.parseInt( split[1].substring(1) ) );
    Metric metric = Metric.getMetricById( Integer.parseInt( split[2].substring(1) ) );
    Position position = Position.getPosition( game, user, plant, metric, round, abs - 1 );
    
    Map<String, Market> markets = Market.getMarkets( game, round, abs ).get( abs );
    Market market = markets.get( "m" + metric.getMetricId() );
    
    check( game, plant, metric, position, market, input, split[3], period, abs );
    
    /* TODO
    // During the last period, check the total (triggered on a single input)
    if (component.getId().equals("T4_dg_hed")) {
      checkFinal();
    }
    */
  }

  private void check( Game game, Plant plant, Metric metric, Position position
      , Market market, long input, String postfix, int period, int absperiod )
  {
    PlantType type = PlantType.getPlantTypeById( plant.getTypeId() );
    String which = type.getName() + " " + metric.getName();
    
    String table = "<unknown>";
    boolean forecast = false;
    boolean power = metric.getSpreadName() == null; // TODO this is ugly... detect Power metric by NULL spreadName
    boolean quantity = true;
    if ( postfix.equals( "for" ) ) {
      table = "Forecasting";
      forecast = true;
    } else if ( postfix.equals( "hedp" ) ) {
      table = "Hedging Price";
      quantity = false;
    } else if ( postfix.equals( "hedq" ) ) {
      table = "Hedging Quantity";
    }
    
    String key = "p" + plant.getPlantId() + "_m" + metric.getMetricId();
    Forecast f0 = Forecast.getForecastsBootstrap( game.getGameId() ).get( key );
    
    int multiplier = metric.getMultiplier();
    int netpos = position == null ? plant.getRealOutput() : position.getNetValue();
    
    if ( plant == null || metric == null ) {
      throwException( "Error!", "", "Unknown input type" );
    } else if ( Constants.LIMIT_FORECASTING_ONOFF && forecast && power && ! (input == 0 || input == f0.getValue() ) ) {
      throwException( "Error!" , which, "Plants can only be on/off!" );
    } else if ( forecast && multiplier > 0 && input < 0 ) {
      throwException( "Error!" , which, "May not be negative!" );
    } else if ( forecast && multiplier < 0 && input > 0 ) {
      throwException( "Error!" , which, "May not be positive!" );
    } /*else if ( ! forecast && ! quantity && input < 0 ) {
      throwException( table, which, "Prices must be positive!" );
    } else if ( Constants.LIMIT_HEDGING_SIGN && ! forecast && quantity && multiplier < 0 && input < 0 ) {
      throwException( table, which, "May not be negative!" );
    } else if ( Constants.LIMIT_HEDGING_SIGN && ! forecast && quantity && multiplier > 0 && input > 0 ) { 
      throwException( table, which, "May not be positive!" );
    }
    else if ( Constants.LIMIT_HEDGING_POSITION && quantity && multiplier > 0 && input < -netpos ) {
      throwException( table, which,
          "your hedge can't exceed your net position (" + (-netpos) + ")!" );
    } else if ( Constants.LIMIT_HEDGING_POSITION && quantity && multiplier < 0 && input > -netpos ) {
      throwException( table, which,
          "your hedge can't exceed your net position (" + (-netpos) + ")!" );
    }*/
    if ( Constants.LIMIT_HEDGING_PRICE && !quantity && input < game.getMinPrice() ) {
      throwException( table, which,
          "prices may not be below " + game.getMinPrice() );
    } else if ( Constants.LIMIT_HEDGING_PRICE && !quantity && input > game.getMaxPrice() ) {
      throwException( table, which,
          "prices may not be above " + game.getMaxPrice() );
    }
    if (quantity && input < 0 ) {
      throwException( table, which, "Quantities must be non-negative");
    }
    if (quantity && ! game.isPenultimatePeriod( absperiod ) && ! market.isNegativeMarket()) {
      input = -input;
    }
    else if (quantity && game.isPenultimatePeriod( absperiod ) && period > 0) {
      input = -input;
    }
    /*
    if ( Constants.LIMIT_HEDGING_SIGN && quantity && ! game.isPenultimatePeriod( absperiod )
        && market.isNegativeMarket() && input < 0 ) {
      throwException( table, which, "negative market, your hedge must be positive" );
    }
    else if ( Constants.LIMIT_HEDGING_SIGN && quantity && ! game.isPenultimatePeriod( absperiod )
        && ! market.isNegativeMarket() && input > 0 ) {
      throwException( table, which, "positive market, your hedge must be negative" );
    }
    else if ( Constants.LIMIT_HEDGING_SIGN && quantity && game.isPenultimatePeriod( absperiod )
        && period > 0 && input > 0 ) {
      throwException( table, which, "positive market, your hedge must be negative" );
    }
    else if ( Constants.LIMIT_HEDGING_SIGN && quantity && game.isPenultimatePeriod( absperiod )
        && period < 0 && input < 0 ) {
      throwException( table, which, "negative market, your hedge must be positive" );
    }
    */
    if ( Constants.LIMIT_HEDGING_POSITION && quantity && input < 0 && input < -netpos ) {
      throwException( table, which,
          "your hedge can't exceed your net position" );
    }
    else if ( Constants.LIMIT_HEDGING_POSITION && quantity && input > 0 && input > netpos ) {
      throwException( table, which,
          "your hedge can't exceed your net position " + netpos );
    } else if ( ! plant.getType().isVariableOutput() ) {
      if ( ! game.isPenultimatePeriod() ) {
        if ( quantity && input < 0 && input < -netpos ) {
          throwException( table, which,
              "your hedge can't exceed your max output " + netpos );
        } else if ( quantity && input > 0 && input > netpos ) {
          throwException( table, which,
              "your hedge can't exceed your max output " + netpos );
        }
      } else {
        // Limit hedges such that Net position ends up between 0 and Max Output
        if ( quantity && (input + netpos) < 0 ) {
          throwException( table, which,
              "your hedge would cause your net position to fall below 0" );
        }
        int max = plant.getExpectedOutput();
        if ( quantity && (input + netpos) > max ) {
          throwException( table, which,
              "your hedge would cause your net position to rise above max output " + max );
        }
      }
    } else if ( ! game.isPenultimatePeriod() ) { // plant.isVariableOutput() == true
      int d = (int) Math.round( plant.getExpectedOutput() + 3D * plant.getOutputStandardDeviation() );
      if ( quantity && input < 0 && input < -d ) {
        throwException( table, which, "your hedge can't be less than " + (-d) );
      } else if ( quantity && input > 0 && input > d ) {
        throwException( table, which, "your hedge can't be more than " + d );
      }
    }
    
  }

  // TODO Checks against the DB values, but we're submitting here!!!!!!!!
  /* not used currently
  private void checkFinal ()
  {
    User user = User.getCurrentUser();
    int userId = user.getUserId();

    long netPow = 0;
    long netCoal = 0;
    long netGas = 0;

    for (Forecast forecast : Forecast.getForecasts(userId, T0).values()) {
      netPow += forecast.getaPower();
      netPow += forecast.getbPower();
      netPow += forecast.getcPower();
      netPow += forecast.getdPower();
      netCoal += forecast.getcCoal();
      netGas += forecast.getdGas();
    }

    for (Hedge hedge : Hedge.getHedges(userId, T4).values()) {
      netPow += hedge.getaPower();
      netPow += hedge.getbPower();
      netPow += hedge.getcPower();
      netPow += hedge.getdPower();
      netCoal += hedge.getcCoal();
      netGas += hedge.getdGas();
    }

    if (netPow != 0) {
      throwException("Error!", "", "T4 Net Position Power need to be zero");
    }
    if (netCoal != 0) {
      throwException("Error!", "", "T4 Net Position Coal need to be zero");
    }
    if (netGas != 0) {
      throwException("Error!", "", "T4 Net Position Gas need to be zero");
    }
  }
  */

  private void throwException (String table, String type, String content)
  {
    throw new ValidatorException(new FacesMessage(
        FacesMessage.SEVERITY_INFO, table +" "+ type, content));
  }
}