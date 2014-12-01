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

import static fr.ens.transcriptome.eoulsan.bio.io.BioCharsets.FASTA_CHARSET;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class allow to parse FASTA files line by line without storing the whole
 * sequence in memory.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class FastaLineParser {

  final BufferedReader reader;
  private String seqName;
  private String sequence;
  private boolean fastaSectionFound;

  /**
   * Parse the next sequence line of the FASTA file and return the current
   * sequence name.
   * @return the current sequence name
   * @throws IOException if an error occurs while reading the FASTA file
   */
  public String parseNextLineAndGetSequenceName() throws IOException {

    String line = null;

    while ((line = this.reader.readLine()) != null) {

      // Trim the line
      final String trim = line.trim();

      // discard empty lines
      if ("".equals(trim))
        continue;

      if (!this.fastaSectionFound) {
        if (line.startsWith("##FASTA")) {
          this.fastaSectionFound = true;
        }
        continue;
      }

      if (trim.charAt(0) == '>') {

        seqName = trim.substring(1);
        continue;

      } else if (seqName == null)
        throw new IOException(
            "No fasta header found at the beginning of the fasta file: " + line);

      this.sequence = trim;
      return this.seqName;
    }

    return null;
  }

  /**
   * Get the sequence of the last read line.
   * @return a String with the sequence trimmed
   */
  public String getSequence() {

    return this.sequence;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param is InputStream to use
   */
  public FastaLineParser(final InputStream is) {

    this(is, false);
  }

  /**
   * Public constructor.
   * @param is InputStream to use
   * @param gffFile the input file is a GFF file
   */
  public FastaLineParser(final InputStream is, final boolean gffFile) {

    if (is == null)
      throw new NullPointerException("InputStream is null");

    this.reader = new BufferedReader(new InputStreamReader(is, FASTA_CHARSET));
    if (!gffFile)
      this.fastaSectionFound = true;
  }

  /**
   * Public constructor.
   * @param file File to use
   */
  public FastaLineParser(final File file) throws FileNotFoundException {

    this(file, false);
  }

  /**
   * Public constructor.
   * @param file File to use
   * @param gffFile the input file is a GFF file
   */
  public FastaLineParser(final File file, final boolean gffFile)
      throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("File is null");

    this.reader = FileUtils.createBufferedReader(file, FASTA_CHARSET);

    if (!gffFile)
      this.fastaSectionFound = true;
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public FastaLineParser(final String filename) throws FileNotFoundException {

    this(filename, false);
  }

  /**
   * Public constructor.
   * @param filename File to use
   * @param gffFile the input file is a GFF file
   */
  public FastaLineParser(final String filename, final boolean gffFile)
      throws FileNotFoundException {

    this.reader = FileUtils.createBufferedReader(filename, FASTA_CHARSET);

    if (!gffFile)
      this.fastaSectionFound = true;
  }

}
