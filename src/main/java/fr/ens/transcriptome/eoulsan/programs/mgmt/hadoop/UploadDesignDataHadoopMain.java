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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.datasources.DataSourceUtils;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

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

  public static void upload(final String designPathname,
      final String paramPathname, final String hadoopPathname,
      final Configuration conf) throws IOException, EoulsanIOException {

    final Path hadoopPath = new Path(hadoopPathname);

    if (PathUtils.isFile(hadoopPath, conf)
        || PathUtils.isExistingDirectoryFile(hadoopPath, conf))
      throw new IOException("The output path already exists.");

    PathUtils.mkdirs(hadoopPath, conf);

    final Path designPath = new Path(designPathname);
    final FileSystem designFs = designPath.getFileSystem(conf);

    System.out.println(designPath);
    System.out.println(designFs);
    System.out.println(designFs.getName());

    final DesignReader dr = new SimpleDesignReader(designFs.open(designPath));
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

    int genomesCount = 0;
    int annotationsCount = 0;

    for (Sample s : design.getSamples()) {

      // Copy the sample
      final Path newSamplePath =
          createPath(hadoopPath, CommonHadoop.SAMPLE_FILE_PREFIX, s.getId(),
              Common.TFQ_EXTENSION);

      if (Common.TFQ_EXTENSION.equals(StringUtils
          .extensionWithoutCompressionExtension(s.getSource())))
        copy(s.getSource(), newSamplePath, conf);
      else {

        final FileSystem fs = newSamplePath.getFileSystem(conf);

        final BufferedWriter bw =
            new BufferedWriter(new OutputStreamWriter(fs.create(newSamplePath)));

        final FastQReader fqr =
            new FastQReader(DataSourceUtils.identifyDataSource(s.getSource())
                .getInputStream());

        while (fqr.readEntry())
          bw.write(fqr.toTFQ(false));

        bw.close();
        fqr.close();
      }

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

    // Copy new design file
    writeNewDesign(design, new Path(hadoopPath, designPathname), conf);

    // Copy parameter
    final Path paramPath = new Path(paramPathname);
    final FileSystem paramFs = paramPath.getFileSystem(conf);
    PathUtils.copyInputStreamToPath(paramFs.open(paramPath), hadoopPath, conf);

  }
}
