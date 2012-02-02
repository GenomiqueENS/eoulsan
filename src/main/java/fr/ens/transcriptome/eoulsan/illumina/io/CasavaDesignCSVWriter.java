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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
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
 * @since 1.1
 * @author Laurent Jourdren
 */
public class CasavaDesignCSVWriter implements CasavaDesignWriter {

  private Writer writer;

  @Override
  public void writer(final CasavaDesign design) throws IOException {

    this.writer
        .write("\"FCID\",\"Lane\",\"SampleID\",\"SampleRef\",\"Index\",\"Description\","
            + "\"Control\",\"Recipe\",\"Operator\",\"SampleProject\"\n");

    if (design != null) {

      final StringBuilder sb = new StringBuilder();

      for (CasavaSample s : design) {

        sb.append(s.getFlowCellId().trim().toUpperCase());
        sb.append(',');
        sb.append(s.getLane());
        sb.append(',');
        sb.append(quote(s.getSampleId().trim()));
        sb.append(',');
        sb.append(quote(s.getSampleRef().trim()));
        sb.append(',');
        sb.append(quote(s.getIndex().toUpperCase()));
        sb.append(',');
        sb.append(quote(s.getDescription().trim()));
        sb.append(',');
        sb.append(s.isControl() ? 'Y' : 'N');
        sb.append(',');
        sb.append(quote(s.getRecipe().trim()));
        sb.append(',');
        sb.append(quote(s.getOperator().trim()));
        sb.append(',');
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

    if (s.indexOf(' ') != -1 || s.indexOf(',') != -1)
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
