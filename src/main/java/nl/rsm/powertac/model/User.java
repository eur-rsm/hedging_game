package nl.rsm.powertac.model;

import nl.rsm.powertac.Database;
import nl.rsm.powertac.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;


@SessionScoped
@ManagedBean
public class User implements Serializable
{
  private static final long serialVersionUID = 1L;

  private static List<String> adminUsers = null;
  private static List<String> observerUsers = null;
  public static String guestUser = "Guest";

  private int userId = -1;
  private String username = guestUser;
  private String password;
  private String salt;
  private int gameId = 0;
  private Set<Plant> plants;

  public User ()
  {
  }

  public User (ResultSet resultSet) throws SQLException
  {
    this.userId = resultSet.getInt("userId");
    this.username = resultSet.getString("username");
    this.password = resultSet.getString("password");
    this.salt = resultSet.getString("salt");
    this.gameId = resultSet.getInt("gameId");
    this.plants = Plant.getPlantsByUser( this );
  }

  @Override
  public int hashCode() {
    return new Integer( userId ).hashCode();
  }
  
  @Override
  public boolean equals( Object other ) {
    return other instanceof User && ((User) other).userId == this.userId;
  }
  
  public boolean save ()
  {
    Database db = new Database();
    try {
      db.saveUser(this);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    finally {
      db.close();
    }

    return true;
  }
  
  public boolean savePlants ()
  {
    Database db = new Database();
    try {
      db.saveUserPlants( this );
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    finally {
      db.close();
    }

    return true;
  }
  
  public boolean touch() {
    Database db = new Database();
    try {
      db.touchUser(this);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    finally {
      db.close();
    }

    return true;
  }
  
  public long inactiveSeconds() {
    Database db = new Database();
    long time = Long.MAX_VALUE;
    try {
      time = db.inactiveSeconds( this );
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }
    return time;
  }

  public boolean isLoggedIn ()
  {
    return userId >= 0;
  }

  
  public boolean isUser() {
    return userId > 0 && ! isAdmin();
  }
  
  public boolean isAdmin () {
    if (adminUsers == null) {
      adminUsers = new Properties().getPropertyList("admin_users");
    }
    return adminUsers.contains(username);
  }
  
  public boolean isObserver () {
    if (observerUsers == null) {
      observerUsers = new Properties().getPropertyList("observer_users");
    }
    return observerUsers.contains(username);
  }


  public void logout ()
  {
    /* 
    try {
      Database db = new Database();
      db.assignUser( this, null );
    } catch ( SQLException e ) {
      e.printStackTrace();
    }*/
    
    userId = -1;
    gameId = -1;
    username = "Guest";
    salt = "";
    password = "";
  }

  public void setPasswordAndSalt (String password)
  {
    String genSalt = DigestUtils.md5Hex(Math.random() + (new Date()).toString());

    setPassword(DigestUtils.md5Hex(password + genSalt));
    setSalt(genSalt);
  }

  //<editor-fold desc="Getters and Setters">
  public int getUserId ()
  {
    return userId;
  }

  public void setUserId (int userId)
  {
    this.userId = userId;
  }

  public String getUsername ()
  {
    return username;
  }

  public void setUsername (String username)
  {
    this.username = username;
  }

  public String getSalt ()
  {
    return salt;
  }

  public void setSalt (String salt)
  {
    this.salt = salt;
  }

  public String getPassword ()
  {
    return password;
  }

  public void setPassword (String password)
  {
    this.password = password;
  }
  
  public int getGameId ()
  {
    return gameId;
  }

  public void setGameId (int gameId)
  {
    this.gameId = gameId;
  }
  
  //</editor-fold>

  
  public void setPlants( Set<Plant> plants ) {
    this.plants = plants;
  }
  
  public Set<Plant> getPlants() {
    return plants;
  }
  
  public static boolean loginUser (String username, String password)
  {
    User user = null;

    Database db = new Database();
    try {
      user = db.getUser(username);
      
      if ( user == null || ! DigestUtils.md5Hex(password + user.salt).equals(user.password)) {
        // not found or password is incorrect
        throw new RuntimeException( "Username and/or password incorrect" );
      }
      
      Game game = Game.getActiveGame();
      
      if ( ! user.isAdmin() && ! user.isObserver() && user.getGameId() != game.getGameId() ) {
        user.setGameId( game.getGameId() );
        Set<Plant> plants = Plant.getPlantsByUser( user );
        if ( plants.size() != game.getNumPlants() ) {
          int rows = db.assignFreePlants( user, game );
          if ( rows != game.getNumPlants() ) {
            throw new RuntimeException( "No free plants to assign to this user!" );
          }
        }
        user.setPlants( Plant.getPlantsByUser( user ) );
        db.assignUser( user, game );
      }
      
      user.touch();
      
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }

    if (user == null) {
      throw new RuntimeException( "Unknown problem logging in" );
    }

    FacesContext.getCurrentInstance().getExternalContext()
        .getSessionMap().put("user", user);

    return true;
  }

  public static User getUserByName (String userName)
  {
    User user = null;

    Database db = new Database();
    try {
      user = db.getUser(userName);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }

    return user;
  }

  public static User getCurrentUser ()
  {
    return (User) FacesContext.getCurrentInstance().getExternalContext()
        .getSessionMap().get("user");
  }

  public static List<User> notSavedUsers( Game game, int round, int period )
  {
    List<User> missingUsers = null;

    Database db = new Database();
    try {
      missingUsers = db.getMissingUsers( game, round, period );
      for ( Iterator<User> it = missingUsers.iterator(); it.hasNext(); ) {
        User user = it.next();
        if ( user.isObserver() ) {
          it.remove();
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      db.close();
    }

    return missingUsers;
  }
}
