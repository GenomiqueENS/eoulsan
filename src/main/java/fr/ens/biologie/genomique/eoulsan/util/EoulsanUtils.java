package fr.ens.biologie.genomique.eoulsan.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class EoulsanUtils {

  public static final String datetoString(long date) {

    return Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy",
            Locale.ENGLISH));
  }

  public static final String datetoString() {

    return datetoString(System.currentTimeMillis());
  }

  public static final void silentSleep(int millis) {

    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // Do not handle interruption exception
    }
  }

}
