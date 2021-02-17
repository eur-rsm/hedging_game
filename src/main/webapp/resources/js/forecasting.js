function updateForecast( period ) {
  var pre = "tabs:form1:T" + period;
  
  if ( plantKeys.length > 1 ) {
    
    var totals = {};
    
    for ( var i = 0; i < plantKeys.length; i++ ) {
      var plant = plantKeys[ i ];
      for ( var j = 0; j < metricKeys.length; j++ ) {
        var metric = metricKeys[ j ];
        var type = plant + '_' + metric;
        totals[ metric ] = totals[ metric ] || 0;
        for ( var k = 0; k < typeKeys.length; k++ ) {
          if ( typeKeys[ k ] === type ) {
            totals[ metric ] += getValue( pre, '_' + type + '_for' );
          }
        }
      }
    }
    
    for ( var j = 0; j < metricKeys.length; j++ ) {
      var metric = metricKeys[ j ];
      setValue( pre, '_tt_' + metric + '_for', totals[ metric ] );
    }
    
  }
  
  /*
  updateCumm( period );
  updateNet( period );
  updateMtM( period );
  */
}
