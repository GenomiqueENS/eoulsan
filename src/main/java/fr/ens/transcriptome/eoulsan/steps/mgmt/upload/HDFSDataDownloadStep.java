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
            addFile(context, df, sample, files);
          }
        } else {
          for (DataFormat df : context.getWorkflow().getGlobalOutputDataFormat(
              sample)) {
            addFile(context, df, sample, files);
          }
        }
      }

      // If no file to copy, do nothing
      if (files.size() > 0) {

        if (new DataFile(context.getOutputPathname()).isDefaultProtocol()) {

          // Local FileSystem output
          for (Map.Entry<DataFile, DataFile> e : files.entrySet()) {

            // Copy the file
            new DataFormatConverter(e.getKey(), e.getValue()).convert();
          }

        } else {

          // Use distributed copy if output is not on local FileSystem
          final Path jobPath =
              PathUtils.createTempPath(new Path(context.getBasePathname()),
                  "distcp-", "", this.conf);

          DataSourceDistCp distCp = new DataSourceDistCp(this.conf, jobPath);
          distCp.copy(files);
        }
      }

      /*
       * final FileSystem inFs = inPath.getFileSystem(conf); final FileSystem
       * outFs = outPath.getFileSystem(conf); final FileStatus[] files =
       * inFs.listStatus(inPath, new PathFilter() {
       * @Override public boolean accept(final Path p) { final String filename =
       * p.getName(); final DataFormat df = DataFormats.EXPRESSION_RESULTS_TXT;
       * if (filename.startsWith(df.getType().getPrefix()) &&
       * filename.endsWith(df.getDefaultExtention())) return true; return false;
       * } }); // Create output path is does not exists if
       * (!outFs.exists(outPath)) outFs.mkdirs(outPath); final StringBuilder
       * logMsg = new StringBuilder(); if (files != null) for (FileStatus f :
       * files) { final Path ip = f.getPath(); final Path op = new Path(outPath,
       * ip.getName()); String msg = "Copy " + ip + " to " + op;
       * logger.info(msg); logMsg.append(msg); logMsg.append("\n");
       * PathUtils.copy(ip, op, conf); }
       */

      return new StepResult(context, startTime, files.keySet().toString());

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
   * @param files map of the files to download
   */
  private void addFile(final Context context, final DataFormat df,
      final Sample sample, final Map<DataFile, DataFile> files) {

    if (context == null || df == null || sample == null) {
      return;
    }

    final DataFile inFile = context.getDataFile(df, sample);

    if (!inFile.exists()) {
      return;
    }

    final DataFile outFile =
        new DataFile(new Path(new Path(context.getOutputPathname()), inFile
            .getName()
            + CompressionType.BZIP2.getExtension()).toString());

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

      final DataFormat df = registry.getDataFormat(dataFormatName);
      if (df != null) {
        result.add(df);
      }
    }

    return result;
  }

}
