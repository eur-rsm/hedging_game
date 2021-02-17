function updateMtM( period ) {
  var prevPeriod = period - 1;
  var prev = "tabs:form5:T" + prevPeriod;
  var pre = "tabs:form5:T" + period;
  var net = "tabs:form2:T" + period;
  var mar = "tabs:form1:T" + period;
  var hed = "tabs:form1:T" + period;
  var hedneg = "tabs:form1:T-" + period;
  var marginal = "tabs:form1:margcost_";
  var rampup = "tabs:form1:rupcost_";
  
  var hedges_total = 0;
  var costs_total = 0;
  var profits_total = 0;
  var assets_total = 0;
  var total_total = 0;
  
  for ( var i = 0; i < plantKeys.length; i++ ) {
    var plant = plantKeys[ i ];
    var hedges_sum = 0;
    var costs_sum = 0;
    var marg_costs_sum = 0;
    var rup_costs_sum = 0;
    var profits_sum = 0;
    var assets_sum = 0;
    var total_sum = 0;
    
    var margcost = getValue( marginal, plant );
    var rupcost = getValue( rampup, plant );
    
    for ( var j = 0; j < metricKeys.length; j++ ) {
      var metric = metricKeys[ j ];
      var type = plant + '_' + metric;
      for ( var k = 0; k < typeKeys.length; k++ ) {
        if ( typeKeys[ k ] === type ) {
          var price = getValue( mar, '_' + metric );
          var quant = getValue( hed, '_' + type + '_hedq' );
          if ( period === 2 ) {
            quant += getValue( hedneg, '_' + type + '_hedq' );
          }
          var hedges = -1 * quant * price;
          var marg_costs = quant * margcost;
          var rup_costs = 0;
          if ( period > 1 ) {
            rup_costs = - Math.abs( rupcost * quant );
          }
          var assets = getValue( net, '_' + type + '_net' ) * price;
          
          if ( metric === 'm1' ) {
            hedges_sum += hedges;
            marg_costs_sum += marg_costs;
            rup_costs_sum += rup_costs;
            assets_sum += assets;
          }
        }
      }
    }
    
    costs_sum = marg_costs_sum + rup_costs_sum;
    /*
    if ( period > 0 ) {
      //hedges_sum += getValue( prev, '_' + plant + '_h_mtm' );
      setValue( pre, '_' + plant + '_h_mtm', hedges_sum );
      if ( margcost > 0 ) {
        setValue( pre, '_' + plant + '_mc_mtm', marg_costs_sum );
        setValue( pre, '_' + plant + '_rc_mtm', rup_costs_sum );
        setValue( pre, '_' + plant + '_p_mtm', hedges_sum + costs_sum );
      }
      //setValue( pre, '_' + plant + '_a_mtm', assets_sum );
      //setValue( pre, '_' + plant + '_t_mtm', hedges_sum + costs_sum + assets_sum );
    }
    */
    hedges_total += hedges_sum;
    costs_total += costs_sum;
    profits_total += hedges_sum + costs_sum;
    assets_total += assets_sum;
    total_total += hedges_sum + costs_sum + assets_sum;
  }
  
  if ( plantKeys.length > 1 ) {
    setValue( pre, '_tt_h_mtm', hedges_total );
    setValue( pre, '_tt_c_mtm', costs_total );
    setValue( pre, '_tt_p_mtm', profits_total );
    setValue( pre, '_tt_a_mtm', assets_total );
    setValue( pre, '_tt_t_mtm', total_total );
    
    if ( period == 1 ) {
      setValue( pre, "_tt_tp_mtm", profits_total );
      setValue( pre, "_tt_tt_mtm", total_total );
    } else if ( period > 1 ) {
      setValue( pre, "_tt_tp_mtm", profits_total + getValue( prev, '_tt_tp_mtm' ) );
      setValue( pre, "_tt_tt_mtm", total_total + getValue( prev, '_tt_tt_mtm' ) );
    }
    
  }
}

function updateAllMtM() {
  var currentPeriod = document.getElementById("currentPeriod").value;
  for ( var period = 0; period == 0 || period < currentPeriod; period++ ) {
    updateMtM(period);
  }
}

$(document).ready(function () {
  updateAllMtM();
});