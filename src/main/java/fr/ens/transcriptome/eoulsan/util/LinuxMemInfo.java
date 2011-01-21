package fr.ens.transcriptome.eoulsan.util;

import java.io.File;

/**
 * Get information about memory on Linux systems
 * @author Laurent Jourdren
 */
public class LinuxMemInfo extends LinuxInfo {

  @Override
  protected File getInfoFile() {

    return new File("/proc/meminfo");
  }

  /**
   * Get the total memory of the system.
   * @return the total memory of the system
   */
  public String getMemTotal() {

    return get("MemTotal");
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public LinuxMemInfo() {

    parse();
  }

}
