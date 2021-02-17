package nl.rsm.powertac.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.myfaces.webapp.StartupServletContextListener;

import java.util.Set;


public class Initializer
extends StartupServletContextListener
implements ServletContextListener
{
  @SuppressWarnings( "deprecation" )
  public void contextDestroyed (ServletContextEvent e)
  {
    super.contextDestroyed( e );
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
    for (Thread t: threadArray) {
      if (t.getName().contains("Abandoned connection cleanup thread")) {
        synchronized(t) {
          t.stop(); //don't complain, it works
        }
      }
    }
  }

  public void contextInitialized (ServletContextEvent e)
  {
    super.contextInitialized( e );
  }
}