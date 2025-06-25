/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.data;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocol;
import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocolService;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

/**
 * This class define a DataFile.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFile implements Comparable<DataFile>, Serializable {

  private static final long serialVersionUID = -3280343485491150872L;

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
   * Get the name of this DataFile.
   * @return a String with the name of this DataFile
   */
  public String getName() {

    return this.name;
  }

  /**
   * Get the base name of this DataFile without all its extensions. The result
   * is computed with the output of the getName() method.
   * @return a String with the base name of this DataFile
   */
  public String getBasename() {

    return StringUtils.basename(getName());
  }

  /**
   * Get the extension of this DataFile without compression extension. The
   * result is computed with the output of the getName() method.
   * @return a String with the extension of this DataFile. The result String is
   *         empty if there is no extension
   */
  public String getExtension() {

    return StringUtils.extensionWithoutCompressionExtension(getName());
  }

  /**
   * Get the base name of this DataFile with all its extensions (include
   * compression extension). The result is computed with the output of the
   * getName() method.
   * @return a String with the base name of this DataFile. The result String is
   *         empty if there is no extension
   */
  public String getFullExtension() {

    return StringUtils.extension(getName());
  }

  /**
   * Get the compression extension of this DataFile. The result is computed with
   * the output of the getName() method.
   * @return a String with the compression extension of this DataFile. The
   *         result String is empty if there is no compression extension
   */
  public String getCompressionExtension() {

    return StringUtils.compressionExtension(getName());
  }

  /**
   * Get the compression Type of this DataFile. The result is computed with the
   * output of the getName() method.
   * @return a CompressionType object
   */
  public CompressionType getCompressionType() {

    return CompressionType.getCompressionTypeByFilename(getName());
  }

  /**
   * Get the DataFormat of the DataFile. The result is computed with the output
   * of the getName() method. This is an alias for getDataFormatFromFilename()
   * of the DataFormatRegistry.
   * @return the DataFormat of the DataFile
   */
  public DataFormat getDataFormat() {

    return DataFormatRegistry.getInstance()
        .getDataFormatFromFilename(getName());
  }

  /**
   * Get the parent of this DataFile.
   * @return the parent DataFile
   * @throws IOException if an error occurs while the parent
   */
  public DataFile getParent() throws IOException {

    return getProtocol().getDataFileParent(this);
  }

  /**
   * Get the protocol of this DataFile.
   * @return a DataProtocol
   * @throws IOException if the protocol is unknown
   */
  public DataProtocol getProtocol() throws IOException {

    if (this.protocol == null) {
      throw new IOException("Unknown protocol: " + this.unknownProtocolName);
    }

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

    if (this.md == null) {
      this.md = getProtocol().getMetadata(this);
    }

    return this.md;
  }

  /**
   * Get the prefix of the protocol in the source name of the DataFile.
   * @return the prefix of the protocol
   */
  public String getProtocolPrefixInSource() {

    return this.protocolPrefixInSource;
  }

  /**
   * Test if the DataFile use the defaultProtocol.
   * @return true if the DataFile use the default protocol
   */
  public boolean isLocalFile() {

    try {
      return DataProtocolService.getInstance().getDefaultProtocol()
          .equals(getProtocol());
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Convert the DataFile object to File object if the underlying protocol allow
   * it. Only local protocol can return a value.
   * @return a File object or null if the underlying protocol does not allow it
   */
  public File toFile() {

    if (this.protocol == null) {
      return null;
    }

    return this.protocol.getSourceAsFile(this);
  }

  /**
   * Convert the DataFile object to Path object if the underlying protocol allow
   * it.
   * @return a Path object or null if the underlying protocol does not allow it
   */
  public Path toPath() {

    if (this.protocol == null) {
      return null;
    }

    final URI uri = toUri();

    if (uri == null) {
      return null;
    }

    return Paths.get(uri);
  }

  /**
   * Convert the DataFile object to an URI.
   * @return an URI object or null if the DataFile cannot be converted into URI
   */
  public URI toUri() {

    try {
      return new URI(this.src);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Convert the the DataFile to a DataFile where all the indirection has been
   * solved. This method use the JDK Path.toRealPath() method. This method has
   * only an effect for local DataFiles.
   * @return a DataFile
   */
  public DataFile toRealDataFile() {

    if (!isLocalFile()) {
      return this;
    }

    try {
      return new DataFile(toFile().toPath().toRealPath());
    } catch (IOException e) {
      return this;
    }
  }

  /**
   * Convert the the DataFile to a absolute DataFile. This method use the JDK
   * File.getAbsoluteFile() method. This method has only an effect for local
   * DataFiles.
   * @return a DataFile
   */
  public DataFile toAbsoluteDataFile() {

    if (!isLocalFile()) {
      return this;
    }

    return new DataFile(toFile().getAbsoluteFile());
  }

  //
  // Other methods
  //

  /**
   * Create an OutputStream for the DataFile. If the DataFile is declared as
   * compressed by its content type or its extension, the output stream will be
   * automatically compress data.
   * @return an OutputStream object
   * @throws IOException if an error occurs while creating the DataFile
   */
  public OutputStream create() throws IOException {

    final OutputStream os = rawCreate();

    final CompressionType ct;

    final String contentEncoding =
        this.md == null ? null : this.md.getContentEncoding();

    if (contentEncoding != null) {
      ct = CompressionType.getCompressionTypeByContentEncoding(contentEncoding);
    } else {
      ct = CompressionType.getCompressionTypeByFilename(getName());
    }

    if (ct == null) {
      return os;
    }

    return ct.createOutputStream(os);
  }

  /**
   * Create an OutputStream for the DataFile. The output stream will not
   * automatically compress data.
   * @return an OutputStream object
   * @throws IOException if an error occurs while creating the DataFile
   */
  public OutputStream rawCreate() throws IOException {

    return getProtocol().putData(this, this.md);
  }

  /**
   * Create an InputStream for the DataFile. If the DataFile is compressed, the
   * input stream will be automatically uncompress.
   * @return an InputStream object
   * @throws IOException if an error occurs while opening the DataFile
   */
  public InputStream open() throws IOException {

    final InputStream is = rawOpen();
    final DataFileMetadata md = getMetaData();

    final CompressionType ct = CompressionType
        .getCompressionTypeByContentEncoding(md.getContentEncoding());

    if (ct == null) {
      return is;
    }

    return ct.createInputStream(is);
  }

  /**
   * Create an InputStream for the DataFile. The input stream will not
   * automatically uncompress data.
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

    if (dest == null) {
      throw new NullPointerException("The destination DataFile is null.");
    }

    dest.getProtocol().putData(this, dest);
  }

  /**
   * Check if this DataFile exists.
   * @return true if this DataFile exists
   */
  public boolean exists() {

    return exists(true);
  }

  /**
   * Check if this DataFile exists.
   * @param followLink follow the link target if the file is a symbolic link
   * @return true if this DataFile exists
   */
  public boolean exists(final boolean followLink) {

    try {
      return getProtocol().exists(this, followLink);
    } catch (IOException e) {

      return false;
    }
  }

  /**
   * Create a directory with the path of the DataFile.
   * @throws IOException if an error occurs while creating the directory
   */
  public void mkdir() throws IOException {

    if (!getProtocol().canMkdir()) {
      throw new IOException(
          "The underlying protocol does not allow creating directories");
    }

    getProtocol().mkdir(this);
  }

  /**
   * Create a directory and its parents if not exists with the path of the
   * DataFile.
   * @throws IOException if an error occurs while creating the directory
   */
  public void mkdirs() throws IOException {

    if (!getProtocol().canMkdir()) {
      throw new IOException(
          "The underlying protocol does not allow creating directories");
    }

    getProtocol().mkdirs(this);
  }

  /**
   * Create a symbolic link that target is the current file.
   * @param link symbolic file
   * @throws IOException if an error occurs while creating the symbolic link
   */
  public void symlink(final DataFile link) throws IOException {

    symlink(link, false);
  }

  /**
   * Create a symbolic link that target is the current file.
   * @param link symbolic file
   * @param relativize relativize the link target path
   * @throws IOException if an error occurs while creating the symbolic link
   */
  public void symlink(final DataFile link, final boolean relativize)
      throws IOException {

    if (link == null) {
      throw new NullPointerException("The link can not be null.");
    }

    if (!getProtocol().canSymlink()) {
      throw new IOException(
          "The underlying protocol does not allow creating symbolic links");
    }

    if (relativize) {

      final DataFile parent = isLocalFile()
          ? new DataFile(getParent().toFile().getAbsoluteFile()) : getParent();

      final DataFile newTarget =
          new DataFile(relativize(link.getParent(), parent), this.getName());

      getProtocol().symlink(newTarget, link);

    } else {
      getProtocol().symlink(this, link);
    }
  }

  /**
   * Delete the DataFile.
   * @throws IOException if an error occurs while deleting the DataFile
   */
  public void delete() throws IOException {

    delete(false);
  }

  /**
   * Delete the DataFile.
   * @param recursive recursive deletion
   * @throws IOException if an error occurs while deleting the DataFile
   */
  public void delete(final boolean recursive) throws IOException {

    if (!getProtocol().canDelete()) {
      throw new IOException(
          "The underlying protocol does not allow deleting files");
    }

    getProtocol().delete(this, recursive);
  }

  /**
   * List the content of a directory.
   * @return a List with the content of the directory
   * @throws IOException if an error occurs while listing the directory
   */
  public List<DataFile> list() throws IOException {

    if (!getProtocol().canList()) {
      throw new IOException(
          "The underlying protocol does not allow to list a directory");
    }

    return getProtocol().list(this);
  }

  /**
   * Rename the DataFile.
   * @param dest destination DataFile
   * @throws IOException if an error occurs while renaming the DataFile
   */
  public void renameTo(final DataFile dest) throws IOException {

    if (!getProtocol().canRename()) {
      throw new IOException(
          "The underlying protocol does not allow to rename files");
    }

    getProtocol().rename(this, dest);
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

    if (pos == -1) {
      return null;
    }

    if (len <= pos + 1) {
      return null;
    }

    if (src.charAt(pos) == ':' && src.charAt(pos + 1) == '/') {
      return src.substring(0, pos);
    }

    return null;
  }

  /**
   * Parse the source
   * @param source the source of the DataFile
   */
  private void parseSource(final String source) {

    // Looking for the protocol
    this.protocolPrefixInSource = findProtocol(source);
    final DataProtocolService registry = DataProtocolService.getInstance();

    if (this.protocolPrefixInSource == null) {
      this.protocol = registry.getDefaultProtocol();
    } else {
      this.protocol = registry.newService(this.protocolPrefixInSource);
    }

    if (this.protocol == null) {
      getLogger().severe("Unknown protocol: \""
          + this.protocolPrefixInSource
          + "\", can't set protocol for DataFile.");
      this.unknownProtocolName = this.protocolPrefixInSource;
    }

    // Set the source name
    this.src = source;

    final int lastSlashPos = source.lastIndexOf(separatorChar);

    if (lastSlashPos == -1) {
      this.name = source;
    } else {
      this.name = source.substring(lastSlashPos + 1);
    }

  }

  /**
   * Relativize two path.
   * @param f1 first path
   * @param f2 second path
   * @return the relative path
   */
  private static DataFile relativize(final DataFile f1, final DataFile f2) {

    final URI uri1 = f1.toUri();
    final URI uri2 = f2.toUri();

    return new DataFile(uri1.relativize(uri2));
  }

  //
  // Object methods overrides
  //

  @Override
  public int compareTo(final DataFile o) {

    if (o == null) {
      throw new NullPointerException("argument cannot be null");
    }

    return this.src.compareTo(o.src);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof DataFile)) {
      return false;
    }

    final DataFile df = (DataFile) o;

    return this.src.equals(df.src);
  }

  @Override
  public int hashCode() {

    return this.src.hashCode();
  }

  @Override
  public String toString() {

    return this.src;
  }

  //
  // Serialization methods
  //

  /**
   * Serialize the object.
   * @param out the object output stream
   * @throws IOException if an error occurs while serializing the object
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {

    out.writeObject(this.src);
  }

  /**
   * Deserialize the object.
   * @param in the object input stream
   * @throws IOException if an error occurs while deserializing the object
   * @throws ClassNotFoundException if class not found while deserializing the
   *           object
   */
  private void readObject(final ObjectInputStream in)
      throws IOException, ClassNotFoundException {

    final String source = (String) in.readObject();

    parseSource(source);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param source the source of the DataFile
   */
  public DataFile(final String source) {

    if (source == null) {
      throw new NullPointerException("The source can not be null.");
    }

    parseSource(source);
  }

  /**
   * Public constructor.
   * @param parentFile the parent file of the DataFile
   * @param filename the filename of the DataFile
   */
  public DataFile(final DataFile parentFile, final String filename) {

    if (parentFile == null) {
      throw new NullPointerException("The parent file can not be null.");
    }

    if (filename == null) {
      throw new NullPointerException("The name can not be null.");
    }

    final String parentSource = parentFile.getSource();

    // If parent is empty, use only the filename
    if (parentSource == null || "".equals(parentSource)) {
      parseSource(filename);
    } else {
      parseSource(parentFile.getSource() + separator + filename);
    }
  }

  /**
   * Public constructor.
   * @param parentFile the parent file of the DataFile
   * @param filename the filename of the DataFile
   */
  public DataFile(final File parentFile, final String filename) {

    this(new DataFile(parentFile), filename);
  }

  /**
   * Public constructor.
   * @param parentPath the parent file of the DataFile
   * @param filename the filename of the DataFile
   */
  public DataFile(final Path parentPath, final String filename) {

    this(new DataFile(parentPath), filename);
  }

  /**
   * Public constructor.
   * @param file the source file of the DataFile
   */
  public DataFile(final File file) {

    if (file == null) {
      throw new NullPointerException("The source file can not be null.");
    }

    parseSource(file.getPath());
  }

  /**
   * Public constructor.
   * @param path the source path of the DataFile
   */
  public DataFile(final Path path) {

    if (path == null) {
      throw new NullPointerException("The source path can not be null.");
    }

    parseSource(path.toFile().getPath());
  }

  /**
   * Public constructor.
   * @param uri the URI of the DataFile
   */
  public DataFile(final URI uri) {

    if (uri == null) {
      throw new NullPointerException("The source URI can not be null.");
    }

    parseSource(uri.toString());
  }

}
