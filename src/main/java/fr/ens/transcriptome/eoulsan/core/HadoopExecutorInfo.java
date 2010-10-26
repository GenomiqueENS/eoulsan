package fr.ens.transcriptome.eoulsan.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define an executor info in Hadoop mode.
 * @author jourdren
 */
public class HadoopExecutorInfo extends AbstractExecutorInfo {

  private Configuration conf;

  @Override
  public InputStream getInputStream(final DataType dt, final Sample sample)
      throws IOException {

    final String src = getPathname(dt, sample);
    final Path p = new Path(src);
    final FileSystem fs = p.getFileSystem(this.conf);

    return decompressInputStreamIsNeeded(fs.open(p), src);
  }

  @Override
  public OutputStream getOutputStream(final DataType dt, final Sample sample)
      throws IOException {

    final Path p = new Path(getPathname(dt, sample));
    final FileSystem fs = p.getFileSystem(this.conf);

    return fs.create(p);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param conf Hadoop Configuration object
   */
  public HadoopExecutorInfo(final Configuration conf) {

    super();

    if (conf == null)
      throw new NullPointerException("The configuration is null");

    this.conf = conf;
  }

}
