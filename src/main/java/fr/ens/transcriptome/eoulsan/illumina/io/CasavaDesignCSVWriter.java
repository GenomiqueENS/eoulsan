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
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesignUtil;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a writer for Casava design CSV files.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class CasavaDesignCSVWriter implements CasavaDesignWriter {

  private final Writer writer;

  @Override
  public void writer(final CasavaDesign design) throws IOException {

    this.writer.write(CasavaDesignUtil.toCSV(design));

    this.writer.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public CasavaDesignCSVWriter(final Writer writer) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

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
