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

package fr.ens.biologie.genomique.eoulsan.modules.mgmt.upload;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.annotations.Terminal;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.core.workflow.AbstractWorkflow;
import fr.ens.biologie.genomique.eoulsan.core.workflow.StepOutputDataFile;
import fr.ens.biologie.genomique.eoulsan.core.workflow.WorkflowContext;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.io.DesignWriter;
import fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignWriter;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopJarRepackager;

/**
 * This class define a abstract module class for files uploading.
 * @since 1.0
 * @author Laurent Jourdren
 */
@Terminal
public abstract class UploadModule extends AbstractModule {

  private final DataFile dest;

  //
  // Getter
  //

  protected DataFile getDest() {

    return this.dest;
  }

  //
  // Module methods
  //

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final StringBuilder log = new StringBuilder();

    // Save and change base pathname
    final WorkflowContext fullContext =
        ((AbstractWorkflow) context.getWorkflow()).getWorkflowContext();

    final Map<DataFile, DataFile> filesToCopy = new HashMap<>();
    Path repackagedJarFile = null;

    try {
      final Design design = context.getWorkflow().getDesign();
      for (Sample sample : design.getSamples()) {
        filesToCopy.putAll(findDataFilesInWorkflow(sample, context));
      }

      removeNotExistingDataFile(filesToCopy);

      // Check if destination path already exists
      if (getDest().exists()) {
        throw new IOException(
            "The uploading destination already exists: " + getDest());
      }

      // Repackage the jar file if necessary
      if (!context.getRuntime().getMode().isHadoopMode()) {
        repackagedJarFile = HadoopJarRepackager.repack();
        final DataFile jarDataFile =
            new DataFile(repackagedJarFile.toAbsolutePath());
        filesToCopy.put(jarDataFile, getUploadedDataFile(jarDataFile));
      }

      final Settings settings = context.getRuntime().getSettings();

      // Add all files to upload in a map
      reWriteDesign(context, filesToCopy);

      // Obfuscate design is needed
      if (settings.isObfuscateDesign()) {
        DesignUtils.obfuscate(design,
            settings.isObfuscateDesignRemoveReplicateInfo());
      }

      // Create a new design file
      final File newDesignFile = writeTempDesignFile(context, design);
      final DataFile uploadedDesignDataFile =
          getUploadedDataFile(context.getDesignFile());
      filesToCopy.put(new DataFile(newDesignFile.getAbsolutePath()),
          uploadedDesignDataFile);

      // Add workflow file to the list of file to upload
      final DataFile currentParamDataFile = context.getWorkflowFile();
      final DataFile uploadedParamDataFile =
          getUploadedDataFile(currentParamDataFile);
      filesToCopy.put(currentParamDataFile, uploadedParamDataFile);

      // Create log entry
      for (Map.Entry<DataFile, DataFile> e : filesToCopy.entrySet()) {
        log.append("Copy ");
        log.append(e.getKey());
        log.append(" to ");
        log.append(e.getValue());
        log.append('\n');
      }

      // Copy the files
      copy(filesToCopy);

      // Remove temporary design file
      if (!newDesignFile.delete()) {
        getLogger()
            .warning("Cannot remove temporary design file: " + newDesignFile);
      }

      // Change the path of design and workflow file in the context
      fullContext
          .setDesignFile(new DataFile(uploadedDesignDataFile.getSource()));
      fullContext
          .setWorkflowFile(new DataFile(uploadedParamDataFile.getSource()));

    } catch (IOException e) {

      return status.createTaskResult(e);
    }

    // The base path is now the place where files where uploaded.
    // TODO Warning, the context.setBasePathname() no more exist
    // Upload step must be rewritten or replace by something better
    // fullContext.setBasePathname(getDest().toString());

    // The path to the jar file
    if (!context.getRuntime().getMode().isHadoopMode()) {
      fullContext.setJarFile(new DataFile(
          getDest().toString() + "/" + repackagedJarFile.getFileName()));
    }

