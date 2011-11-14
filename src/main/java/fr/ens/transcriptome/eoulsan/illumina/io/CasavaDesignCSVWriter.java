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

package fr.ens.transcriptome.eoulsan.illumina.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a writer for Casava design CSV files.
 * @author Laurent Jourdren
 */
public class CasavaDesignCSVWriter implements CasavaDesignWriter {

  private Writer writer;

  @Override
  public void writer(final CasavaDesign design) throws IOException {

    this.writer
        .write("\"FCID\",\"Lane\",\"SampleID\",\"SampleRef\",\"Index\",\"Description\","
            + "\"Control\",\"Recipe\",\"Operator\",\"SampleProject\"");

    if (design != null) {

      final StringBuilder sb = new StringBuilder();

      for (CasavaSample s : design) {

        sb.append(s.getFlowCellId());
        sb.append('\t');
        sb.append(s.getLane());
        sb.append('\t');
        sb.append(quote(s.getSampleId()));
        sb.append('\t');
        sb.append(quote(s.getSampleRef()));
        sb.append('\t');
        sb.append(quote(s.getIndex()));
        sb.append('\t');
        sb.append(quote(s.getDescription()));
        sb.append('\t');
        sb.append(s.isControl() ? 'Y' : 'N');
        sb.append('\t');
        sb.append(quote(s.getRecipe()));
        sb.append('\t');
        sb.append(quote(s.getOperator()));
        sb.append('\t');
        sb.append(quote(s.getSampleProject()));

        sb.append('\n');

        // Write the string
        writer.write(sb.toString());
        sb.setLength(0);

      }

    }

    this.writer.close();
  }

  private static String quote(final String s) {

    if (s == null)
      return "";

    final String trimmed = s.trim();

    if (s.indexOf(' ') != -1)
      return '\"' + trimmed + '\"';
    return trimmed;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public CasavaDesignCSVWriter(final Writer writer) {

    if (writer == null)
      throw new NullPointerException("The writer is null.");

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public CasavaDesignCSVWriter(final OutputStream os)
      throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public CasavaDesignCSVWriter(final File outputFile) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public CasavaDesignCSVWriter(final String outputFilename) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFilename);
  }

}
