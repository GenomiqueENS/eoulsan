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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * this class class allow to upload design file and its data to an HDFS or S3
 * partition.
 * @author Laurent Jourdren
 */
public abstract class DataUploadStep implements Step {

  /** Step name. */
  public static final String STEP_NAME = "_upload";

  protected boolean uploadGemome = false;

  private String designURI;
  private String paramURI;
  private String destURI;

  //
  // Getters
  //

  /**
   * Get the design URI
   * @return Returns the designURI
   */
  protected String getDesignURI() {

    return this.designURI;
  }

  /**
   * Get the parameter URI
   * @return Returns the paramURI
   */
  protected String getParamURI() {

    return this.paramURI;
  }

  /**
   * Get the parameter URI
   * @return Returns the paramURI
   */
  protected String getDestURI() {

    return this.destURI;
  }

  //
  // Setters
  //

  /**
   * Set the designURI
   * @param designURI The designURI to set
   */
  private void setDesignURI(final String designURI) {

    this.designURI = designURI;
  }

  /**
   * Set the parameter URI
   * @param paramURI The paramURI to set
   */
  private void setParamURI(final String paramURI) {

    this.paramURI = paramURI;
  }

  /**
   * Set the destination URI
   * @param paramURI The paramURI to set
   */
  private void setDestURI(final String destURI) {

    this.destURI = destURI;
  }

  //
  // Abstract methods
  //

  /**
   * Get the uploader for fastq data.
   * @param src source of the data
   * @param outputFilename output filename
   * @throws IOExeception if the an error occurs while creating the uploader
   */
  protected abstract FileUploader getFastqUploader(final String src,
      final String outputFilename) throws IOException;

  /**
   * Get the uploader for fasta data.
   * @param src source of the data
   * @param outputFilename output filename
   * @throws IOExeception if the an error occurs while creating the uploader
   */
  protected abstract FileUploader getFastaUploader(final String src,
      final String outputFilename) throws IOException;

  /**
   * Get the uploader for the genome index data.
   * @param src source of the data
   * @param outputFilename output filename
   * @throws IOExeception if the an error occurs while creating the uploader
   */
  protected abstract FileUploader getIndexUploader(final String src,
      final String outputFilename) throws IOException;

  /**
   * Get the uploader for the gff file data.
   * @param src source of the data
   * @param outputFilename output filename
   * @throws IOExeception if the an error occurs while creating the uploader
   */
  protected abstract FileUploader getGFFUploader(final String src,
      final String outputFilename) throws IOException;

  /**
   * Get the uploader for the design file data.
   * @param src source of the data
   * @param outputFilename output filename
   * @throws IOExeception if the an error occurs while creating the uploader
   */
  protected abstract FileUploader getDesignUploader(final String src,
      final String outputFilename) throws IOException;

  /**
   * Get the uploader for the parameter file data.
   * @param src source of the data
   * @param outputFilename output filename
   * @throws IOExeception if the an error occurs while creating the uploader
   */
  protected abstract FileUploader getParameterUploader(final String src,
      final String outputFilename) throws IOException;

  protected abstract void writeLog(final URI destURI, final long startTime,
      final String msg) throws IOException;

  protected abstract void uploadFiles(List<FileUploader> files)
      throws IOException;

  //
  // Step methods
  //

  @Override
  public String getLogName() {

    return "upload";
  }

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public DataType[] getInputTypes() {
    return new DataType[] {};
  }

  @Override
  public DataType[] getOutputType() {
    return new DataType[] {};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

  }

