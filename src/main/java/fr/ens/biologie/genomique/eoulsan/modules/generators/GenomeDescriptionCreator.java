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

package fr.ens.biologie.genomique.eoulsan.modules.generators;

import fr.ens.biologie.genomique.eoulsan.checkers.CheckStore;
import fr.ens.biologie.genomique.eoulsan.checkers.GenomeChecker;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.storages.DataFileGenomeDescStorage;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import java.io.IOException;

/**
 * This class define a genome description creator.
 *
 * @since 1.2
 * @author Laurent Jourdren
 */
public class GenomeDescriptionCreator {

  private static final String CHECK_STORE_KEY = GenomeChecker.GENOME_DESCRIPTION;

  private final CheckStore checkStore;
  private final DataFileGenomeDescStorage storage;

  /**
   * Create genome description object from the storage if already exists or compute it from the
   * genome.
   *
   * @param annotationDataFile annotation file
   * @return the genome description object
   * @throws BadBioEntryException if an error occurs while computing the genome description
   * @throws IOException if an error occurs while computing the genome description
   */
  public GenomeDescription createGenomeDescriptionFromAnnotation(final DataFile annotationDataFile)
      throws BadBioEntryException, IOException {

    return createGenomeDescription(annotationDataFile);
  }

  /**
   * Create genome description object from the storage if already exists or compute it from the
   * genome.
   *
   * @param genomeDataFile genome file
   * @return the genome description object
   * @throws BadBioEntryException if an error occurs while computing the genome description
   * @throws IOException if an error occurs while computing the genome description
   */
  public GenomeDescription createGenomeDescription(final DataFile genomeDataFile)
      throws BadBioEntryException, IOException {

    // Check if the genome description has been already put in the CheckStore
    GenomeDescription desc = (GenomeDescription) this.checkStore.get(CHECK_STORE_KEY);
    if (desc != null) {
      return desc;
    }

    if (this.storage != null) {
      desc = this.storage.get(genomeDataFile.getSource());
    }

    if (desc == null) {

      // Compute the genome description
      desc =
          GenomeDescription.createGenomeDescFromFasta(
              genomeDataFile.open(), genomeDataFile.getName());

      // Store it if storage exists
      if (this.storage != null) {
        this.storage.put(genomeDataFile.getSource(), desc);
      }
    }

    // Add the genome description in the CheckStore
    this.checkStore.add(CHECK_STORE_KEY, desc);

    return desc;
  }

  //
  // Constructor
  //

  /** Public constructor. */
  public GenomeDescriptionCreator() {

    this.storage = GenomeDescriptionGeneratorModule.checkForGenomeDescStore();
    this.checkStore = CheckStore.getCheckStore();
  }
}
