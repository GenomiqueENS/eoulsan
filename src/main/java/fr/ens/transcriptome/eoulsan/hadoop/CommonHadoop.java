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

package fr.ens.transcriptome.eoulsan.hadoop;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define common constants and other methods specific to Hadoop mode.
 * @author Laurent Jourdren
 */
public class CommonHadoop extends Common {

  public static final int CHECK_COMPLETION_TIME = 5000;

  /**
   * Retrieve the genome file name from the files of a directory
   * @param basePath Base path directory
   * @param conf Hadoop configuration
   * @return the genome file path
   * @throws IOException if the genome file can't be identified
   */
  public static Path getGenomeFilePath(final Path basePath,
      final Configuration conf) throws IOException {

    if (basePath == null)
      throw new NullPointerException("Base path is null");

    final List<Path> genomePaths =
        PathUtils.listPathsBySuffix(basePath, CommonHadoop.FASTA_EXTENSION,
            conf);
    if (genomePaths.size() == 0)
      throw new IOException("Genome file not found.");
    if (genomePaths.size() > 1)
      throw new IOException("More than one genome file found.");

    return genomePaths.get(0);
  }

}
