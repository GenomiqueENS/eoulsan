package fr.ens.transcriptome.eoulsan.datatypes.protocols;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;

public class DataProtocolRegistry {

  private static DataProtocolRegistry instance;

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final String ADDITIONAL_PROTOCOLS_PREFIX_KEY =
      "main.protocol.";
  private static final String ADDITIONAL_PROTOCOLS_SUFFIX_KEY = ".class";

  private Map<String, DataProtocol> protocols =
      new HashMap<String, DataProtocol>();
  private String defaultProtocolName;

  //
  // Static method
  //

  /**
   * Get the singleton of this class.
   * @return the singleton of this class
   */
  public static DataProtocolRegistry getInstance() {

    if (instance == null)
      instance = new DataProtocolRegistry();

    return instance;
  }

  //
  // Instance methods
  //

  /**
   * Register a protocol.
   * @param protocolName name of the protocol
   * @param protocol protocol to add
   */
  public void registerProtocol(final String protocolName,
      final DataProtocol protocol) {

    if (protocolName == null || protocol == null)
      return;

    logger.info("Register DataProtocol: " + protocolName);
    protocols.put(protocolName, protocol);
  };

  /**
   * Register a protocol.
   * @param protocolName name of the protocol
   * @param className name of the class for the protocol
   */
  @SuppressWarnings("static-access")
  public void registerProtocol(final String protocolName, final String className) {

    if (protocolName == null || protocolName == null)
      return;

    try {
      Class<?> clazz = DataProtocolRegistry.class.forName(className);

      registerProtocol(protocolName, clazz);

      logger.info("Add external protocol: " + protocolName);

    } catch (ClassNotFoundException e) {

      logger.severe("Cannot find " + className + " for step addon");
      throw new RuntimeException("Cannot find " + className + " for step addon");

    }
  }

  /**
   * Register a protocol.
   * @param protocolName name of the protocol
   * @param clazz class for the protocol
   */
  public void registerProtocol(final String protocolName, final Class<?> clazz) {

    if (clazz == null)
      return;

    final DataProtocol dt = testClassType(clazz);

    if (dt != null) {

      registerProtocol(protocolName.trim().toLowerCase(), dt);
    } else
      logger.warning("Addon " + clazz.getName() + " is not a protocol class");
  }

  private DataProtocol testClassType(final Class<?> clazz) {

    if (clazz == null)
      return null;

    try {

      final Object o = clazz.newInstance();

      if (o instanceof DataProtocol)
        return (DataProtocol) o;

      return null;

    } catch (InstantiationException e) {
      Common.showAndLogErrorMessage("Can't create instance of "
          + clazz.getName()
          + ". Maybe your class doesn't have a void constructor.");
    } catch (IllegalAccessException e) {
      Common.showAndLogErrorMessage("Can't access to " + clazz.getName());
    }

    return null;
  }

  /**
   * Get DataProtocol.
   * @param protocolName name of the protocol to get
   * @return a DataProtocol
   */
  public DataProtocol getProtocol(final String protocolName) {

    return this.protocols.get(protocolName);
  }
  
  /**
   * Test if a protocol exists.
   * @param protocolName name of the protocol to test
   * @return true if the protocol exists
   */
  public boolean isProtocol(final String protocolName) {

    return this.protocols.containsKey(protocolName);
  }

  /**
   * Get the default protocol.
   * @return the default DataProtocol
   */
  public DataProtocol getDefaultProtocol() {

    return getProtocol(this.defaultProtocolName);
  }

  /**
   * Register protocols from settings.
   */
  public void registerProtocolsFromSettings() {

    final Settings s = EoulsanRuntime.getSettings();

    for (String key : s.getSettingsNames())
      parseGlobalParameter(key, s.getSetting(key));
  }

  /**
   * Parse a global parameter and check if define an additional protocol.
   * @param key parameter key
   * @param value parameter value
   */
  public void parseGlobalParameter(final String key, final String value) {

    if (key == null || value == null)
      return;

    if (key.startsWith(ADDITIONAL_PROTOCOLS_PREFIX_KEY)
        && key.endsWith(ADDITIONAL_PROTOCOLS_SUFFIX_KEY)) {

      String protocolName =
          key.substring(ADDITIONAL_PROTOCOLS_PREFIX_KEY.length(), key.length()
              - ADDITIONAL_PROTOCOLS_PREFIX_KEY.length()
              - ADDITIONAL_PROTOCOLS_SUFFIX_KEY.length());

      registerProtocol(protocolName, value);
    }

  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DataProtocolRegistry() {

    registerProtocol("file", new FileDataProtocol());
    this.defaultProtocolName = "file";

    if (EoulsanRuntime.getRuntime().isHadoopMode()) {

      registerProtocol("hdfs", new PathDataProtocol());
      registerProtocol("ftp", new PathDataProtocol());
      registerProtocol("http", new PathDataProtocol());
      registerProtocol("s3", new PathDataProtocol());
    } else {

      registerProtocol("ftp", new URLDataProtocol());
      registerProtocol("http", new URLDataProtocol());
      registerProtocol("s3", new S3DataProtocol());
    }
  }

}
