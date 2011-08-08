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

package fr.ens.transcriptome.eoulsan.checkers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

public class GenomeChecker implements Checker {

  public static final String INFO_CHROMOSOME = "info_chromosomes";

  @Override
  public String getName() {

    return "genome_checker";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {
  }

  @Override
  public boolean check(final Design design, final Context context,
      final CheckStore checkInfo) throws EoulsanException {

    if (design == null)
      throw new NullPointerException("The design is null");

    if (context == null)
      throw new NullPointerException("The execution context is null");

    if (checkInfo == null)
      throw new NullPointerException("The check info info is null");

    final List<Sample> samples = design.getSamples();

    if (samples == null)
      throw new NullPointerException("The samples are null");

    if (samples.size() == 0)
      throw new EoulsanException("No samples found in design");

    final Sample s = samples.get(0);

    final InputStream is;

    final DataFile file = context.getDataFile(DataFormats.GENOME_FASTA, s);
    try {

      if (!file.exists())
        return true;

      is = file.open();

      checkInfo.add(INFO_CHROMOSOME, checkGenomeFile(is));

    } catch (IOException e) {
      throw new EoulsanException("Error while reading genome "
          + file.getSource() + " for checking");
    } catch (BadBioEntryException e) {
      throw new EoulsanException("Found bad read entry in genome "
          + file.getSource() + ": " + e.getMessage());
    }

    return true;
  }

  /**
   * Check a genome file.
   * @param is Input stream to read for the checking
   * @return a map the the sizes of the chromosomes
   * @throws IOException if an error occurs while reading data
   * @throws BadBioEntryException if the name or the sequence of the chromosome
   *           is not valid
   */
  private Map<String, Integer> checkGenomeFile(final InputStream is)
      throws IOException, BadBioEntryException {

    final BufferedReader br = new BufferedReader(new InputStreamReader(is));

    String line = null;

    final Map<String, Integer> chromosomes = new HashMap<String, Integer>();
    String currentChr = null;
    int currentSize = 0;

    while ((line = br.readLine()) != null) {

      line = line.trim();
      if ("".equals(line))
        continue;

      if (line.startsWith(">")) {
        chromosomes.put(currentChr, currentSize);
        currentChr = parseFastaEntryHeader(line);
        currentSize = 0;
      } else {
        if (currentChr == null)
          throw new BadBioEntryException(
              "No fasta header found at the start of the fasta file.", line);
        currentSize += checkBases(line.trim());
      }
    }

    // Check if two sequences exists with the same name exists
    if (chromosomes.containsKey(currentChr))
      throw new BadBioEntryException(
          "Sequence name found twice: " + currentChr, line);

    chromosomes.put(currentChr, currentSize);

    is.close();
    return chromosomes;
  }

  /**
   * Check the base used in a sequence
   * @param sequence sequence
   * @return the length of the sequence
   * @throws BadBioEntryException if an invalid base is found
   */
  private int checkBases(final String sequence) throws BadBioEntryException {

    final int len = sequence.length();

    for (int i = 0; i < len; i++)
      switch (sequence.charAt(i)) {

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
        throw new BadBioEntryException("Invalid base in genome: "
            + sequence.charAt(i), sequence);
      }

    return len;
  }

  /**
   * Parse a fasta entry header.
   * @param fastaHeader fasta header
   * @return the first word of the header
   * @throws BadBioEntryException if there is an error in the name of the
   *           sequence
   */
  private String parseFastaEntryHeader(final String fastaHeader)
      throws BadBioEntryException {

    if (fastaHeader == null)
      return null;

    final String s = fastaHeader.substring(1);

    if (s.startsWith(" "))
      throw new BadBioEntryException(
          "A whitespace was found at the begining of the sequence name: ",
          fastaHeader);

    String[] fields = s.split("\\s");

    if (fields == null || fields.length == 0)
      throw new BadBioEntryException("Invalid sequence header", fastaHeader);

    final String result = fields[0].trim();

    if ("".equals(result))
      throw new BadBioEntryException("Invalid sequence name: " + result,
          fastaHeader);

    return result;
  }
}
