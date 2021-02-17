function updateHedging( round, period ) {
  var pre = "tabs:form1:T" + period;
  var bid = "tabs:form7:R" + round + "_T" + period;
  
  var totals = {};
  
  for ( var i = 0; i < plantKeys.length; i++ ) {
    var plant = plantKeys[ i ];
    for ( var j = 0; j < metricKeys.length; j++ ) {
      var metric = metricKeys[ j ];
      var type = plant + '_' + metric;
      totals[ metric ] = totals[ metric ] || 0;
      for ( var k = 0; k < typeKeys.length; k++ ) {
        if ( typeKeys[ k ] === type ) {
          var p = getValue( pre, '_' + type + '_hedp' );
          var q = getValue( pre, '_' + type + '_hedq' );
          setValue( bid, '_' + type + '_hedp_b_b', p );
          setValue( bid, '_' + type + '_hedq_b_b', q );
          totals[ metric ] += q; // getValue( pre, '_' + type + '_hed' );
        }
      }
    }
  }
  
  /*
  updateCumm( period );
  updateNet( period );
  updateMtM( period );
  */
}
