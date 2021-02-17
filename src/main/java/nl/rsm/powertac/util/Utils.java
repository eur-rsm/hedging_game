package nl.rsm.powertac.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;


public class Utils
{
  public static void growlMessage (FacesMessage.Severity severity, String title, String message)
  {
    FacesContext.getCurrentInstance().addMessage( null,
        new FacesMessage( severity, title, message ) );
  }
  
  public static void growlMessage (String title, String message)
  {
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_INFO, title, message));
  }

  public static void growlMessage (String message)
  {
    growlMessage("Error", message);
  }

  public static void redirect() {
    redirect( null );
  }
  
  public static void redirect ( String arg )
  {
    try {
      ExternalContext externalContext =
          FacesContext.getCurrentInstance().getExternalContext();
      externalContext.redirect( arg == null ? "index.xhtml" : arg );
    }
    catch (Exception ignored) {
    }
  }
}
