/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class allow to upload a file to HDFS.
 * @author Laurent Jourdren
 */
public class HDFSFileUploader implements FileUploader {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static long MAX_LEN_STD_COPY = 10 * 1024 * 1024L;

  // Create configuration object
  private final Configuration conf;

  private DataSource src;
  private String dest;
  private boolean outputStreamMode;

  private Map<String, String> distCp = new HashMap<String, String>();

  @Override
  public void init(final DataSource src, final String dest) {

    this.src = src;
    this.dest = dest;
  }

  @Override
  public void prepare() throws IOException {
  }

  @Override
  public void upload() throws IOException {

    if (this.outputStreamMode) {
      logger.info("Copy [outputStream] to " + this.dest);
      return;
    }

    boolean copied = false;

    if (this.src != null) {

      // Create a valid URL for file on local file system
      String srcName = this.src.getSourceInfo();
      if (srcName.startsWith("/"))
        srcName = "file://" + this.src.getSourceInfo();

      final Path src = new Path(srcName);
      final Path dest = new Path(this.dest);

      // No translation, tiny file
      if (src.getName().equals(dest.getName())
          && getFileSize(src) < MAX_LEN_STD_COPY) {

        logger.info("Copy [basic] " + this.src + " to " + this.dest);
        PathUtils.copy(src, dest, this.conf);
        copied = true;
      }

      // Network protocol
      if (StringUtils.startsWith(this.src.getSourceInfo(), new String[] {
          Common.S3_PROTOCOL + "://", "ftp://", "http://"})) {

        logger.info("Copy [distrituted] " + this.src + " to " + this.dest);
        this.distCp.put(this.src.getSourceInfo(), this.dest);
        copied = true;
      }
    }

    // Other cases
    if (!copied) {

      logger.info("Copy [standard] " + this.src + " to " + this.dest);
      final CopyDataSource cds = new CopyDataSource(this.src, this.dest);

      final Path destPath = new Path(this.dest);
      FileSystem fs = destPath.getFileSystem(conf);
      cds.copy(fs.create(destPath));
    }

  }

  private long getFileSize(final Path path) throws IOException {

    final FileSystem fs = path.getFileSystem(this.conf);
    return fs.getFileStatus(path).getLen();
  }

  @Override
  public OutputStream createUploadOutputStream() throws FileNotFoundException,
      IOException {

    final Path path = new Path(this.dest);
    final FileSystem fs = path.getFileSystem(this.conf);

    this.outputStreamMode = true;

    return fs.create(path);
  }

  @Override
  public String getSrc() {

    return this.src == null ? "[new file]" : this.src.toString();
  }

  @Override
  public String getDest() {

    return this.dest;
  }

  /**
   * Get the map of the files to copy in distributed mode.
   * @return a map with the list of files to copy
   */
  public Map<String, String> getDistCpEntries() {

    return this.distCp;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param conf Configuration object
   */
  public HDFSFileUploader(final Configuration conf) {

    this.conf = conf;
  }

}
