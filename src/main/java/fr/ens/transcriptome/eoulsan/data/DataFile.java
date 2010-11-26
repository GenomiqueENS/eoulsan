package fr.ens.transcriptome.eoulsan.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocol;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocolService;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a DataFile.
 * @author Laurent Jourdren
 */
public class DataFile {

  /** Logger. */
  private static final Logger logger = Logger.getLogger(Globals.APP_NAME);

  /** The separator char ('/'). */
  public static final char separatorChar = '/';

  /** The separator char ('/') as a String. */
  public static final String separator = "" + separatorChar;

  private String src;
  private String name;
  private String protocolPrefixInSource;

  private DataProtocol protocol;
  private DataFileMetadata md;

  private String unknownProtocolName;

  //
  // Getters
  //

  /**
   * Get the source of this DataFile.
   * @return a String with the source of this DataFile
   */
  public String getSource() {

    return this.src;
  }

  /**
   * Get the source of this DataFile without extension.
   * @return a String with the source of this DataFile without extension
   */
  public String getSourceWithoutExtension() {

    final String newName = StringUtils.filenameWithoutExtension(this.name);
    final String result =
        this.src.substring(0, this.src.length() - this.name.length()) + newName;

    return result;
  }

  /**
   * Get the name of this DataFile.
   * @return a String with the name of this DataFile
   */
  public String getName() {

    return this.name;
  }

  /**
   * Get the protocol of this DataFile.
   * @return a DataProtocol
   */
  public DataProtocol getProtocol() throws IOException {

    if (this.protocol == null)
      throw new IOException("Unknow protocol: " + unknownProtocolName);

    return this.protocol;
  }

  /**
   * Get the metadata for this DataFile.
   * @return a DataFileMetadata with all metadata information about this
   *         DataFile
   * @throws IOException if the protocol is unknown or if the DataFile does not
   *           exists
   */
  public DataFileMetadata getMetaData() throws IOException {

    if (this.md == null)
      this.md = getProtocol().getMetadata(this);

    return this.md;
  }

  /**
   * Get the prefix of the protocol in the source name of the DataFile.
   * @return the prefix of the protocol
   */
  public String getProtocolPrefixInSource() {

    return this.protocolPrefixInSource;
  }

  //
  // Other methods
  //

  /**
   * Create an OutputStream for the DataFile.
   * @return an OutputStream object
   * @throws IOException if an error occurs while creating the DataFile
   */
  public OutputStream create() throws IOException {

    return getProtocol().putData(this, md == null ? null : md);
  }

  /**
   * Create an InputStream for the DataFile.
   * @return an InputStream object
   * @throws IOException if an error occurs while opening the DataFile
   */
  public InputStream open() throws IOException {

    final InputStream is = rawOpen();
    final DataFileMetadata md = getMetaData();

    final CompressionType ct =
        CompressionType.getCompressionTypeByContentEncoding(md
            .getContentEncoding());

    if (ct == null)
      return is;

    return ct.createInputStream(is);
  }

  /**
   * Create an InputStream for the DataFile.
   * @return an InputStream object
   * @throws IOException if an error occurs while opening the DataFile
   */
  public InputStream rawOpen() throws IOException {

    return getProtocol().getData(this);
  }

  /**
   * Copy this DataFile in a other DataFile.
   * @param dest destination DataFile
   * @throws IOException if an error occurs while copying the DataFile
   */
  public void copyTo(final DataFile dest) throws IOException {

    if (dest == null)
      throw new NullPointerException("The destination DataFile is null.");

    dest.getProtocol().putData(this, dest);
  }

  /**
   * Check if this DataFile exists.
   * @return true if this DataFile exists
   */
  public boolean exists() {

    try {
      return getProtocol().exists(this);
    } catch (IOException e) {

      return false;
    }
  }

  @Override
  public String toString() {

    return this.src;
  }

  //
  // Internal methods
  //

  /**
   * Find the protocol for this DataFile.
   * @param src the Data File source
   */
  private String findProtocol(final String src) {

    final int len = src.length();

    int pos = -1;

    for (int i = 0; i < len; i++) {

      int c = src.charAt(i);

      if (!(Character.isDigit(c) || Character.isLetter(c))) {

        pos = i;
        break;
      }
    }

    if (pos == -1)
      return null;

    if (len <= pos + 1)
      return null;

    if (src.charAt(pos) == ':' && src.charAt(pos + 1) == '/')
      return src.substring(0, pos);

    return null;
  }

  /**
   * Parse the source
   * @param source the source of the DataFile
   */
  private void parseSource(final String source) {

    // Looking for the protocol
    this.protocolPrefixInSource = findProtocol(source);
    final DataProtocolService registery = DataProtocolService.getInstance();
    //final DataProtocolRegistry registery = DataProtocolRegistry.getInstance();

    if (protocolPrefixInSource == null)
      this.protocol = registery.getDefaultProtocol();
    else
      this.protocol = registery.getProtocol(protocolPrefixInSource);

    if (this.protocol == null) {
      logger.severe("Unknown protocol: \""
          + protocolPrefixInSource + "\", can't set protocol for DataFile.");
      this.unknownProtocolName = protocolPrefixInSource;
    }

    // Set the source name
    this.src = source;

    final int lastSlashPos = source.lastIndexOf(separatorChar);

    if (lastSlashPos == -1)
      this.name = source;
    else
      this.name = source.substring(lastSlashPos + 1);

  }

  @Override
  public boolean equals(final Object o) {

    if (o == null)
      return false;

    if (!(o instanceof DataFile))
      return false;

    final DataFile df = (DataFile) o;

    return this.src.equals(df.src);
  }

  @Override
  public int hashCode() {

    return this.src.hashCode();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param source the source of the DataFile
   * @throws EoulsanException if an error occurs when searching the protocol of
   *           the source
   */
  public DataFile(final String source) {

    if (source == null)
      throw new NullPointerException("The source can not be null.");

    parseSource(source);
  }

  /**
   * Public constructor.
   * @param file the parent file of the DataFile
   * @param filename the filename of the DataFile
   * @throws EoulsanException if an error occurs when searching the protocol of
   *           the source
   */
  public DataFile(final DataFile parentFile, final String filename) {

    if (parentFile == null)
      throw new NullPointerException("The parent file can not be null.");

    if (filename == null)
      throw new NullPointerException("The name can not be null.");

    parseSource(parentFile.getSource() + separator + filename);
  }

}
