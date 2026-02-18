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

package fr.ens.biologie.genomique.eoulsan.data.protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

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
  public Path getSourceAsPath(final DataFile dataFile) {

    if (dataFile == null || dataFile.getSource() == null) {
      throw new NullPointerException("The source is null.");
    }

    final String protocolName = dataFile.getProtocolPrefixInSource();

    if (protocolName == null) {
      return Path.of(dataFile.getSource());
    }

    return Path.of(dataFile.getSource().substring(protocolName.length() + 1));
  }

  @Override
  public File getSourceAsFile(final DataFile dataFile) {

    Path result = getSourceAsPath(dataFile);

    return result == null ? null : result.toFile();
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

      // Broken link
      if (exists(src, false)) {
        final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
        setLinkTargetInMetadata(result, getSourceAsFile(src).toPath());
        return result;
      }

      throw new FileNotFoundException("File not found: " + src);
    }

    final File f = getSourceAsFile(src);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(f.length());
    result.setLastModified(f.lastModified());

    final DataFormat format = DataFormatRegistry.getInstance()
        .getDataFormatFromFilename(src.getName());

    result.setDataFormat(format);

    if (format != null) {
      result.setContentType(format.getContentType());
    } else {
      result.setContentType(StringUtils.getCommonContentTypeFromExtension(
          StringUtils.extensionWithoutCompressionExtension(src.getName())));
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
      setLinkTargetInMetadata(result, f.toPath());
    }

    return result;
  }

  private static void setLinkTargetInMetadata(
      final SimpleDataFileMetadata result, Path link) throws IOException {

    try {
      result.setSymbolicLink(new DataFile(Files.readSymbolicLink(link)));
    } catch (FileSystemException e) {
      // Do nothing
      // TODO In some case on a cluster Files.readSymbolicLink() throw an IO
      // Error
    }
  }

  @Override
  public boolean exists(final DataFile src, final boolean followLink) {

    final Path path = getSourceAsFile(src).toPath();

    return followLink
        ? Files.exists(path) : Files.exists(path, LinkOption.NOFOLLOW_LINKS);
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
      throw new NullPointerException("target argument is null");
    }

    if (link == null) {
      throw new NullPointerException("link argument is null");
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

    final Path targetPath = target.toFile().toPath();
    final Path linkPath = link.toFile().toPath();

    Files.createSymbolicLink(linkPath, targetPath);
  }

  @Override
  public boolean canSymlink() {

    return true;
  }

  @Override
  public void delete(final DataFile file, final boolean recursive)
      throws IOException {

    final Path path = getSourceAsFile(file).toPath();

    // Check if use wants to remove /
    if (Path.of("/").equals(path.normalize().toAbsolutePath())) {
      throw new IOException("Cannot remove /: " + file);
    }

    // Non recursive deletion
    if (!(recursive && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {

      Files.delete(path);
      return;
    }

    // Remove recursively a directory
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {

        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException e)
          throws IOException {

        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException e)
          throws IOException {

        throw new IOException("Cannot remove file: " + file);
      }

    });

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

    if (files == null) {
      return Collections.emptyList();
    }

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

  @Override
  public void rename(final DataFile src, final DataFile dest)
      throws IOException {

    if (dest == null) {
      throw new NullPointerException("dest argument is null");
    }

    if (dest.getProtocol() != this) {
      throw new IOException("the protocol of the dest is not "
          + getName() + " protocol: " + dest);
    }

    final File file = getSourceAsFile(src);
    final File destFile = getSourceAsFile(dest);

    if (!file.renameTo(destFile)) {
      throw new IOException("Cannot rename " + src + " to " + dest);
    }
  }

  @Override
  public boolean canRename() {

    return true;
  }

}
