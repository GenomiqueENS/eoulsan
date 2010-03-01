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

import fr.ens.transcriptome.eoulsan.util.PathUtils;

public class Common {

  public static final String FASTA_EXTENSION = ".fasta";
  public static final String READS_SUBDIR = "reads";
  public static final String FASTQ_EXTENSION = ".fq";
  public static final String SOAP_INDEX_ZIP_FILE_EXTENSION = ".soapindex.zip";
  public static final String READS_FILTERED_EXTENSION = ".readsfiltered";
  public static final String SOAP_RESULT_EXTENSION = ".soapaln";
  public static final String UNMAP_EXTENSION = ".unmap";
  public static final int CHECK_COMPLETION_TIME = 5000;

  public static Path getGenomeFilePath(final Path basePath,
      final Configuration conf) throws IOException {

    if (basePath == null)
      throw new NullPointerException("Base path is null");

    final List<Path> genomePaths =
        PathUtils.listPathsBySuffix(basePath, Common.FASTA_EXTENSION, conf);
    if (genomePaths.size() == 0)
      throw new IOException("Genome file not found.");
    if (genomePaths.size() > 1)
      throw new IOException("More than one genome file found.");

    return genomePaths.get(0);
  }

}
