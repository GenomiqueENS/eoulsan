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
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeDescriptionCreator;

/**
 * This class define a Checker on genome FASTA files.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeChecker implements Checker {

  public static final String GENOME_DESCRIPTION = "genome_description";

  @Override
  public String getName() {

    return "genome_checker";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {
  }

  @Override
  public boolean check(final Context context, final Sample sample,
      final CheckStore checkInfo) throws EoulsanException {

    if (context == null)
      throw new NullPointerException("The execution context is null");

    if (sample == null)
      throw new NullPointerException("The sample is null");

    if (checkInfo == null)
      throw new NullPointerException("The check info info is null");

    // If genome has already been checked do not launch check another time
    if (checkInfo.contains(GENOME_DESCRIPTION)) {
      context.getLogger().info("Genome check has already been done");
      return true;
    }

    final DataFile genomeFile =
        context.getOutputDataFile(DataFormats.GENOME_FASTA, sample);

    try {

      if (!genomeFile.exists())
        return true;

      // Check the genome and add genome description to CheckStore
      new GenomeDescriptionCreator().createGenomeDescription(genomeFile);

    } catch (IOException e) {
      throw new EoulsanException("Error while reading genome "
          + genomeFile.getSource() + " for checking");
    } catch (BadBioEntryException e) {
      throw new EoulsanException("Found bad read entry in genome "
          + genomeFile.getSource() + ": " + e.getMessage());
    }

    return true;
  }

}
