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
import fr.ens.transcriptome.eoulsan.datatypes.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

public class GenomeChecker implements Checker {

  public static final String INFO_CHROMOSOME = "info_chromosomes";

  @Override
  public String getName() {

    return "genome_checker";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {
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

    try {
      is = context.getInputStream(DataFormats.GENOME_FASTA, s);

      checkInfo.add(INFO_CHROMOSOME, checkGenomeFile(is));

    } catch (IOException e) {
      throw new EoulsanException("Error while reading genome "
          + s.getMetadata().getGenome() + " for checking");
    } catch (BadBioEntryException e) {
      throw new EoulsanException("Found bad read entry in genome "
          + s.getMetadata().getGenome() + ": " + e.getMessage());
    }

    return true;
  }

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
        currentChr = parseChromosomeName(line);
        currentSize = 0;
      } else {
        if (currentChr == null)
          throw new BadBioEntryException(
              "No fasta header found at the start of the fasta file.", line);
        currentSize += checkBases(line.trim());
      }
    }
    chromosomes.put(currentChr, currentSize);

    is.close();
    return chromosomes;
  }

  private int checkBases(final String s) throws BadBioEntryException {

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

  private String parseChromosomeName(final String fastaHeader) {

    if (fastaHeader == null)
      return null;

    final String s = fastaHeader.substring(1).trim();
    String[] fields = s.split("\\s");

    if (fields == null || fields.length == 0)
      return null;

    return fields[0];
  }
}
