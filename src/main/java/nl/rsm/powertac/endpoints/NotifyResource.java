package nl.rsm.powertac.endpoints;

import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import java.util.HashSet;
import java.util.Set;


@PushEndpoint(NotifyResource.channel + "{user}")
@ManagedBean
public class NotifyResource
{
  protected static final String channel = "/notify/";

  @PathParam("user")
  private String user;

  private static Set<String> users = new HashSet<String>();

  public static void sendMessage (String user, String title, String content)
  {
    if (users.contains(user) || user.equals("*")) {
      EventBus eventBus = EventBusFactory.getDefault().eventBus();
      eventBus.publish(channel + user, new FacesMessage(title, content));
    }
  }

  @OnOpen
  public void onOpen (RemoteEndpoint r, EventBus eventBus)
  {
    if (user != null) {
      users.add(user);
    }
  }

  @OnClose
  public void onClose (RemoteEndpoint r, EventBus eventBus)
  {
    users.remove(user);
  }

  @OnMessage(encoders = {JSONEncoder.class})
  public FacesMessage onMessage (FacesMessage message)
  {
    return message;
  }

  public String getChannel ()
  {
    return channel;
  }
}