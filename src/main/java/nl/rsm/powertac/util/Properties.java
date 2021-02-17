package nl.rsm.powertac.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class Properties
{
  private static String resourceName = "server.properties";
  private static java.util.Properties properties = new java.util.Properties();
  private static boolean loaded = false;

  public String getProperty (String key)
  {
    loadIfNecessary();
    return properties.getProperty(key);
  }

  public String getProperty (String key, String defaultValue)
  {
    loadIfNecessary();
    return properties.getProperty(key, defaultValue);
  }

  public int getPropertyInt (String key)
  {
    loadIfNecessary();
    return Integer.parseInt(properties.getProperty(key));
  }

  public int getPropertyInt (String key, int defaultValue)
  {
    loadIfNecessary();
    return Integer.parseInt(
        properties.getProperty(key, String.valueOf(defaultValue)));
  }

  public double getPropertyDouble (String key)
  {
    loadIfNecessary();
    return Double.parseDouble(properties.getProperty(key));
  }

  public boolean getPropertyBool (String key)
  {
    loadIfNecessary();
    String value = properties.getProperty(key).toLowerCase();
    return Boolean.valueOf(value);
  }

  public boolean getPropertyBool (String key, boolean defaultValue)
  {
    loadIfNecessary();
    String value = properties.getProperty(key).toLowerCase();
    if (value.isEmpty()) {
      return defaultValue;
    }
    return Boolean.valueOf(value);
  }

  public List<String> getPropertyList(String name)
  {
    String tmp = properties.getProperty(name);
    return Arrays.asList(tmp.replace(" ", "").split(","));
  }

  // lazy loader
  private void loadIfNecessary ()
  {
    if (!loaded) {
      try {
        properties.load(Properties.class.getClassLoader()
            .getResourceAsStream(resourceName));
        loaded = true;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}