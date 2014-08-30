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

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepOutputDataFile;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.DistCp;
import fr.ens.transcriptome.eoulsan.util.Version;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This class define a download step that retrieve data from HDFS at the end of
 * an analysis.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class HDFSDataDownloadStep extends AbstractStep {

  // Logger
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  /**
   * Key in the settings to use to save the list of DataFormat of the files to
   * download.
   */
  public static final String DATAFORMATS_TO_DOWNLOAD_SETTING =
      "dataformat.to.download";

  /**
   * Key in the settings to use to disable the downloads.
   */
  public static final String NO_HDFS_DOWNLOAD = "no.hdfs.download";

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
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Skip the step if the global parameter NO_HDFS_DOWNLOAD is set
    final String noDownloadValue =
        context.getSettings().getSetting(NO_HDFS_DOWNLOAD);
    if (noDownloadValue != null
        && "true".equals(noDownloadValue.trim().toLowerCase())) {

      status.setMessage("Download step skipped in settings.");
      return status.createStepResult();
    }

    LOGGER.info("Start copying results.");
    LOGGER.info("inpath="
        + context.getHadoopWorkingPathname() + "\toutpath="
        + context.getOutputPathname());

    final Configuration conf = this.conf;

    if (context.getHadoopWorkingPathname() == null)
      throw new NullPointerException("The input path is null");

    if (context.getOutputPathname() == null)
      throw new NullPointerException("The output path is null");

    // Set the output directory
    final DataFile outputDir = new DataFile(context.getOutputPathname());

    try {

      final Path inPath = new Path(context.getHadoopWorkingPathname());

      if (!PathUtils.isExistingDirectoryFile(inPath, conf))
        throw new EoulsanException("The base directory is not a directory: "
            + inPath);

      // Map with files to download
      final Map<DataFile, DataFile> files = Maps.newHashMap();

      // Get the output file of the workflow
      final Set<WorkflowStepOutputDataFile> outFiles =
          context.getWorkflow().getWorkflowFilesAtFirstStep().getOutputFiles();

      // Add the output files of the workflow to the list of files to downloads
      for (WorkflowStepOutputDataFile file : outFiles) {
        final DataFile in = file.getDataFile();

        final DataFile out =

            new DataFile(outputDir, in.getName()
                + CompressionType.BZIP2.getExtension());

        files.put(in, out);
      }

      // If no file to copy, do nothing
      if (files.size() > 0) {

        if (outputDir.isLocalFile()) {

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
              PathUtils.createTempPath(
                  new Path(context.getHadoopWorkingPathname()), "distcp-", "",
                  this.conf);

          final DataFileDistCp dsdcp = new DataFileDistCp(this.conf, jobPath);
          dsdcp.copy(filesToTranscode);

          // Remove job path directory
          final FileSystem fs = jobPath.getFileSystem(conf);
          if (!fs.delete(jobPath, true)) {
            LOGGER.warning("Cannot remove DataFileDistCp job path: " + jobPath);
          }

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

      status.setMessage(logMsg.toString());
      return status.createStepResult();

    } catch (EoulsanException e) {

      return status.createStepResult(e,
          "Error while download results: " + e.getMessage());
    } catch (IOException e) {

      return status.createStepResult(e,
          "Error while download results: " + e.getMessage());
    }
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

      final DataFile destDir;
      try {
        destDir = e.getValue().getParent();
      } catch (IOException exp) {
        throw new EoulsanException(exp.getMessage());
      }

      if (destDir == null)
        throw new EoulsanException("Destination directory is null.");

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
      final String[] args = argsList.toArray(new String[argsList.size()]);

      // Run distcp
      distcp.runWithException(args);
    }

  }

}
