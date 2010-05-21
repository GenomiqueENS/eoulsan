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

package fr.ens.transcriptome.eoulsan.programs.mgmt.hadoop;

import java.io.File;
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
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.datasources.DataSourceUtils;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class allow to copy data to hdfs.
 * @author Laurent Jourdren
 */
public class UploadDesignDataHadoopMain {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /**
   * Copy a DataSource to a Path
   * @param source source to copy
   * @param destPath destination path
   * @param conf Configuration object
   * @throws IOException if an error occurs while copying data
   */
  private static void copy(final String source, final Path destPath,
      final Configuration conf) throws IOException {

    logger.info("Copy " + source.toString() + " to " + destPath);
    final DataSource ds = DataSourceUtils.identifyDataSource(source);

    if ("File".equals(ds.getSourceType()))
      PathUtils.copyLocalFileToPath(new File(ds.toString()), destPath, conf);
    else
      PathUtils.copyInputStreamToPath(ds.getInputStream(), destPath, conf);
  }

  /***
   * Create a Path for ressources.
   * @param basePath base path for ressources
   * @param prefix prefix for ressources
   * @param id identifier of the ressource
   * @param extension extension of the ressource
   */
  private static Path createPath(final Path basePath, final String prefix,
      final int id, final String extension) {

    return new Path(basePath, prefix + id + extension);
  }

  /**
   * Write the design file on hdfs.
   * @param design Design object to write
   * @param destPath destination path
   * @param conf Configuration
   * @throws IOException if an error occurs while writing data
   * @throws EoulsanIOException if an error occurs while writing data
   */
  private static void writeNewDesign(final Design design, final Path destPath,
      final Configuration conf) throws IOException, EoulsanIOException {

    logger.info("Create new design file in " + destPath);
    final FileSystem fs = FileSystem.get(destPath.toUri(), conf);
    final OutputStream os = fs.create(destPath);
    final SimpleDesignWriter sdw = new SimpleDesignWriter(os);
    sdw.write(design);
  }

  //
  // Main method
  //

  /**
   * Main method.
   * @param args command line argument
   */
  public static void main(final String[] args) {

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length != 2)
      throw new IllegalArgumentException(
          "Expression need three or more arguments");

    final String designFilename = args[0];
    final String hadoopPathname = args[1];

    // Create configuration object
    final Configuration conf = new Configuration();

    final Path hadoopPath = new Path(hadoopPathname);

    try {

      if (PathUtils.isFile(hadoopPath, conf)
          || PathUtils.isExistingDirectoryFile(hadoopPath, conf))
        throw new IOException("The output path already exists.");

      PathUtils.mkdirs(hadoopPath, conf);

      final DesignReader dr = new SimpleDesignReader(designFilename);
      final Design design = dr.read();

      if (!DesignUtils.checkSamples(design)) {

        System.err
            .println("Error: The design contains one or more duplicate sample sources.");
        System.exit(1);
      }

      if (!DesignUtils.checkGenomes(design))
        System.err
            .println("Warning: The design contains more than one genome file.");

      if (!DesignUtils.checkAnnotations(design))
        System.err
            .println("Warning: The design contains more than one annotation file.");

      final Map<String, String> genomesMap = new HashMap<String, String>();
      final Map<String, String> annotationsMap = new HashMap<String, String>();

      int samplesCount = 0;
      int genomesCount = 0;
      int annotationsCount = 0;

      for (Sample s : design.getSamples()) {

        // Set the number of the sample
        samplesCount++;

        // Copy the sample
        final Path newSamplePath =
            createPath(hadoopPath, CommonHadoop.SAMPLE_FILE_PREFIX,
                samplesCount, Common.FASTQ_EXTENSION);
        copy(s.getSource(), newSamplePath, conf);

        s.setSource(newSamplePath.getName());

        // copy the genome file
        final String genome = s.getMetadata().getGenome();

        if (!genomesMap.containsKey(genome)) {
          genomesCount++;

          final Path newGenomePath =
              createPath(hadoopPath, CommonHadoop.GENOME_FILE_PREFIX,
                  genomesCount, Common.FASTA_EXTENSION);
          copy(genome, newGenomePath, conf);

          // Create soap index file
          final File indexFile = SOAPWrapper.makeIndexInZipFile(new File(genome));
          copy(indexFile.toString(), createPath(hadoopPath,
              CommonHadoop.GENOME_SOAP_INDEX_FILE_PREFIX, genomesCount,
              CommonHadoop.GENOME_SOAP_INDEX_FILE_SUFFIX), conf);
          indexFile.delete();

          genomesMap.put(genome, newGenomePath.getName());
        }
        s.getMetadata().setGenome(genomesMap.get(genome));

        // Copy the annotation
        final String annotation = s.getMetadata().getAnnotation();

        if (!annotationsMap.containsKey(annotation)) {
          annotationsCount++;

          final Path newAnnotationPath =
              createPath(hadoopPath, CommonHadoop.ANNOTATION_FILE_PREFIX,
                  annotationsCount, Common.GFF_EXTENSION);
          copy(annotation, newAnnotationPath, conf);
          annotationsMap.put(annotation, newAnnotationPath.getName());
        }
        s.getMetadata().setAnnotation(annotationsMap.get(annotation));

      }

      writeNewDesign(design, new Path(hadoopPath, designFilename), conf);

    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    } catch (EoulsanIOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }

  }
}
