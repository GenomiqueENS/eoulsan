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

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.mapping.FilterReadsConstants;
import fr.ens.transcriptome.eoulsan.steps.mapping.ReadsFilter;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a filter for read in local mode.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public class FilterReadsLocal {

  public static final String COUNTER_GROUP = "Filter reads";

  private DataFile fastqDF;
  private int lengthThreshold = FilterReadsConstants.LENGTH_THRESHOLD;
  private double qualityThreshold = FilterReadsConstants.LENGTH_THRESHOLD;

  /**
   * Filter the reads file.
   * @param outputFile output file with filtered reads
   * @throws IOException if an error occurs while reading the reads file
   */
  public void filter(final File outputFile, final Reporter reporter)
      throws IOException {

    final Writer writer = FileUtils.createBufferedWriter(outputFile);
    final BufferedReader br =
        FileUtils.createBufferedReader(this.fastqDF.open());

    final StringBuilder sb = new StringBuilder();
    final ReadSequence read = new ReadSequence();

    final int lengthThreshold = this.lengthThreshold;
    final double qualityThreshold = this.qualityThreshold;
    String line = null;
    int count = 0;

    while ((line = br.readLine()) != null) {

      // Trim the line
      final String trim = line.trim();

      // discard empty lines
      if ("".equals(trim))
        continue;

      count++;
      sb.append(trim);

      if (count == 1 && trim.charAt(0) != '@')
        throw new IOException("Invalid Fastq file.");

      if (count == 3 && trim.charAt(0) != '+')
        throw new IOException("Invalid Fastq file.");

      if (count == 4) {

        // Fill the ReadSequence object
        read.parseFastQ(sb.toString());
        reporter.incrCounter(COUNTER_GROUP, "input fastq", 1);

        if (!read.check()) {

          reporter.incrCounter(COUNTER_GROUP, "input fastq not valid", 1);
          continue;
        }
        reporter.incrCounter(COUNTER_GROUP, "input fastq valid", 1);

        // Trim the sequence with polyN as tail
        ReadsFilter.trimReadSequence(read);

        // Filter bad sequence
        if (ReadsFilter.isReadValid(read, lengthThreshold, qualityThreshold)) {
          writer.write(read.toFastQ());
          reporter.incrCounter(COUNTER_GROUP, "reads after filtering", 1);
        } else
          reporter.incrCounter(COUNTER_GROUP, "reads rejected by filter", 1);

        sb.setLength(0);
        count = 0;
      } else
        sb.append('\n');

    }

    br.close();
    writer.close();
  }

  /**
   * Get the threshold for the filter
   * @return the threshold
   */
  public int getThreshold() {

    return lengthThreshold;
  }

  /**
   * Set the threshold for the filter
   * @param threshold the threshold
   */
  public void setLengthThreshold(final int threshold) {

    this.lengthThreshold = threshold;
  }

  /**
   * Set the threshold for the filter
   * @param threshold the threshold
   */
  public void setQualityThreshold(final double threshold) {

    this.qualityThreshold = threshold;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param fastqFile fastq file to parse
   */
  public FilterReadsLocal(final File fastqFile) {

    if (fastqFile == null)
      throw new NullPointerException("The input fastq file is null.");
    this.fastqDF = new DataFile(fastqFile.getPath());
  }

  /**
   * Public constructor.
   * @param fastqFile fastq file to parse
   */
  public FilterReadsLocal(final DataFile df) {

    if (df == null)
      throw new NullPointerException("The data source is null.");

    this.fastqDF = df;
  }

}
