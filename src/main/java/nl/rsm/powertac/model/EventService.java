package nl.rsm.powertac.model;

import nl.rsm.powertac.Database;
import nl.rsm.powertac.endpoints.TimerResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Singleton;

import java.sql.SQLException;
import java.util.SortedSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@ManagedBean(eager = true)
@ApplicationScoped
@Singleton
public class EventService
{
  private ScheduledExecutorService scheduler;

  private static SortedSet<Event> events;
  private static int count = 362;
  private boolean inited = false;

  public EventService ()
  {
    // When using Jetty, annotations such as @PostConstruct are not being handled
    // properly at the moment... So call init() manually for the time being.
    init();
  }

  @PostConstruct
  public void init ()
  {
    if ( inited ) {
      return;
    }
    inited = true;
    
    fillEvents();

    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(doTimerTick(), 60, 1, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void destroy ()
  {
    scheduler.shutdownNow();
  }

  private void fillEvents ()
  {
    if ( events == null ) {
      try {
        Database db = new Database();
        events = db.getEvents( null );
      } catch (SQLException x ) {
        x.printStackTrace();
      }
    }
    
  }

  private Runnable doTimerTick ()
  {
    return new Runnable()
    {
      @Override
      public void run ()
      {
        if (count > 361) {
          return;
        }
        else if (count == 361) {
          TimerResource.sendMessage("");
          count++;
        }

        int period = Game.getActiveGame().getCurrentPeriod();
        for (Event event : events) {
          if (event.getPeriod() == period && event.getDelay() == count) {
            event.setShow(true);
          }
        }

        TimerResource.sendMessage(String.valueOf(count++));
      }
    };
  }

  public static void resetCounter ()
  {
    count = 0;
  }

  public static SortedSet<Event> getEvents ()
  {
    return events;
  }
}