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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class implements a Fastq reader.
 * @author Laurent Jourdren
 */
public class FastQReader extends ReadSequence {

  private BufferedReader reader;
  private final StringBuilder sb = new StringBuilder();

  /**
   * Read the next entry in the stream.
   * @return false if there is no more entry to read
   * @throws IOException if an error occurs while reading file
   */
  public boolean readEntry() throws IOException, BadBioEntryException {

    String line = null;
    int count = 0;

    while ((line = this.reader.readLine()) != null) {

      // Trim the line
      final String trim = line.trim();

      // discard empty lines
      if ("".equals(trim))
        continue;

      count++;
      sb.append(trim);

      if (count == 1 && trim.charAt(0) != '@')
        throw new BadBioEntryException(
            "Invalid Fastq file. First line don't start with '@'", line);

      if (count == 3 && trim.charAt(0) != '+')
        throw new BadBioEntryException(
            "Invalid Fastq file. Third line don't start with '@'", line);

      if (count == 4) {

        // Fill the ReadSequence object
        parseFastQ(sb.toString());
        sb.setLength(0);
        return true;
      }
      sb.append('\n');
    }

    return false;
  }

  /**
   * Close the stream.
   * @throws IOException
   */
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
  public FastQReader(final InputStream is) {

    if (is == null)
      throw new NullPointerException("InputStream is null");

    this.reader = new BufferedReader(new InputStreamReader(is));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public FastQReader(final File file) throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("File is null");

    if (!file.isFile())
      throw new FileNotFoundException("File not found: "
          + file.getAbsolutePath());

    this.reader = FileUtils.createBufferedReader(file);
  }

}
