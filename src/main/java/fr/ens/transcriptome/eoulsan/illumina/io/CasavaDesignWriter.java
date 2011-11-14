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

package fr.ens.transcriptome.eoulsan.illumina.io;

import java.io.IOException;

import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;

/**
 * This interface define a writer for Casava designs.
 * @author Laurent Jourdren
 */
public interface CasavaDesignWriter {

  /**
   * Write a design.
   * @param design design to write
   * @throws IOException if an error occurs while writing the design
   */
  void writer(CasavaDesign design) throws IOException;

}
