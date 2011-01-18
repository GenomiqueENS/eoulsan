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

package fr.ens.transcriptome.eoulsan.bio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a genome description.
 * @author Laurent Jourdren
 */
public class GenomeDescription {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private Map<String, Integer> sequences = Maps.newHashMap();

  //
  // Setters
  //

  /**
   * Add a sequence.
   * @param sequenceName name of the sequence
   * @param sequenceLength length of the sequence
   */
  public void addSequence(final String sequenceName, final int sequenceLength) {

    LOGGER.info("Add sequence: "
        + sequenceName + " with " + sequenceLength + " pb");

    this.sequences.put(sequenceName, sequenceLength);
  }

  //
  // Getters
  //

  /**
   * Get the length of a sequence
   * @param sequenceName name of the sequence
   * @return the length of the sequence or -1 if the sequence does not exists
   */
  public int getSequenceLength(final String sequenceName) {

    if (this.sequences.containsKey(sequenceName)) {

      return this.sequences.get(sequenceName);
    }

    return -1;
  }

  /**
   * Get the names of the sequences.
   * @return a set with the name of the sequence
   */
  public Set<String> getSequencesNames() {

    return this.sequences.keySet();
  }

  //
  // Save description
  //

  /**
   * Save genome description.
   * @param os OutputStream to use for genome description writing
   */
  public void save(final OutputStream os) throws IOException {

    Preconditions.checkNotNull(os, "OutputStream is null");

    final Writer writer = FileUtils.createBufferedWriter(os);

    for (String seqName : getSequencesNames()) {

      writer.write(seqName + "\t" + getSequenceLength(seqName) + "\n");
    }

    writer.close();
  }

  /**
   * Save genome description.
   * @param file output file
   */
  public void save(final File file) throws FileNotFoundException, IOException {

    Preconditions.checkNotNull(file, "File is null");
    save(new FileOutputStream(file));
  }

  //
  // Load description
  //

  /**
   * Load genome description.
   * @param is InputStream to use
   */
  public static GenomeDescription load(final InputStream is) throws IOException {

    Preconditions.checkNotNull(is, "InputStream is null");

    final GenomeDescription result = new GenomeDescription();

    final BufferedReader read = FileUtils.createBufferedReader(is);

    String line = null;

    final Splitter splitter = Splitter.on('\t').trimResults();

    while ((line = read.readLine()) != null) {

      final List<String> fields =
          Lists.newArrayList(splitter.split(line.toString()));

      if (fields.size() > 1) {
        try {
          result.addSequence(fields.get(0), Integer.parseInt(fields.get(1)));
        } catch (NumberFormatException e) {

        }
      }
    }

    is.close();

    return result;
  }

  /**
   * Load genome description.
   * @param file File to use
   */
  public static GenomeDescription load(final File file) throws IOException {

    Preconditions.checkNotNull(file, "File is null");
    return load(new FileInputStream(file));
  }

  //
  // Static methods
  //

  /**
   * Create a GenomeDescription object from a Fasta file.
   * @param genomeFastaIs InputStream
   */
  public static GenomeDescription createGenomeDescFromFasta(
      final File genomeFastaFile) throws BadBioEntryException, IOException {

    return createGenomeDescFromFasta(FileUtils
        .createInputStream(genomeFastaFile));
  }

  /**
   * Create a GenomeDescription object from a Fasta file.
   * @param genomeFastaIs InputStream
   */
  public static GenomeDescription createGenomeDescFromFasta(
      final InputStream genomeFastaIs) throws BadBioEntryException, IOException {

    final GenomeDescription result = new GenomeDescription();

    final BufferedReader br = FileUtils.createBufferedReader(genomeFastaIs);

    String line = null;

    String currentChr = null;
    int currentSize = 0;

    while ((line = br.readLine()) != null) {

      line = line.trim();
      if ("".equals(line))
        continue;

      if (line.startsWith(">")) {
        if (currentChr != null) {
          result.addSequence(currentChr, currentSize);
        }

        currentChr = parseChromosomeName(line);
        currentSize = 0;
      } else {
        if (currentChr == null)
          throw new BadBioEntryException(
              "No fasta header found at the start of the fasta file.", line);
        currentSize += checkBases(line.trim());
      }
    }
    result.addSequence(currentChr, currentSize);

    genomeFastaIs.close();

    return result;
  }

  private static String parseChromosomeName(final String fastaHeader) {

    if (fastaHeader == null)
      return null;

    final String s = fastaHeader.substring(1).trim();
    String[] fields = s.split("\\s");

    if (fields == null || fields.length == 0)
      return null;

    return fields[0];
  }

  private static int checkBases(final String s) throws BadBioEntryException {

    final int len = s.length();

    for (int i = 0; i < len; i++)
      switch (s.charAt(i)) {

      case 'A':
      case 'a':
      case 'C':
      case 'c':
      case 'G':
      case 'g':
      case 'T':
      case 't':
      case 'U':
      case 'R':
      case 'Y':
      case 'K':
      case 'M':
      case 'S':
      case 'W':
      case 'B':
      case 'D':
      case 'H':
      case 'V':
      case 'N':
      case 'n':

        break;

      default:
        throw new BadBioEntryException(
            "Invalid base in genome: " + s.charAt(i), s);
      }

    return len;
  }

  //
  // Other methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("sequences", this.sequences.size())
        .toString();
  }

}
