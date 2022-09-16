package fr.ens.biologie.genomique.eoulsan;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.util.LinuxCpuInfo;
import fr.ens.biologie.genomique.eoulsan.util.LinuxMemInfo;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.SystemUtils;

/**
 * This class gathers information about Eoulsan configuration and system
 * environment.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class Infos {

  private static final String NOT_SET = "(Not set)";

  /**
   * This class define an info.
   */
  public static class Info {

    private final String name;
    private final List<String> values;

    /**
     * Get the name of the information
     * @return the name of the information
     */
    public String getName() {
      return this.name;
    }

    /**
     * Get the value of the information
     * @return the value of the information
     */
    public List<String> getValues() {
      return this.values;
    }

    //
    // Methods
    //

    /**
     * Get the maximal length of the values
     * @return the maximal length of the values
     */
    public int maxValueLength() {

      int result = -1;

      for (String s : this.values) {
        result = Math.max(result, s.length());
      }

      return result;
    }

    //
    // Object methods
    //

    @Override
    public int hashCode() {

      return Objects.hash(this.name, this.values);
    }

    @Override
    public boolean equals(Object obj) {

      if (obj == null) {
        return false;
      }

      if (obj == this) {
        return true;
      }

      if (!(obj instanceof Info)) {
        return false;
      }

      Info that = (Info) obj;

      return Objects.equals(this.name, that.name)
          && Objects.equals(this.values, that.values);
    }

    //
    // Constructors
    //

    /**
     * Constructor.
     * @param name name of the info
     * @param value value of the info
     */
    public Info(final String name, final int value) {

      this(name, "" + value);
    }

    /**
     * Constructor.
     * @param name name of the info
     * @param value value of the info
     */
    public Info(final String name, final boolean value) {

      this(name, value ? "True" : "False");
    }

    /**
     * Constructor.
     * @param name name of the info
     * @param value value of the info
     */
    public Info(final String name, final String value) {

      this(name, value, NOT_SET);
    }

    /**
     * Constructor.
     * @param name name of the info
     * @param value value of the info
     * @param defaultValue default value
     */
    public Info(final String name, final String value,
        final String defaultValue) {

      this(name,
          value == null
              ? Collections.singletonList(defaultValue)
              : Collections.singletonList(value));
    }

    /**
     * Constructor.
     * @param name name of the info
     * @param values values of the info
     */
    public Info(final String name, final List<String> values,
        final String defaultValue) {

      this(name,
          values.isEmpty() ? Collections.singletonList(NOT_SET) : values);
    }

    /**
     * Constructor.
     * @param name name of the info
     * @param values values of the info
     */
    public Info(final String name, final List<String> values) {

      Objects.requireNonNull(name, "name argument cannot be null");
      Objects.requireNonNull(values, "values argument cannot be null");

      this.name = name;
      this.values = values;
    }
  }

  /**
   * This class define a list of info. It just avoid too long code lines when
   * creatig a new Info object and adding it to a List.
   */
  private static class ListInfo {

    private final List<Info> infos = new ArrayList<>();

    public List<Info> getList() {
      return this.infos;
    }

    /**
     * Add an info to the list.
     * @param name name of the info
     * @param value value of the info
     */
    public void add(final String name, final int value) {

      this.infos.add(new Info(name, value));
    }

    /**
     * Add an info to the list.
     * @param name name of the info
     * @param value value of the info
     */
    public void add(final String name, final boolean value) {

      this.infos.add(new Info(name, value));
    }

    /**
     * Add an info to the list.
     * @param name name of the info
     * @param value value of the info
     */
    public void add(final String name, final String value) {

      this.infos.add(new Info(name, value));
    }

    /**
     * Add an info to the list.
     * @param name name of the info
     * @param values values of the info
     * @param defaultValue default value
     */
    public void add(final String name, final List<String> values,
        final String defaultValue) {

      this.infos.add(new Info(name, values, defaultValue));

    }

  }

  /**
   * This class implements a "df" like command based on the FileStore class.
   */
  private static class DiskFree {

    private final FileStore fileStore;

    /**
     * Get the capacity of the partition.
     * @return the capacity of the partition
     * @throws IOException if an error occurs while getting information about
     *           the partition
     */
    public long getCapacity() throws IOException {
      return this.fileStore.getTotalSpace();
    }

    /**
     * Get the number of bytes used of the partition.
     * @return the number of bytes used of the partition
     * @throws IOException if an error occurs while getting information about
     *           the partition
     */
    public long getUsed() throws IOException {
      return this.fileStore.getTotalSpace() - this.fileStore.getUsableSpace();
    }

    /**
     * Get the number of byte available of the partition.
     * @return the number of bytes used of the partition
     * @throws IOException if an error occurs while getting information about
     *           the partition
     */
    public long getAvailable() throws IOException {
      return this.fileStore.getUsableSpace();
    }

    /**
     * Get the usage of the partition in percent of the partition.
     * @return the usage of the partition in percent of the partition as a
     *         double
     * @throws IOException if an error occurs while getting information about
     *           the partition
     */
    public double getPercentUsed() throws IOException {

      return (double) getUsed() / getCapacity();
    }

    /**
     * Get the usage of the partition in percent of the partition.
     * @return the usage of the partition in percent of the partition as an
     *         integer
     * @throws IOException if an error occurs while getting information about
     *           the partition
     */
    public double getIntPercentUsed() throws IOException {

      return (int) (getPercentUsed() * 100);
    }

    /**
     * Get the description of the partition
     * @return the description of the partition
     */
    public String getDescription() {
      return this.fileStore.name()
          + " : " + this.fileStore.type() + " : " + this.fileStore.toString();
    }

    /**
     * Constructor.
     * @param file a file of the partition
     * @throws IOException if an error occurs while getting information about
     *           the partition
     */
    public DiskFree(final File file) throws IOException {

      this.fileStore = Files.getFileStore(file.toPath());
    }

  }

  /**
   * Log a list of Info.
   * @param level Log level
   * @param listInfos the list of info to log
   */
  public static void log(Level level, List<Info> listInfos) {

    Objects.requireNonNull(level, "level argument cannot be null");
    Objects.requireNonNull(listInfos, "listInfos argument cannot be null");

    for (Info i : listInfos) {
      log(level, i);
    }
  }

  /**
   * Log a list of Info.
   * @param level Log level
   * @param info info to log
   */
  public static void log(final Level level, final Info info) {

    Objects.requireNonNull(level, "level argument cannot be null");
    Objects.requireNonNull(info, "info argument cannot be null");

    final Logger logger = EoulsanLogger.getLogger();

    logger.log(level,
        info.getName() + ": " + StringUtils.join(info.getValues(), " "));
  }

  /**
   * Get information about the partition of a file.
   * @param partition a file in the partition
   */
  public static Info diskFreeInfo(final File partition) throws IOException {

    DiskFree df = new DiskFree(partition);

    return new Info(df.getDescription(),
        StringUtils.sizeToHumanReadable(df.getCapacity())
            + " capacity, " + StringUtils.sizeToHumanReadable(df.getUsed())
            + " used, " + StringUtils.sizeToHumanReadable(df.getAvailable())
            + " available, " + df.getIntPercentUsed() + "% used");
  }

  /**
   * Return a list with Eoulsan software informations.
   * @param main Eoulsan Main object
   * @return a list with Info objects
   */
  public static List<Info> softwareInfos(Main main) {

    requireNonNull(main, "main argument cannot be null");
    ListInfo li = new ListInfo();

    // Show versions
    li.add(Globals.APP_NAME + " version", Globals.APP_VERSION_STRING);
    li.add(Globals.APP_NAME + " revision", Globals.APP_BUILD_COMMIT);
    li.add(Globals.APP_NAME + " build date", Globals.APP_BUILD_DATE);
    li.add(Globals.APP_NAME + " website", Globals.WEBSITE_URL);
    li.add(Globals.APP_NAME + " contact", Globals.CONTACT_EMAIL);
    li.add(Globals.APP_NAME + " disscusion group", Globals.DISCUSSION_GROUP);

    // Startup script
    li.add(Globals.APP_NAME + " Startup script",
        (main.getLaunchScriptPath() == null
            ? "(no startup script)" : main.getLaunchScriptPath()));

    // Eoulsan installation directory
    li.add(Globals.APP_NAME + " installation directory",
        (main.getEoulsanDirectory() == null
            ? "(installation directory not found)"
            : main.getEoulsanDirectory().toString()));

    return li.getList();
  }

  /**
   * Return a list with command line informations.
   * @param main Eoulsan Main object
   * @return a list with Info objects
   */
  public static List<Info> commandLineInfo(final Main main) {

    requireNonNull(main, "main argument cannot be null");
    ListInfo li = new ListInfo();

    // Command line arguments
    final List<String> args = new ArrayList<>();
    for (String a : main.getArgs()) {
      if (a.indexOf(' ') != -1) {
        args.add("\"" + a + "\"");
      } else {
        args.add(a);
      }
    }

    li.add("Command line", Joiner.on(' ').join(args));

    li.add("Configuration file", main.getConfigurationFileArgument());
    // li.add("Classpath", main.getClassPath());

    // Log file
    li.add("Log file", (main.getLogFileArgument() == null
        ? NOT_SET : main.getLogFileArgument()));

    // Log level
    li.add("Log level", getLogger().getLevel().getName());

    return li.getList();
  }

  /**
   * Return a list with system informations.
   * @return a list with Info objects
   */
  public static List<Info> systemInfos() {

    ListInfo li = new ListInfo();

    // Host
    li.add("Host", SystemUtils.getHostName());

    // Operating system
    li.add("Operating system name", System.getProperty("os.name"));
    li.add("Operating system version", System.getProperty("os.version"));
    li.add("Operating system arch", System.getProperty("os.arch"));

    // User information
    li.add("User name", System.getProperty("user.name"));
    li.add("User home", System.getProperty("user.home"));
    li.add("User current directory", System.getProperty("user.dir"));

    // Java version
    li.add("Java vendor", System.getProperty("java.vendor"));
    li.add("Java vm name", System.getProperty("java.vm.name"));
    li.add("Java version", System.getProperty("java.version"));
    li.add("Java max memory", "" + Runtime.getRuntime().maxMemory());

    return li.getList();
  }

  /**
   * Return a list with general configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> generalConf(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Debug mode", settings.isDebug());
    li.add("User interface", settings.getUIName());
    li.add("Maximum local thread count", settings.getLocalThreadsNumber());
    li.add("Print stack trace", settings.isPrintStackTrace());
    li.add("User defined temporary directory",
        settings.isUserDefinedTempDirectory());
    li.add("Temporary directory", settings.getTempDirectory());
    li.add("Executable temporary directory",
        settings.getExecutablesTempDirectory());
    li.add("Output type", settings.getOutputTreeType());
    li.add("Generate workflow image", settings.isSaveWorkflowImage());

    return li.getList();
  }

  /**
   * Return a list with modules and format informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> modulesAndFormatsInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    // Modules and formats
    li.add("Data format path", settings.getDataFormatPaths(), NOT_SET);
    li.add("Galaxy tools path", settings.getGalaxyToolPaths(), NOT_SET);
    li.add("Data format count",
        DataFormatRegistry.getInstance().getAllFormats().size());

    // TODO add Galaxy tools count
    // TODO add module count, version count
    // Add Eoulsan plugin path ?

    return li.getList();
  }

  /**
   * Return a list with storage informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> storageInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Genome description path", settings.getGenomeDescStoragePath());
    li.add("Genome mapper index storage path",
        settings.getGenomeMapperIndexStoragePath());
    li.add("Genome storage path", settings.getGenomeStoragePath());
    li.add("GFF storage path", settings.getGFFStoragePath());
    li.add("GTF storage path", settings.getGTFStoragePath());
    li.add("Additional annotation path",
        settings.getAdditionalAnnotationStoragePath());
    li.add("Hyperlinks conf file for additional annotation",
        settings.getAdditionalAnnotationHypertextLinksPath());

    return li.getList();
  }

  /**
   * Return a list with cluster configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> clusterInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Cluster scheduler", settings.getClusterSchedulerName());
    li.add("Default cluster memory required",
        settings.getDefaultClusterMemoryRequired() == -1
            ? NOT_SET : "" + settings.getDefaultClusterMemoryRequired());

    // HTCondor Concurency limit
    if (settings.getSetting("htcondor.concurrency.limits") != null) {
      li.add("HTCondor concurrency limits",
          settings.getSetting("htcondor.concurrency.limits"));
    }

    return li.getList();
  }

  /**
   * Return a list with Hadoop configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> hadoopInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Hadoop log level", settings.getHadoopLogLevel());
    li.add("ZooKeeper connection string", settings.getZooKeeperConnectString());
    li.add("ZooKeeper port", settings.getZooKeeperDefaultPort());
    li.add("ZooKeeper session timeout", settings.getZooKeeperSessionTimeout());

    return li.getList();
  }

  /**
   * Return a list with cloud configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> cloudInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Hadoop AWS access key", settings.getAWSAccessKey() != null
        ? Strings.repeat("X", settings.getAWSAccessKey().length()) : NOT_SET);
    li.add("Hadoop AWS secret key", settings.getAWSSecretKey() != null
        ? Strings.repeat("X", settings.getAWSSecretKey().length()) : NOT_SET);
    li.add("Obfuscate design", settings.isObfuscateDesign());
    li.add("Remove duplicate info when obfuscate design",
        settings.isObfuscateDesignRemoveReplicateInfo());

    return li.getList();
  }

  /**
   * Return a list with email configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> mailInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Send result by email", settings.isSendResultMail());
    li.add("Send results to", settings.getResultMail());
    li.add("SMTP server", settings.getSMTPHost());
    Properties p = settings.getJavaMailSMTPProperties();
    li.add("JavaMail configuration",
        p == null || p.isEmpty() ? NOT_SET : p.toString());

    return li.infos;
  }

  /**
   * Return a list with R and Rserve configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> rAndRserveInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Do not remove R scripts", settings.isSaveRscripts());
    li.add("Use RServe", settings.isRServeServerEnabled());
    li.add("RServe server name", settings.getRServeServerName());
    li.add("Keep files on RServe server", settings.isKeepRServeFiles());

    return li.infos;
  }

  /**
   * Return a list with Docker configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> dockerInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");
    ListInfo li = new ListInfo();

    li.add("Docker connection", settings.getDockerConnectionURI() == null
        ? NOT_SET : settings.getDockerConnectionURI().toString());
    li.add("Mount NFS roots in containers", settings.isDockerMountNFSRoots());

    return li.infos;
  }

  /**
   * Return a list with CPU informations.
   * @return a list with Info objects
   */
  public static List<Info> cpuInfo() {

    ListInfo li = new ListInfo();

    final LinuxCpuInfo cpuinfo = new LinuxCpuInfo();

    final String modelName = cpuinfo.getModelName();
    final String processor = cpuinfo.getProcessor();
    final String cpuMHz = cpuinfo.getCPUMHz();
    final String bogomips = cpuinfo.getBogoMips();
    final String cores = cpuinfo.getCores();

    li.add("CPU model name", (modelName == null ? "NA" : modelName));
    li.add("CPU count", (processor == null
        ? "NA" : "" + (Integer.parseInt(processor.trim()) + 1)));

    li.add("CPU cores", (cores == null ? "NA" : cores));
    li.add("CPU clock", (cpuMHz == null ? "NA" : cpuMHz) + " MHz");
    li.add("CPU Bogomips", (bogomips == null ? "NA" : bogomips));

    return li.infos;
  }

  /**
   * Return a list with memory informations.
   * @return a list with Info objects
   */
  public static List<Info> memInfo() {

    ListInfo li = new ListInfo();

    final LinuxMemInfo meminfo = new LinuxMemInfo();
    final String memTotal = meminfo.getMemTotal();

    li.add("Total memory", (memTotal == null ? "NA" : memTotal));

    return li.infos;
  }

  /**
   * Return a list with Docker configuration informations.
   * @param settings the Eoulsan settings
   * @return a list with Info objects
   */
  public static List<Info> partitionInfo(final Settings settings) {

    requireNonNull(settings, "settings argument cannot be null");

    Set<Info> set = new LinkedHashSet<>();

    for (String p : new HashSet<>(Arrays.asList("/", "/tmp", "/var",
        settings.getTempDirectory(), settings.getExecutablesTempDirectory()))) {

      if (p != null) {

        File f = new File(p);

        if (f.exists()) {
          try {
            set.add(diskFreeInfo(f));
          } catch (IOException e) {
            // Do nothing
          }
        }
      }
    }

    return new ArrayList<>(set);
  }

}
