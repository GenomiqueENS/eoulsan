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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.checkers;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.generators.GenomeDescriptionCreator;

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
  public boolean isDesignChecker() {
    return false;
  }

  @Override
  public DataFormat getFormat() {
    return DataFormats.GENOME_FASTA;
  }

  @Override
  public Set<DataFormat> getCheckersRequired() {
    return Collections.emptySet();
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {
  }

  @Override
  public boolean check(final Data data, final CheckStore checkInfo)
      throws EoulsanException {

    if (data == null) {
      throw new NullPointerException("The data is null");
    }

    if (checkInfo == null) {
      throw new NullPointerException("The check info info is null");
    }

    // If genome has already been checked do not launch check another time
    if (checkInfo.contains(GENOME_DESCRIPTION)) {
      getLogger().info("Genome check has already been done");
      return true;
    }

    final DataFile genomeFile = data.getDataFile();

    try {

      if (!genomeFile.exists()) {
        return true;
      }

      // Check the genome and add genome description to CheckStore
      new GenomeDescriptionCreator().createGenomeDescription(genomeFile);

    } catch (IOException e) {
      throw new EoulsanException("Error while reading genome "
          + genomeFile.getSource() + " for checking: " + e.getMessage(), e);
    } catch (BadBioEntryException e) {
      throw new EoulsanException("Found bad read entry in genome "
          + genomeFile.getSource() + ": " + e.getMessage(), e);
    }

    return true;
  }

}
