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
 * of the �cole Normale Sup�rieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.design.io;

import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;



/**
 * This interface define a DesignReader.
 * @author Laurent Jourdren
 */
public interface DesignReader  {

  /**
   * Read the design.
   * @return a new Design object
   * @throws NividicIOException if an error occurs while reading the design
   */
  Design read() throws EoulsanIOException;
}
