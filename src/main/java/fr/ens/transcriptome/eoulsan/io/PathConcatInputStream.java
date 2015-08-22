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

package fr.ens.transcriptome.eoulsan.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * This class define an InputStream that concatenate path in an InputStream.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PathConcatInputStream extends AbstractConcatInputStream {

  private final Iterator<Path> it;
  private final Configuration conf;

  @Override
  protected boolean hasNextInputStream() {

    return this.it.hasNext();
  }

  @Override
  protected InputStream nextInputStream() throws IOException {

    final Path path = this.it.next();

    if (path == null) {
      throw new IOException("path is null");
    }

    final FileSystem fs = path.getFileSystem(this.conf);

    return fs.open(path);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param paths paths to concatenate in the InputStream.
   * @param conf Hadoop configuration
   */
  public PathConcatInputStream(final List<Path> paths,
      final Configuration conf) {

    checkNotNull(paths, "paths is null");
    checkNotNull(conf, "conf is null");

    this.it = paths.iterator();
    this.conf = conf;
  }

}
