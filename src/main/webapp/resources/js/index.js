

function getValue (pre, key) {
  var id = pre + key;
  var source = document.getElementById( id );
  if ( ! source ) {
    console.log( 'getValue(): no element with id "' + id + '"!' );
    val = 0;
  } else {
    var val = parseInt( source.value.replace(/,/g, "") );
    if ( isNaN( val ) ) {
      console.log( 'getValue(): value for "' + id + '" not numerical! (' + source.value + ')' );
      val = 0;
    }
  }
  return val;
}

function setValue( pre, key, value ) {
  var id = pre + key;
  var target = document.getElementById( pre + key );
  if ( ! target ) {
    console.log( 'setValue(): no element with id "' + id + '"!' );
  } else {
    val = parseInt( value );
    if ( isNaN( val ) ) {
      console.log( 'setValue(): value for "' + id + '" not numerical! (' + value + ')' );
      val = 0;
    } else {
      var neg = val < 0 ? '-' : '';
      value = '' + Math.abs( val );
      var s = value.length % 3;
      if( s === 0 ) {
        s = 3;
      }
      val = value.substr( 0, s );
      value = value.substr( s );
      while ( value.length ) {
        val += ',' + value.substr( 0, 3 );
        value = value.substr( 3 );
      }
      /*
      s = val.indexOf( '.' );
      if ( s >= 0 ) {
        while ( val.length - s < 2 ) {
          val += '0';
        }
      }
      */
    }
    
    target.value = neg + val;
    
    //console.log( 'set ' + target.id + ' = ' + neg + val );
  }
}


function handleMessageRefresh(data) {
  if (data == 'all') {
    updatePeriod();
    updateAll();
    updateSaveButton();
  }
  else if (data == 'events') {
    updateEvents();
  }
}

function handleMessageNotify(message) {
  message.severity = 'info';
  PF('growl').show([message]);
  /*if ( message.detail.startsWith( "Started" ) || message.detail.endsWith( "ended"  ) ) {
    setTimeout( function() { window.location.reload(); }, 1000 );
  }*/
}

function savePeriod() {
  document.getElementById('tabs:form1:hiddenSaveButton').click();
}

$(document).ready(function () {
  
  var tmp0 = {};
  var tmp1 = {};
  for ( var i = 0; i < typeKeys.length; i++ ) {
    var split = typeKeys[ i ].split( '_' );
    tmp0[ split[ 0 ] ] = true;
    tmp1[ split[ 1 ] ] = true;
  }
  plantKeys = [];
  metricKeys = [];
  for ( var k in tmp0 ) {
    plantKeys.push( k );
  }
  for ( var k in tmp1 ) {
    metricKeys.push( k );
  }
  
});
