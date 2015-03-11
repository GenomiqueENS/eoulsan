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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class implements a File Protocol.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class FileDataProtocol extends AbstractDataProtocol {

  /** Protocol name. */
  public static final String PROTOCOL_NAME = "file";

  @Override
  public String getName() {

    return PROTOCOL_NAME;
  }

  @Override
  public File getSourceAsFile(final DataFile dataFile) {

    if (dataFile == null || dataFile.getSource() == null) {
      throw new NullPointerException("The source is null.");
    }

    final String protocolName = dataFile.getProtocolPrefixInSource();

    if (protocolName == null) {
      return new File(dataFile.getSource()).getAbsoluteFile();
    }

    return new File(dataFile.getSource().substring(protocolName.length() + 1));
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return FileUtils.createInputStream(getSourceAsFile(src));
  }

  @Override
  public OutputStream putData(final DataFile src) throws IOException {

    return FileUtils.createOutputStream(getSourceAsFile(src));
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src, true)) {
      throw new FileNotFoundException("File not found: " + src);
    }

    final File f = getSourceAsFile(src);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(f.length());
    result.setLastModified(f.lastModified());

    final DataFormat format =
        DataFormatRegistry.getInstance().getDataFormatFromFilename(
            src.getName());

    result.setDataFormat(format);

    if (format != null) {
      result.setContentType(format.getContentType());
    } else {
      result.setContentType(StringUtils
          .getCommonContentTypeFromExtension(StringUtils
              .extensionWithoutCompressionExtension(src.getName())));
    }

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(src.getSource());

    if (ct != null) {
      result.setContentEncoding(ct.getContentEncoding());
    }

    if (f.isDirectory()) {
      result.setDirectory(true);
    }

    if (Files.isSymbolicLink(f.toPath())) {
      result.setSymbolicLink(true);
    }

    return result;
  }

  @Override
  public boolean exists(final DataFile src, final boolean followLink) {

    // return getSourceAsFile(src).exists();

    final Path path = getSourceAsFile(src).toPath();

    if (followLink) {
      Files.exists(path);
    }

    return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
  }

  @Override
  public boolean canRead() {

    return true;
  }

  @Override
  public boolean canWrite() {

    return true;
  }

  @Override
  public void mkdir(final DataFile dir) throws IOException {

    final File file = getSourceAsFile(dir);

    if (!file.mkdir()) {
      throw new IOException("Unable to create the directory: " + dir);
    }

  }

  @Override
  public void mkdirs(final DataFile dir) throws IOException {

    final File file = getSourceAsFile(dir);

    if (!file.mkdirs()) {
      throw new IOException("Unable to create the directory: " + dir);
    }
  }

  @Override
  public boolean canMkdir() {

    return true;
  }

  @Override
  public void symlink(final DataFile target, final DataFile link)
      throws IOException {

    if (target == null) {
      throw new NullPointerException("target is null");
    }

    if (link == null) {
      throw new NullPointerException("link is null");
    }

    if (link.exists()) {
      throw new IOException("the symlink already exists");
    }

    if (target.getProtocol() != this) {
      throw new IOException("the protocol of the target is not "
          + getName() + " protocol: " + target);
    }

    if (link.getProtocol() != this) {
      throw new IOException("the protocol of the link is not "
          + getName() + " protocol: " + link);
    }

    final File targetFile = target.toFile();
    final File linkFile = link.toFile();

    FileUtils.createSymbolicLink(targetFile, linkFile);
  }

  @Override
  public boolean canSymlink() {

    return true;
  }

  @Override
  public void delete(final DataFile file) throws IOException {

    final Path path = getSourceAsFile(file).toPath();

    Files.delete(path);
  }

  @Override
  public boolean canDelete() {

    return true;
  }

  @Override
  public List<DataFile> list(final DataFile file) throws IOException {

    final File directoryFile = getSourceAsFile(file);

    if (!directoryFile.exists()) {
      throw new FileNotFoundException("File not found: " + file);
    }

    if (!directoryFile.isDirectory()) {
      throw new IOException("The file is not a directory: " + file);
    }

    // List directory
    final File[] files = directoryFile.listFiles();

    // Convert the File array to a list of DataFile
    final List<DataFile> result = new ArrayList<>(files.length);
    for (File f : files) {
      result.add(new DataFile(f));
    }

    // Return an unmodifiable list
    return Collections.unmodifiableList(result);
  }

  @Override
  public boolean canList() {

    return true;
  }

}
