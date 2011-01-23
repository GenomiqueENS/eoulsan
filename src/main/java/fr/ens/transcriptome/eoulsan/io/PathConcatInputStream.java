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
 * @author Laurent Jourdren
 */
public class PathConcatInputStream extends AbstractConcatInputStream {

  private final Iterator<Path> it;
  private final Configuration conf;

  @Override
  protected boolean hasNextInputStream() {

    return it.hasNext();
  }

  @Override
  protected InputStream nextInputStream() throws IOException {

    final Path path = it.next();

    if (path == null)
      throw new IOException("path is null");

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
  public PathConcatInputStream(final List<Path> paths, final Configuration conf) {

    checkNotNull(paths, "paths is null");
    checkNotNull(conf, "conf is null");

    this.it = paths.iterator();
    this.conf = conf;
  }

}