    status.setProgressMessage(log.toString());
    return status.createTaskResult();
  }

  @Override
  public String getName() {

    return "upload";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  //
  // Abstract methods
  //

  /**
   * Generate the DataFile Object for the uploaded DataFile
   * @param file DataFile to upload
   * @return a new DataFile object with the path to the upload DataFile
   * @throws IOException if an error occurs while creating the result DataFile
   */
  protected abstract DataFile getUploadedDataFile(final DataFile file)
      throws IOException;

  /**
   * Generate the DataFile Object for the uploaded DataFile
   * @param file DataFile to upload
   * @param step step that create the data
   * @param format the format of the file to upload
   * @param portName the port name
   * @param sample the sample for the source
   * @return a new DataFile object with the path to the upload DataFile
   * @throws IOException if an error occurs while creating the result DataFile
   */
  private DataFile getUploadedDataFile(final DataFile file, final Step step,
      final Sample sample, final String portName, final DataFormat format)
      throws IOException {

    return getUploadedDataFile(file, step, sample, portName, format, -1);
  }

  /**
   * Generate the DataFile Object for the uploaded DataFile
   * @param file DataFile to upload
   * @param portName the port name
   * @param format the format of the file to upload
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a new DataFile object with the path to the upload DataFile
   * @throws IOException if an error occurs while creating the result DataFile
   */
  protected abstract DataFile getUploadedDataFile(final DataFile file,
      final Step step, final Sample sample, final String portName,
      final DataFormat format, final int fileIndex) throws IOException;

  /**
   * Copy files to destinations.
   * @param files map with source and destination for each file
   * @throws IOException if an error occurs while copying files
   */
  protected abstract void copy(Map<DataFile, DataFile> files)
      throws IOException;

  //
  // Other methods
  //

  /**
   * Find DataFiles used by the steps of a Workflow for a sample
   * @param sample sample
   * @param context Execution context
   * @return a set of DataFile used by the workflow for the sample
   * @throws IOException if an error occurs while finding files
   */
  private Map<DataFile, DataFile> findDataFilesInWorkflow(final Sample sample,
      final TaskContext context) throws IOException {

    final Map<DataFile, DataFile> result = new HashMap<>();

    Set<StepOutputDataFile> inFiles =
        // context.getWorkflow().getWorkflowFilesAtFirstStep().getInputFiles();
        new HashSet<>();

    for (StepOutputDataFile file : inFiles) {
      final DataFile in = file.getDataFile();
      final DataFile out =
          getUploadedDataFile(in, file.getStep(), file.getSample(),
              file.getPortName(), file.getFormat(), file.getFileIndex());
      result.put(in, out);
    }

    return result;
  }

  /**
   * Remove the DataFiles that not exists in a set of DataFiles.
   * @param files Set of DataFile to filter
   */
  private void removeNotExistingDataFile(final Map<DataFile, DataFile> files) {

    Set<DataFile> filesToRemove = new HashSet<>();

    for (DataFile file : files.keySet()) {
      if (!file.exists()) {
        filesToRemove.add(file);
      }
    }

    for (DataFile file : filesToRemove) {
      files.remove(file);
    }
  }

  private void reWriteDesign(final TaskContext context,
      final Map<DataFile, DataFile> filesToCopy) throws IOException {

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    final Step designStep = context.getWorkflow().getDesignStep();
    final Design design = context.getWorkflow().getDesign();

    final Set<String> fieldWithFiles = new HashSet<>();
    boolean first = true;
    for (final Sample s : design.getSamples()) {

      if (first) {
        for (String fieldName : s.getMetadata().keySet()) {
          if (registry.getDataFormatForDesignMetadata(fieldName) != null) {
            fieldWithFiles.add(fieldName);
          }
        }
        first = false;
      }

      for (final String field : fieldWithFiles) {

        final List<String> oldValues = s.getMetadata().getAsList(field);
        final List<String> newValues = new ArrayList<>();

        final int nValues = oldValues.size();

        if (nValues == 1) {
          final DataFile inFile = new DataFile(oldValues.get(0));
          // final DataFormat format = inFile.getDataFormat();

          Set<DataFormat> formats =
              registry.getDataFormatsFromExtension(inFile.getExtension());

          final DataFormat format;

          // Not very pretty
          if (formats.size() == 1) {
            format = formats.iterator().next();
          } else {
            format = inFile.getMetaData().getDataFormat();
          }

          final DataFile outFile;

          if (format.getMaxFilesCount() == 1) {
            outFile = getUploadedDataFile(inFile, designStep, s,
                format.getName(), format);
          } else {
            outFile = getUploadedDataFile(inFile, designStep, s,
                format.getName(), format, 0);
          }

          filesToCopy.put(inFile, outFile);
          newValues.add(outFile.toString());

        } else if (nValues > 1) {

          for (int i = 0; i < nValues; i++) {
            final DataFile inFile = new DataFile(oldValues.get(i));
            final DataFormat format = inFile.getDataFormat();
            final DataFile outFile = getUploadedDataFile(inFile, designStep, s,
                format.getName(), format, i);
            filesToCopy.put(inFile, outFile);
            newValues.add(outFile.toString());
          }
        }

        // Replace old paths with new path in design
        s.getMetadata().set(field, newValues);
      }

    }

  }

  /**
   * Write temporary design file
   * @param context context object
   * @param design Design object
   * @return the temporary design file
   * @throws IOException if an error occurs while writing the design file
   */
  private File writeTempDesignFile(final TaskContext context,
      final Design design) throws IOException {

    final File result = context.getRuntime().createTempFile("design-", ".txt");

    DesignWriter writer =
        new Eoulsan1DesignWriter(FileUtils.createOutputStream(result));
    writer.write(design);

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param destination destination of the uploaded files
   */
  public UploadModule(final DataFile destination) {

    if (destination == null) {
      throw new NullPointerException("The destination file is null.");
    }

    this.dest = destination;

  }
}
