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

package fr.ens.transcriptome.eoulsan.programs.mapping.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.datasources.FileDataSource;
import fr.ens.transcriptome.eoulsan.parsers.ReadSequence;
import fr.ens.transcriptome.eoulsan.programs.mapping.FilterReadsConstants;
import fr.ens.transcriptome.eoulsan.programs.mapping.ReadsFilter;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a filter for read in local mode.
 * @author Laurent Jourdren
 */
public class FilterReadsLocal {

  private DataSource fastqDS;
  private int threshold = FilterReadsConstants.THRESHOLD;

  /**
   * Filter the reads file.
   * @param outputFile output file with filtered reads
   * @throws IOException if an error occurs while reading the reads file
   */
  public void filter(final File outputFile) throws IOException {

    final Writer writer = FileUtils.createBufferedWriter(outputFile);
    final BufferedReader br;

    // Create the reader
    if ("File".equals(this.fastqDS.getSourceType()))
      br =
          FileUtils
              .createBufferedReader(new File(this.fastqDS.getSourceInfo()));
    else
      br =
          new BufferedReader(new InputStreamReader(this.fastqDS
              .getInputStream()));

    final StringBuilder sb = new StringBuilder();
    final ReadSequence read = new ReadSequence();

    final int threshold = this.threshold;
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

        // Trim the sequence with polyN as tail
        ReadsFilter.trimReadSequence(read);

        // Filter bad sequence
        if (ReadsFilter.isReadValid(read, threshold))
          writer.write(read.toFastQ());

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

    return threshold;
  }

  /**
   * Set the threshold for the filter
   * @param threshold the threshold
   */
  public void setThreshold(final int threshold) {

    this.threshold = threshold;
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
    this.fastqDS = new FileDataSource(fastqFile);
  }

  /**
   * Public constructor.
   * @param fastqFile fastq file to parse
   */
  public FilterReadsLocal(final DataSource ds) {

    if (ds == null)
      throw new NullPointerException("The data source is null.");

    this.fastqDS = ds;
  }

}
