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

package fr.ens.transcriptome.eoulsan.splitermergers;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This interface define a splitter.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface Splitter {

  /**
   * Get the format related to the splitter.
   * @return a DataFormat object
   */
  DataFormat getFormat();

  /**
   * Configure the splitter.
   * @param conf configuration
   * @throws EoulsanException if the configuration is invalid
   */
  void configure(Set<Parameter> conf) throws EoulsanException;

  /**
   * Split data.
   * @param inFile input DataFile
   * @param outFileIterator iterator over DataFile to create
   * @throws IOException if an error occurs while split data
   */
  void split(DataFile inFile, Iterator<DataFile> outFileIterator)
      throws IOException;

}
