package nl.rsm.powertac.action;

import nl.rsm.powertac.model.User;
import nl.rsm.powertac.util.Utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;


@ManagedBean
@RequestScoped
public class ActionRegister
{
  private String username;
  private String password1;
  private String password2;
  private String message;
  
  public void register ()
  {
    if (nameExists(username)) {
      message = "Username already in use";
      return;
    }
    if (passwordMismatch(password1, password2)) {
      message = "Passwords don't match";
      return;
    }

    User user = new User();
    user.setUsername(username);
    user.setPasswordAndSalt(password1);
    
    boolean saved = user.save();

    if (!saved) {
      message = "Failed to save the user";
      return;
    }
    else {
      try {
        User.loginUser(username, password1);
        Utils.redirect();
      } catch ( Exception x ) {
        message = x.getMessage();
      }
    }
  }

  private boolean nameExists (String username)
  {
    User user = User.getUserByName(username);
    if (user != null) {
      Utils.growlMessage("Username taken, please select a new name");
      return true;
    }
    return false;
  }

  private boolean passwordMismatch (String password1, String password2)
  {
    if (!password1.equals(password2)) {
      Utils.growlMessage("Passwords do not match");
      return true;
    }
    return false;
  }

  //<editor-fold desc="Setters and Getters">
  public String getUsername ()
  {
    return username;
  }
  public void setUsername (String username)
  {
    this.username = username;
  }

  public String getPassword1 ()
  {
    return password1;
  }
  public void setPassword1 (String password1)
  {
    this.password1 = password1;
  }

  public String getPassword2 ()
  {
    return password2;
  }
  public void setPassword2 (String password2)
  {
    this.password2 = password2;
  }
  
  public String getMessage() {
    return message;
  }
  
  //</editor-fold>
}
