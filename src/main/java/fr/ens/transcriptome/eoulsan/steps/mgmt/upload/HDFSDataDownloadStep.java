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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.DistCp;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

@HadoopOnly
public class HDFSDataDownloadStep extends AbstractStep {

  // Logger
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /**
   * Key in the settings to use to save the list of DataFormat of the files to
   * download.
   */
  public static final String DATAFORMATS_TO_DOWNLOAD_SETTING =
      "dataformat.to.download";

  /** Step name. */
  public static final String STEP_NAME = "_download";

  private Configuration conf;

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "Download output data from HDFS filesystem";
  }

  @Override
  public String getLogName() {

    return "download";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    this.conf = CommonHadoop.createConfiguration(globalParameters);
  }

  @Override
  public StepResult execute(Design design, Context context) {

    LOGGER.info("Start copying results.");
    LOGGER.info("inpath="
        + context.getBasePathname() + "\toutpath="
        + context.getOutputPathname());

    final long startTime = System.currentTimeMillis();
    final Configuration conf = this.conf;

    if (context.getBasePathname() == null)
      throw new NullPointerException("The input path is null");

    if (context.getOutputPathname() == null)
      throw new NullPointerException("The output path is null");

    // Set the output directory
    final DataFile outputDir = new DataFile(context.getOutputPathname());

    try {

      final Path inPath = new Path(context.getBasePathname());

      if (!PathUtils.isExistingDirectoryFile(inPath, conf))
        throw new EoulsanException("The base directory is not a directory: "
            + inPath);

      // Get the list of DataFormat to download
      final List<DataFormat> formats = getDataFormats(context);

      // Add files to download
      final Map<DataFile, DataFile> files = Maps.newHashMap();
      for (Sample sample : design.getSamples()) {

        // If no DataFormat set, add all generated data
        if (formats.size() > 0) {
          for (DataFormat df : formats) {
            addFile(context, df, sample, outputDir, files);
          }
        } else {
          for (DataFormat df : context.getWorkflow().getGlobalOutputDataFormat(
              sample)) {
            addFile(context, df, sample, outputDir, files);
          }
        }
      }

      // If no file to copy, do nothing
      if (files.size() > 0) {

        if (outputDir.isDefaultProtocol()) {

          // Local FileSystem output
          for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {

            // Copy the file
            LOGGER.info("Copy " + e.getKey() + " to " + e.getValue());
            new DataFormatConverter(e.getKey(), e.getValue()).convert();
          }

        } else {

          // Use distributed copy if output is not on local FileSystem
          final Map<DataFile, DataFile> filesToTranscode = Maps.newHashMap();
          final Map<DataFile, DataFile> filesToDistCp = Maps.newHashMap();

          // Test if temporary file is needed
          for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {

            final DataFile src = e.getKey();
            final DataFile dest = e.getValue();

            if (src.getName().equals(dest.getName())) {
              filesToDistCp.put(src, dest);
            } else {
              final DataFile tmp =
                  new DataFile(src.getParent(), dest.getName());
              filesToTranscode.put(src, tmp);
              filesToDistCp.put(tmp, dest);
            }
          }

          // Create temporary files
          final Path jobPath =
              PathUtils.createTempPath(new Path(context.getBasePathname()),
                  "distcp-", "", this.conf);

          DataSourceDistCp dsdcp = new DataSourceDistCp(this.conf, jobPath);
          dsdcp.copy(filesToTranscode);

          // Copy files to destination
          hadoopDistCp(conf, filesToDistCp);
        }
      }

      final StringBuilder logMsg = new StringBuilder();
      for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {
        logMsg.append("Copy ");
        logMsg.append(e.getKey());
        logMsg.append(" to ");
        logMsg.append(e.getValue());
        logMsg.append('\n');
      }

      return new StepResult(context, startTime, logMsg.toString());

    } catch (EoulsanException e) {

      return new StepResult(context, e, "Error while download results: "
          + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "Error while download results: "
          + e.getMessage());
    }
  }

  /**
   * Add a file to the the list of file to download
   * @param context current context
   * @param df DataFormat of the file to download
   * @param sample current sample
   * @param outputDir output directory
   * @param files map of the files to download
   */
  private void addFile(final Context context, final DataFormat df,
      final Sample sample, final DataFile outputDir,
      final Map<DataFile, DataFile> files) {

    if (context == null || df == null || sample == null) {
      return;
    }

    final DataFile inFile = context.getDataFile(df, sample);

    if (!inFile.exists()) {
      return;
    }

    final DataFile outFile =
        new DataFile(outputDir, inFile.getName()
            + CompressionType.BZIP2.getExtension());

    files.put(inFile, outFile);
  }

  /**
   * Get the list of DataFormat of the files to download
   * @param context current context
   * @return a list with the DataFormat of the files to download
   */
  private List<DataFormat> getDataFormats(final Context context) {

    final List<DataFormat> result = Lists.newArrayList();

    final String list =
        context.getRuntime().getSettings().getSetting(
            DATAFORMATS_TO_DOWNLOAD_SETTING);

    if (list == null) {
      return result;
    }

    final String[] fields = list.split(",");

    if (fields == null) {
      return result;
    }

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (String dataFormatName : fields) {

      final DataFormat df = registry.getDataFormatFromName(dataFormatName);
      if (df != null) {
        result.add(df);
      }
    }

    return result;
  }

  /**
   * Copy files using hadoop DistCp.
   * @param conf Hadoop configuration
   * @param files files to copy
   * @throws EoulsanException if an error occurs while copying
   */
  private void hadoopDistCp(final Configuration conf,
      final Map<DataFile, DataFile> files) throws EoulsanException {

    final DistCp distcp = new DistCp(conf);
    final Map<DataFile, Set<DataFile>> toCopy = Maps.newHashMap();

    // Create a map of file to copy with destination directory as key
    for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {

      final DataFile destDir = e.getValue().getParent();

      final Set<DataFile> inputFiles;

      if (toCopy.containsKey(destDir)) {
        inputFiles = toCopy.get(destDir);
      } else {
        inputFiles = Sets.newHashSet();
        toCopy.put(destDir, inputFiles);
      }

      inputFiles.add(e.getKey());
    }

    // For each desitination run distcp
    for (Map.Entry<DataFile, Set<DataFile>> e : toCopy.entrySet()) {

      final List<String> argsList = Lists.newArrayList();

      // Add input files
      for (DataFile f : e.getValue())
        argsList.add(f.toString());

      // Add destination
      argsList.add(e.getKey().toString());

      // Convert arguments in a n array
      final String[] args = argsList.toArray(new String[0]);

      // Run distcp
      distcp.runWithException(args);
    }

  }

}
