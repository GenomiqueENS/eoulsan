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

package fr.ens.transcriptome.eoulsan.steps.mgmt.newupload;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.SimpleContext;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.HadoopJarRepackager;

/**
 * This class define a abstract step class for files uploading.
 * @author Laurent Jourdren
 */
public abstract class UploadStep extends AbstractStep {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private DataFile dest;

  //
  // Getter
  //

  protected DataFile getDest() {

    return this.dest;
  }

  //
  // Step methods
  //

  @Override
  public StepResult execute(final Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    // Save and change base pathname
    final SimpleContext fullContext = (SimpleContext) context;

    final Set<DataFile> files = new HashSet<DataFile>();

    for (Sample sample : design.getSamples())
      files.addAll(findDataFiles(sample, context));

    removeNotExistingDataFile(files);

    File repackagedJarFile = null;

    try {

      // Repackage the jar file if necessary
      if (!context.getRuntime().isHadoopMode()) {
        repackagedJarFile = HadoopJarRepackager.repack();
        files.add(new DataFile(repackagedJarFile.getAbsolutePath()));
      }

      // Add all files to upload in a map
      final Map<DataFile, DataFile> filesToCopy = reWriteDesign(design, files);

      // Create a new design file
      final File newDesignFile = writeTempDesignFile(design);
      final DataFile uploadedDesignDataFile =
          getUploadedDataFile(new DataFile(context.getDesignPathname()));
      filesToCopy.put(new DataFile(newDesignFile.getAbsolutePath()),
          uploadedDesignDataFile);

      // Add parameter file to the list of file to upload
      final DataFile currentParamDataFile =
          new DataFile(context.getParameterPathname());
      final DataFile uploadedParamDataFile =
          getUploadedDataFile(currentParamDataFile);
      filesToCopy.put(currentParamDataFile, uploadedParamDataFile);

      // Copy the file
      copy(filesToCopy);

      // Remove temporary design file
      if (!newDesignFile.delete()) {
        LOGGER.warning("Cannot remove temporary desihn file: " + newDesignFile);
      }

      // Change the path of design and parameter file in the context
      fullContext.setDesignPathname(uploadedDesignDataFile.getSource());
      fullContext.setParameterPathname(uploadedParamDataFile.getSource());

    } catch (IOException e) {

      return new StepResult(this, e);
    } catch (EoulsanIOException e) {

      return new StepResult(this, e);
    }

    // The base path is now the place where files where uploaded.
    fullContext.setBasePathname(getDest().toString());

    // The path to the jar file
    if (!context.getRuntime().isHadoopMode()) {
      fullContext.setJarPathname(getDest().toString()
          + "/" + repackagedJarFile.getName());
    }

    return new StepResult(this, startTime, files.toString());
  }

  @Override
  public String getName() {

    return "Upload";
  }

  @Override
  public boolean isTerminalStep() {

    return false;
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
  private DataFile getUploadedDataFile(final DataFile file) throws IOException {

    return getUploadedDataFile(file, -1);
  }

  /**
   * Generate the DataFile Object for the uploaded DataFile
   * @param file DataFile to upload
   * @return a new DataFile object with the path to the upload DataFile
   * @throws IOException if an error occurs while creating the result DataFile
   */
  protected abstract DataFile getUploadedDataFile(final DataFile file,
      final int id) throws IOException;

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
   */
  private Set<DataFile> findDataFiles(Sample sample, final Context context) {

    boolean afterThis = false;

    final Set<DataFile> result = new HashSet<DataFile>();

    for (Step s : context.getWorkflow().getSteps()) {

      if (afterThis) {

        DataFormat[] formats = s.getInputFormats();
        if (formats != null)

          for (DataFormat df : formats)
            result.add(context.getDataFile(df, sample));

      } else if (s == this)
        afterThis = true;

    }

    return result;
  }

  /**
   * Remove the DataFiles that not exists in a set of DataFiles.
   * @param files Set of DataFile to filter
   */
  private void removeNotExistingDataFile(Set<DataFile> files) {

    Set<DataFile> filesToRemove = new HashSet<DataFile>();

    for (DataFile file : files)
      if (!file.exists())
        filesToRemove.add(file);

    files.removeAll(filesToRemove);
  }

  private Map<DataFile, DataFile> reWriteDesign(final Design design,
      final Set<DataFile> filesToCopy) throws IOException {

    final Map<DataFile, DataFile> result = new HashMap<DataFile, DataFile>();

    final Map<String, String> genomesMap = new HashMap<String, String>();
    final Map<String, String> annotationsMap = new HashMap<String, String>();

    int genomesCount = 0;
    int annotationsCount = 0;

    for (Sample s : design.getSamples()) {

      // Copy the sample
      DataFile sampleOldFile = new DataFile(s.getSource());
      DataFile sampleNewFile = getUploadedDataFile(sampleOldFile, s.getId());

      if (filesToCopy.contains(sampleOldFile)) {

        filesToCopy.remove(sampleOldFile);
        result.put(sampleOldFile, sampleNewFile);
      }

      s.setSource(sampleNewFile.getSource());

      // copy the genome file
      final String genome = s.getMetadata().getGenome();

      if (!genomesMap.containsKey(genome)) {
        genomesCount++;

        // Add genome file
        DataFile genomeOldFile = new DataFile(genome);
        DataFile genomeNewFile =
            getUploadedDataFile(genomeOldFile, genomesCount);

        if (filesToCopy.contains(genomeOldFile)) {

          filesToCopy.remove(genomeOldFile);
          result.put(genomeOldFile, genomeNewFile);
        }

        genomesMap.put(genome, genomeNewFile.getSource());
      }
      s.getMetadata().setGenome(genomesMap.get(genome));

      // Copy the annotation
      final String annotation = s.getMetadata().getAnnotation();

      if (!annotationsMap.containsKey(annotation)) {
        annotationsCount++;

        // Add annotation file
        DataFile annotationOldFile = new DataFile(annotation);
        DataFile annotationNewFile =
            getUploadedDataFile(annotationOldFile, annotationsCount);

        if (filesToCopy.contains(annotationOldFile)) {

          filesToCopy.remove(annotationOldFile);
          result.put(annotationOldFile, annotationNewFile);
        }

        annotationsMap.put(annotation, annotationNewFile.getSource());
      }
      s.getMetadata().setAnnotation(annotationsMap.get(annotation));

    }

    for (DataFile file : filesToCopy)
      result.put(file, getUploadedDataFile(file));

    return result;
  }

  /**
   * Write temporary design file
   * @param design Design object
   * @return the temporary design file
   * @throws EoulsanIOException if an error occurs while writing the design file
   * @throws IOException if an error occurs while writing the design file
   */
  private File writeTempDesignFile(final Design design)
      throws EoulsanIOException, IOException {

    final File result = FileUtils.createTempFile("design-", ".txt");

    DesignWriter writer =
        new SimpleDesignWriter(FileUtils.createOutputStream(result));
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
  public UploadStep(final DataFile destination) {

    if (destination == null)
      throw new NullPointerException("The destination file is null.");

    this.dest = destination;

  }
}
