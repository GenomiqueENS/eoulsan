/*
 * Nividic development code This code may be freely distributed and modified
 * under the terms of the GNU Lesser General Public Licence. This should be
 * distributed with the code. If you do not have a copy, see:
 * http://www.gnu.org/copyleft/lesser.html Copyright for this code is held
 * jointly by the microarray platform of the École Normale Supérieure and the
 * individual authors. These should be listed in @author doc comments. For more
 * information on the Nividic project and its aims, or to join the Nividic
 * mailing list, visit the home page at: http://www.transcriptome.ens.fr/nividic
 */

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.datasources.DataSourceUtils;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.DataSourceDistCp;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.DistCp;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class implements DataUpload for upload data to HDFS.
 * @author Laurent Jourdren
 */
public class HDFSDataUploadStep extends DataUploadStep {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private Configuration conf;

  @Override
  public String getDescription() {

    return "Upload data to HDFS filesystem";
  }

  //
  // Overriding methods
  //

  /**
   * This method define the default uploader for file to HDFS.
   * @param src source data source
   * @param filename output filename
   * @return a FileUploader object
   * @throws IOException if an error occurs while uploading data
   */
  private FileUploader getUploader(final String src, final String filename)
      throws IOException {

    return getUploader(new HDFSFileUploader(this.conf), src, filename);
  }

  /**
   * This method define the default uploader for file to HDFS.
   * @param FileUploader FileUploader to use
   * @param src source data source
   * @param filename output filename
   * @return a FileUploader object
   * @throws IOException if an error occurs while uploading data
   */
  private FileUploader getUploader(final FileUploader uploader,
      final String src, final String filename) {

    final String outputFilename;
    final String ext = StringUtils.extension(filename);

    if (Common.GZIP_EXTENSION.equals(ext) || Common.BZIP2_EXTENSION.equals(ext))
      outputFilename =
          StringUtils.filenameWithoutCompressionExtension(filename);
    else
      outputFilename = filename;

    uploader.init(DataSourceUtils.identifyDataSource(src), getDestURI()
        + "/" + outputFilename);

    return uploader;
  }

  //
  // Overriding methods
  //

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    this.conf = CommonHadoop.createConfiguration(globalParameters);

    if (conf == null)
      throw new NullPointerException("Configuration is null.");

    super.configure(stepParameters, globalParameters);
  }

  @Override
  protected FileUploader getDesignUploader(String src, String filename)
      throws IOException {

    return getUploader(src, filename);
  }

  @Override
  protected FileUploader getParameterUploader(String src, String filename)
      throws IOException {

    return getUploader(src, filename);
  }

  @Override
  protected FileUploader getFastaUploader(String src, String filename)
      throws IOException {

    if (!this.uploadGemome)
      return new FakeFileUploader(src, getDestURI() + "/" + filename);

    return getUploader(src, filename);
  }

  @Override
  protected FileUploader getFastqUploader(String src, String filename)
      throws IOException {

    final String s = StringUtils.filenameWithoutCompressionExtension(filename);
    final String out =
        StringUtils.filenameWithoutExtension(s) + Common.TFQ_EXTENSION;

    return getUploader(src, out);
  }

  @Override
  protected FileUploader getGFFUploader(String src, String filename)
      throws IOException {

    return getUploader(src, filename);
  }

  @Override
  protected FileUploader getIndexUploader(final String src,
      final String filename) throws IOException {

    final Path genomePath;

    if (src.contains("://")) {
      genomePath = new Path(src);
    } else {
      genomePath = new Path(new File(src).getAbsoluteFile().toURI().toString());
    }

    final Path indexPath = new Path(genomePath.getParent(), filename);

    final FileSystem indexFs = indexPath.getFileSystem(this.conf);

    if (indexFs.exists(indexPath))
      return getUploader(indexPath.toString(), filename);

    final FileSystem genomeFs = genomePath.getFileSystem(this.conf);

    final File indexFile;

    if ("file:///".equals(genomeFs.getUri().toString())) {

      final File tmpIndexFile =
          SOAPWrapper.makeIndexInZipFile(new File(genomePath.toUri()));

      // Copy index file to genome directory
      indexFile = new File(new Path(genomePath.getParent(), filename).toUri());
      FileUtils.moveFile(tmpIndexFile, indexFile);
    } else
      indexFile = SOAPWrapper.makeIndexInZipFile(genomeFs.open(genomePath));

    return getUploader(new Path(indexFile.toString()).toString(), filename);

  }

  protected void uploadFiles(final List<FileUploader> files) throws IOException {

    final Map<String, String> originalDistCpEntries =
        new HashMap<String, String>();

    for (FileUploader f : files) {

      f.prepare();
      f.upload();

      if (f instanceof HDFSFileUploader)
        originalDistCpEntries.putAll(((HDFSFileUploader) f).getDistCpEntries());
    }

    // If entries to copy
    if (originalDistCpEntries.size() > 0) {

      final Map<String, String> dataSourceDistCpEntries =
          new HashMap<String, String>();
      final Map<String, String> hadoopdistCpEntries =
          new HashMap<String, String>();

      // Select files to copy with DataSourceDistCp
      for (Map.Entry<String, String> e : originalDistCpEntries.entrySet()) {

        // final Path src = new Path(e.getKey());
        // final Path dest = new Path(e.getValue());

        // if (src.getName().equals(dest.getName())) {
        // hadoopdistCpEntries.put(e.getKey(), e.getValue());
        // } else
        dataSourceDistCpEntries.put(e.getKey(), e.getValue());
      }

      // Copy files with DataSourceDistCp
      final DataSourceDistCp cp =
          new DataSourceDistCp(this.conf, new Path(getDestURI().toString()));
      cp.copy(dataSourceDistCpEntries);

      // Copy files with HadoopDistCp
      hadoopDistCp(hadoopdistCpEntries);
    }

  }

  private void hadoopDistCp(Map<String, String> entries) throws IOException {

    if (entries == null || entries.size() == 0)
      return;

    // Create distcp object
    DistCp distcp = new DistCp(this.conf);

    // Reverse map
    Map<String, Set<String>> reverseEntries = Utils.reverseMap(entries);

    // Prepare distcp arguments to copy together files witch dest is the same
    for (Map.Entry<String, Set<String>> e : reverseEntries.entrySet()) {

      final String dest = e.getKey();
      final Set<String> sources = e.getValue();

      String[] args = new String[sources.size() + 1];
      int i = 0;
      for (String src : sources)
        args[i++] = src;

      args[i] = dest;

      // Copy the entries
      logger.info("Hadoop DistCp arguments: " + Arrays.toString(args));
      if (distcp.run(args) != 0)
        throw new IOException("Unable to copy file with distcp.");
    }
  }

  @Override
  protected void writeLog(final URI destURI, final long startTime,
      final String msg) throws IOException {

    CommonHadoop.writeLog(new Path(destURI.toString() + "/upload.log"),
        startTime, msg);

  }

}
