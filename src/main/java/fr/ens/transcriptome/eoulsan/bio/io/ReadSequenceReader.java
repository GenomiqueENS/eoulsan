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

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This abstract class define method to implements for ReadSequenceReader.
 * @author Laurent Jourdren
 */
public abstract class ReadSequenceReader extends ReadSequence {

  /**
   * Read the next entry in the stream.
   * @return false if there is no more entry to read
   * @throws IOException if an error occurs while reading file
   */
  public abstract boolean readEntry() throws IOException, BadBioEntryException;

  /**
   * Close the stream.
   * @throws IOException
   */
  public abstract void close() throws IOException;

}
