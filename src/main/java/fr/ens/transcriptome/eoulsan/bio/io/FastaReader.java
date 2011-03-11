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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This method define a Fasta reader.
 * @author Laurent Jourdren
 */
public class FastaReader extends SequenceReader {

  private BufferedReader reader;
  private final StringBuilder sb = new StringBuilder();
  private String nextSequenceName;
  private boolean end = false;

  /**
   * Read the next entry in the stream.
   * @return false if there is no more entry to read
   * @throws IOException if an error occurs while reading file
   */
  @Override
  public boolean readEntry() throws IOException, BadBioEntryException {

    if (this.end) {
      return false;
    }

    String line = null;

    while ((line = this.reader.readLine()) != null) {

      // Trim the line
      final String trim = line.trim();

      // discard empty lines
      if ("".equals(trim))
        continue;

      if (trim.charAt(0) == '>') {

        if (this.nextSequenceName != null) {

          setName(this.nextSequenceName);
          setSequence(sb.toString());
          sb.setLength(0);
          this.nextSequenceName = trim.substring(1);

          return true;
        }

        this.nextSequenceName = trim.substring(1);

      } else {
        sb.append(trim);
      }
    }

    setName(this.nextSequenceName);
    setSequence(sb.toString());
    sb.setLength(0);

    this.end = true;

    return true;
  }

  /**
   * Close the stream.
   * @throws IOException
   */
  @Override
  public void close() throws IOException {

    this.reader.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public FastaReader(final InputStream is) {

    if (is == null)
      throw new NullPointerException("InputStream is null");

    this.reader = new BufferedReader(new InputStreamReader(is));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public FastaReader(final File file) throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("File is null");

    if (!file.isFile())
      throw new FileNotFoundException("File not found: "
          + file.getAbsolutePath());

    this.reader = FileUtils.createBufferedReader(file);
  }

}
