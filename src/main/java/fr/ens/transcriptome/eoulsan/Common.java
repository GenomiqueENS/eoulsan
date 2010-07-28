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

package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define common constants.
 * @author Laurent Jourdren
 */
public class Common {

  public static final String FASTA_EXTENSION = ".fasta";
  public static final String READS_SUBDIR = "reads";
  public static final String FASTQ_EXTENSION = ".fq";
  public static final String TFQ_EXTENSION = ".tfq";  
  public static final String GFF_EXTENSION = ".gff";
  public static final String SOAP_INDEX_ZIP_FILE_EXTENSION = ".soapindex.zip";
  public static final String READS_FILTERED_EXTENSION = ".readsfiltered";
  public static final String SOAP_RESULT_EXTENSION = ".soapaln";
  public static final String UNMAP_EXTENSION = ".unmap";
  public static final String ALL_UNMAP_FILE = "all" + UNMAP_EXTENSION;
  public static final String ALL_SOAP_RESULT_FILE =
      "all" + SOAP_RESULT_EXTENSION;
  public static final String EXPRESSION_FILE_SUFFIX = ".txt";

  public static final String SAMPLE_PREFIX = "sample_";
  public static final String SAMPLE_FILTERED_PREFIX = "sample_filtered_";
  public static final String SAMPLE_SOAP_ALIGNMENT_PREFIX =
      "sample_soap_alignment_";
  public static final String SAMPLE_SOAP_UNMAP_ALIGNMENT_PREFIX =
      "sample_soap_unmap_";
  public static final String GENOME_SOAP_INDEX_DIR_PREFIX =
      "genome_soap_index_";

  public static final String SAMPLE_EXPRESSION_FILE_PREFIX =
      "sample_expression_";
  public static final String SAMPLE_EXPRESSION_FILE_SUFFIX = ".txt";

  public static final String SOAP_ARGS_DEFAULT = "-r 2 -l 28";

  
  public static final String SOAP_INPUT_READS_COUNTER = "soap input reads";
  public static final String READS_AFTER_FILTERING_COUNTER = "reads after filtering";
  public static final String SOAP_ALIGNEMENT_WITH_ONLY_ONE_HIT_COUNTER = "soap alignment with only one hit";
  
  /**
   * Get the identifier of the sample from the source path
   * @param sample Sample to use
   * @return the identifier of the source
   */
  public static int getSampleId(final String sampleSource) {

    if (sampleSource == null || "".equals(sampleSource))
      return -1;

    final String basename = StringUtils.basename(sampleSource);
    final int pos = basename.lastIndexOf('_');

    return Integer.parseInt(basename.substring(pos + 1));
  }

  /**
   * Write log data.
   * @param os OutputStream of the log file
   * @param data data to write
   * @throws IOException if an error occurs while writing log file
   */
  public static void writeLog(final OutputStream os, final long startTime,
      final String data) throws IOException {

    final long endTime = System.currentTimeMillis();
    final long duration = endTime - startTime;

    final Writer writer = new OutputStreamWriter(os);
    writer.write("Start time: "
        + new Date(startTime) + "\nEnd time: " + new Date(endTime)
        + "\nDuration: " + StringUtils.toTimeHumanReadable(duration) + "\n");
    writer.write(data);
    writer.close();
  }

  /**
   * Write log data.
   * @param os OutputStream of the log file
   * @param data data to write
   * @throws IOException if an error occurs while writing log file
   */
  public static void writeLog(final File file, final long startTime,
      final String data) throws IOException {

    if (file == null)
      throw new NullPointerException("File for log file is null.");

    writeLog(FileUtils.createOutputStream(file), startTime, data);
  }

}
