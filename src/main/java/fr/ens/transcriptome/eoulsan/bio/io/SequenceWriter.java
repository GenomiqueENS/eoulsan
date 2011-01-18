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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.IOException;

import fr.ens.transcriptome.eoulsan.bio.Sequence;

/**
 * This abstract class define method to implements for SequenceWriter.
 * @author Laurent Jourdren
 */
public abstract class SequenceWriter extends Sequence {

  /**
   * Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  public abstract void write() throws IOException;

  /**
   * Close the writer
   * @throws IOException if an error occurs while closing file
   */
  public abstract void close() throws IOException;
}
