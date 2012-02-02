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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This method define a Fasta reader for fasta section of GFF files.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class GFFFastaReader extends FastaReader {

  @Override
  public boolean hasNext() {

    String line = null;

    try {
      while ((line = this.reader.readLine()) != null) {

        if (line.startsWith("##FASTA"))
          return super.hasNext();

      }
    } catch (IOException e) {

      this.exception = e;
    }

    return false;
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param file File to use
   */
  public GFFFastaReader(final File file) throws FileNotFoundException {
    super(file);
  }

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public GFFFastaReader(final InputStream is) throws FileNotFoundException {
    super(is);
  }

}