  /**
   * Upload data.
   */
  // protected void upload(final URI paramURI, final Design design)
  // throws IOException, EoulsanIOException {
  @Override
  public StepResult execute(Design design, final ExecutorInfo info) {

    final List<FileUploader> files = new ArrayList<FileUploader>();
    final long startTime = System.currentTimeMillis();

    setDesignURI(info.getDesignPathname());
    setParamURI(info.getParameterPathname());
    setDestURI(info.getBasePathname());

    // Create output dir

    final Map<String, String> genomesMap = new HashMap<String, String>();
    final Map<String, String> annotationsMap = new HashMap<String, String>();

    int genomesCount = 0;
    int annotationsCount = 0;

    try {
      for (Sample s : design.getSamples()) {

        // Copy the sample
        final String sampleNewFilename =
            CommonHadoop.SAMPLE_FILE_PREFIX
                + s.getId() + Common.FASTQ_EXTENSION;
        final FileUploader sampleFU =
            getFastqUploader(s.getSource(), sampleNewFilename);
        files.add(sampleFU);
        s.setSource(sampleFU.getDest());

        // copy the genome file
        final String genome = s.getMetadata().getGenome();

        if (!genomesMap.containsKey(genome)) {
          genomesCount++;

          // Add genome file
          final String genomeNewFilename =
              CommonHadoop.GENOME_FILE_PREFIX
                  + genomesCount + Common.FASTA_EXTENSION;
          final FileUploader genomeFU =
              getFastaUploader(genome, genomeNewFilename);
          files.add(genomeFU);

          // Create and add genome index file
          final String genomeIndexNewFilename =
              CommonHadoop.GENOME_SOAP_INDEX_FILE_PREFIX
                  + genomesCount + CommonHadoop.GENOME_SOAP_INDEX_FILE_SUFFIX;
          files.add(getIndexUploader(genome, genomeIndexNewFilename));

          // final File indexFile = SOAPWrapper.makeIndexInZipFile(new
          // File(genome));
          // final String genomeIndexNewFilename =
          // CommonHadoop.GENOME_SOAP_INDEX_FILE_PREFIX
          // + genomesCount + CommonHadoop.GENOME_SOAP_INDEX_FILE_SUFFIX;
          // files.add(getIndexUploader(indexFile.getAbsolutePath(),
          // genomeIndexNewFilename));

          // indexFile.delete();

          genomesMap.put(genome, genomeFU.getDest());
        }
        s.getMetadata().setGenome(genomesMap.get(genome));

        // Copy the annotation
        final String annotation = s.getMetadata().getAnnotation();

        if (!annotationsMap.containsKey(annotation)) {
          annotationsCount++;

          // Add annotation file
          final String newAnnotationFilename =
              CommonHadoop.ANNOTATION_FILE_PREFIX
                  + annotationsCount + Common.GFF_EXTENSION;
          final FileUploader annotationFU =
              getGFFUploader(annotation, newAnnotationFilename);
          files.add(annotationFU);

          annotationsMap.put(annotation, annotationFU.getDest());
        }
        s.getMetadata().setAnnotation(annotationsMap.get(annotation));

      }

      // Create a new design file
      final FileUploader designUploader = getDesignUploader(null, "design.txt");
      final SimpleDesignWriter sdw =
          new SimpleDesignWriter(designUploader.createUploadOutputStream());
      sdw.write(design);
      files.add(designUploader);

      // Add parameter file
      // final FileUploader paramUploader =
      // getParameterUploader(null, new File(paramURL.getFile()).getName());
      // FileUtils.copy(paramURL.openStream(), paramUploader
      // .createUploadOutputStream());
      // final FileUploader paramUploader = getParameterUploader(null, new
      // File(paramURL.getFile()).getName());
      final FileUploader paramUploader =
          getParameterUploader(getParamURI(), StringUtils
              .getURIFilename(getParamURI()));

      files.add(paramUploader);

      // Upload all the files
      uploadFiles(files);

      // Create log message

      final StringBuilder sb = new StringBuilder();
      sb.append("paramURI: "
          + paramURI + "\ndesignURI: " + getDesignURI() + "\ndestURI: "
          + info.getBasePathname() + "\n");

      for (FileUploader f : files) {
        sb.append("Upload ");
        sb.append(f.getSrc());
        sb.append(" to ");
        sb.append(f.getDest());
        sb.append("\n");
      }

      // Write log
      return new StepResult(this, startTime, sb.toString());

    } catch (IOException e) {

      return new StepResult(this, e, "Error while uploading data: "
          + e.getMessage());

    } catch (EoulsanIOException e) {

      return new StepResult(this, e, "Error while uploading data: "
          + e.getMessage());
    }
  }

  /**
   * Upload data.
   * @param paramURI parameter URI
   * @param designURI design URI
   * @param destURI destination URI
   * @throws EoulsanException if an error occurs while uploading data
   * @throws IOException if an error occurs while uploading data
   */
  // public abstract Design upload(final URI paramURI, final URI designURI,
  // final URI destURI) throws EoulsanException, IOException;

}
