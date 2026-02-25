package fr.ens.biologie.genomique.eoulsan.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility methods that will be soon merged in Kenetre.
 * @author Laurent Jourdren
 * @since 2.7
 */
public class EoulsanUtils {

  /**
   * This method create a string with a date like the Date.toString() method but
   * with the new Java 8+ date time API.
   * @param date milliseconds since epoch.
   * @return a String with the date
   */
  public static final String datetoString(long date) {

    return Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy",
            Locale.ENGLISH));
  }

  /**
   * This method create a string with a date like the Date.toString() method but
   * with the new Java 8+ datetime API. this method use the current number of
   * milliseconds since the UNIX epoch.
   * @return a String with the date
   */
  public static final String datetoString() {

    return datetoString(System.currentTimeMillis());
  }

  /**
   * Sleep several milliseconds without throwing an exception.
   * @param millis milliseconds to sleep
   */
  public static final void silentSleep(int millis) {

    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // Do not handle interruption exception
    }
  }

}
