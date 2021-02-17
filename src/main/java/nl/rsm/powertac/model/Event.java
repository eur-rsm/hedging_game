package nl.rsm.powertac.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.SortedSet;

import nl.rsm.powertac.Database;
import nl.rsm.powertac.endpoints.NotifyResource;
import nl.rsm.powertac.endpoints.RefreshResource;


public class Event
implements Comparable<Event> {
  
  private int eventId;
  private int gameId;
  private int round;
  private int period;
  private int delay;
  private String title;
  private String content;
  private boolean show;

  public Event ( int round, int period, int delay, String title, String content ) {
    this( 0, round, period, delay, title, content );
  }
  
  public Event ( int gameId, int round, int period, int delay, String title, String content)
  {
    this.eventId = -1;
    this.gameId = gameId;
    this.round = round;
    this.period = period;
    this.delay = delay;
    this.title = title;
    this.content = content;
    this.show = false;
  }

  public Event( ResultSet rs ) throws SQLException {
    this.eventId = rs.getInt( "eventId" );
    this.gameId = rs.getInt( "gameId" );
    this.round = rs.getInt( "round" );
    this.period = rs.getInt( "period" );
    this.delay = rs.getInt( "delay" );
    this.title = rs.getString( "title" );
    this.content = rs.getString( "content" );
    this.show = false;
  }
  
  public int getEventId() {
    return eventId;
  }
  
  @Override
  public int hashCode() {
    return new Integer( eventId ).hashCode();
  }
  
  @Override
  public boolean equals( Object other ) {
    return other instanceof Event && ((Event) other).eventId == eventId;
  }
  
  @Override
  public int compareTo( Event other ) {
    if ( this.round < other.round ) {
      return -1;
    }
    if ( this.round > other.round ) {
      return 1;
    }
    if ( this.period < other.period ) {
      return -1;
    }
    if ( this.period > other.period ) {
      return 1;
    }
    if ( this.delay < other.delay ) {
      return -1;
    }
    if ( this.delay > other.delay ) {
      return 1;
    }
    if ( this.eventId < other.eventId ) {
      return -1;
    }
    if ( this.eventId > other.eventId ) {
      return 1;
    }
    return 0;
  }
  
  public void setEventId( int eventId ) {
    this.eventId = eventId;
  }

  public int getGameId() {
    return gameId;
  }
  
  public void setGameId( int gameId ) {
    this.gameId = gameId;
  }
  
  public int getRound() {
    return round;
  }

  public void setRound( int round ) {
    this.round = round;
  }
  
  public int getPeriod () {
    return period;
  }
  
  public void setPeriod( int period ) {
    this.period = period;
  }
  
  public int getDelay () {
    return delay;
  }
  
  public void setDelay( int delay ) {
    this.delay = delay;
  }

  public String getTitle() {
    return title;
  }
  
  public void setTitle( String title ) {
    this.title = title;
  }
  
  public String getContent ()
  {
    return content;
  }

  public boolean isShow ()
  {
    return show;
  }

  public void setShow (boolean show)
  {
    if (show) {
      RefreshResource.sendMessage("events");
      NotifyResource.sendMessage("*", title, content);
    }

    this.show = show;
  }
  
  public void save() {
    Database db = new Database();
    try {
      if ( eventId < 0 ) {
        db.insertEvent( this );
      } else {
        db.updateEvent( this );
      }
    } catch ( SQLException x ) {
      x.printStackTrace();
    } finally {
      db.close();
    }
  }

  public static SortedSet<Event> getEvents( Game game ) {
    Database db = new Database();
    SortedSet<Event> events = null;
    try {
      events = db.getEvents( game );
    } catch ( SQLException x ) {
      x.printStackTrace();
    } finally {
      db.close();
    }
    return events;
  }
}