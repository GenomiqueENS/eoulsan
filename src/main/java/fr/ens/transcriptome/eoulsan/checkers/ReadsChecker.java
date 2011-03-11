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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

public class ReadsChecker implements Checker {

  public static final int MAX_READS_TO_CHECK = 1000;

  @Override
  public String getName() {

    return "reads_checker";
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

    for (Sample s : design.getSamples()) {

      final InputStream is;

      try {

        is = context.getInputStream(DataFormats.READS_FASTQ, s);
        checkReadsFile(is, MAX_READS_TO_CHECK);

      } catch (IOException e) {
        throw new EoulsanException("Error while reading reads of sample "
            + s.getSource() + " for checking: " + e.getMessage());
      } catch (BadBioEntryException e) {
        throw new EoulsanException("Found bad read entry in sample "
            + s.getSource() + " when checking: " + e.getEntry());
      }

    }

    return true;
  }

  private boolean checkReadsFile(final InputStream is, final int maxReadTocheck)
      throws IOException, BadBioEntryException {

    final FastQReader reader = new FastQReader(is);

    int count = 0;

    while (reader.readEntry() && count < maxReadTocheck) {

      count++;
    }
    is.close();
    return true;
  }

}
