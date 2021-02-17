package nl.rsm.powertac.endpoints;

import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

import javax.faces.bean.ManagedBean;


@PushEndpoint(RefreshResource.channel)
@ManagedBean
public class RefreshResource
{
  protected static final String channel = "/refresh";

  @OnMessage(encoders = {JSONEncoder.class})
  public String onMessage (String message)
  {
    return message;
  }

  public static void sendMessage (String message)
  {
    EventBus eventBus = EventBusFactory.getDefault().eventBus();
    eventBus.publish(channel, message);
  }

  public String getChannel ()
  {
    return channel;
  }
}