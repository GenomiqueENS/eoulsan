package fr.ens.transcriptome.eoulsan.util;

import java.io.File;

/**
 * Get information about processor on Linux systems
 * @author Laurent Jourdren
 */
public class LinuxCpuInfo extends LinuxInfo {

  @Override
  protected File getInfoFile() {

    return new File("/proc/cpuinfo");
  }

  /**
   * Get the model of processor
   * @return the model of processor
   */
  public String getModelName() {

    return get("model name");
  }

  /**
   * Get the processor name.
   * @return the processor name
   */
  public String getProcessor() {

    return get("processor");
  }

  /**
   * Get CPU MHz.
   * @return the frequency of the processor
   */
  public String getCPUMHz() {

    return get("cpu MHz");
  }

  /**
   * Get processor bogomips.
   * @return the processor bogomips
   */
  public String getBogoMips() {

    return get("bogomips");
  }

  /**
   * Get the number of cores of the processor.
   * @return the number of core of the processor
   */
  public String getCores() {

    return get("cpu cores");
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public LinuxCpuInfo() {

    parse();
  }

}
