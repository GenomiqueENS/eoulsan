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

package fr.ens.biologie.genomique.eoulsan.data.storages;

import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.bio.readmapper.MapperInstance;

/**
 * This interface define a genome index storage.
 * @since 1.1
 * @author Laurent Jourdren
 */
public interface GenomeIndexStorage {

  /**
   * Get the DataFile that corresponds to a mapper and a genome
   * @param mapperInstance mapper
   * @param genome genome description object for the genome
   * @param additionalDescription description of the additional parameters
   * @return a DataFile that contains the path to the index or null if the index
   *         has not yet been computed
   */
  DataFile get(MapperInstance mapperInstance, GenomeDescription genome,
      Map<String, String> additionalDescription);

  /**
   * Put the index archive in the storage.
   * @param mapperInstance mapper
   * @param genome genome description object
   * @param additionalDescription description of the additional parameters
   * @param indexArchive the DataFile that contains the index
   */
  void put(MapperInstance mapperInstance, GenomeDescription genome,
      Map<String, String> additionalDescription, DataFile indexArchive);

}
