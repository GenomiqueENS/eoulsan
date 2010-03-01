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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.FileUtils.SuffixFilenameFilter;

public class ImportData {

  public static void main(final String[] args) throws Exception {

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length != 4)
      throw new IllegalArgumentException("Import data needs 4 arguments");

    final String uri = args[0];
    final File genomeFile = new File(args[1]);
    final File readsFile = new File(args[2]);
    final File genomeSoapIndexFile = new File(args[3]);

    // Check the parameters
    FileUtils.checkExistingStandardFile(genomeFile, "genome file");
    FileUtils.checkExistingStandardFileOrDirectory(readsFile, "reads file");
    FileUtils.checkExistingStandardFile(genomeSoapIndexFile,
        "genome SOAP index zip file");

    // Create configuration
    final Configuration conf = new Configuration();

    // Create the filesystem object
    final FileSystem fs = FileSystem.get(URI.create(uri), conf);

    // Create the directory for the process
    final Path basePath = new Path(uri);
    if (!fs.mkdirs(basePath))
      new IOException("Unable to create directory: " + uri);

    // Create the input directory
    final Path readsDirPath = new Path(uri + "/" + Common.READS_SUBDIR);
    if (!fs.mkdirs(readsDirPath))
      new IOException("Unable to create directory: " + uri);

    //
    // Import data in tree
    //

    if (readsFile.isDirectory()) {

      // Copy all file of the input directory
      final File[] listFiles =
          readsFile.listFiles(new SuffixFilenameFilter(Common.FASTQ_EXTENSION, true));

      for (File f : listFiles)
        if (!PathUtils.copyLocalFileToPath(f, readsDirPath, conf))
          throw new IOException("Unable to copy to hdfs the  file: "
              + f.getAbsolutePath());
    } else if (!PathUtils.copyLocalFileToPath(readsFile, readsDirPath, conf))
      throw new IOException("Unable to copy to hdfs the  file: " + readsFile);

    // import genome index file to hdfs
    if (!PathUtils.copyLocalFileToPath(genomeSoapIndexFile, new Path(basePath,
        genomeSoapIndexFile.getName()), conf))
      throw new IOException("Unable to copy to hdfs the  file: "
          + genomeSoapIndexFile);

    // import genome sequence file to hdfs
    if (!FileUtil.copy(genomeFile, fs, new Path(basePath
        + "/" + genomeFile.getName()), false, conf))
      throw new IOException("Unable to copy to hdfs the  file: " + genomeFile);

  }

}
