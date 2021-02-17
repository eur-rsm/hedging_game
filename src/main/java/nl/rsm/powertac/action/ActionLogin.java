package nl.rsm.powertac.action;

import nl.rsm.powertac.model.User;
import nl.rsm.powertac.util.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;


@ManagedBean
@RequestScoped
public class ActionLogin
{
  private String username;
  private String password;
  private String message;
  
  public String getUsername ()
  {
    return this.username;
  }

  public void setUsername (String username)
  {
    this.username = username;
  }

  public String getPassword ()
  {
    return password;
  }

  public void setPassword (String password)
  {
    this.password = password;
  }
 
  public String getMessage() {
    return message;
  }
  
  public void login ()
  {
    try {
      User.loginUser(getUsername(), getPassword() );
    } catch ( Exception x ) {
      message = x.getMessage();
    }
  }

  public void logout ()
  {
    User.getCurrentUser().logout();
    Utils.redirect( "login.xhtml" );
  }
}
