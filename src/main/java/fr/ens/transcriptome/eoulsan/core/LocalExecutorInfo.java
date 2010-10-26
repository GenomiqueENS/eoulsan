package fr.ens.transcriptome.eoulsan.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an executor info in Hadoop mode.
 * @author jourdren
 */
public class LocalExecutorInfo extends AbstractExecutorInfo {

  @Override
  public InputStream getInputStream(final DataType dt, final Sample sample)
      throws IOException {

    final String src = getPathname(dt, sample);
    File f = new File(src);

    return decompressInputStreamIsNeeded(FileUtils.createInputStream(f), src);
  }

  @Override
  public OutputStream getOutputStream(DataType dt, Sample sample)
      throws IOException {

    File f = new File(getPathname(dt, sample));

    return FileUtils.createOutputStream(f);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public LocalExecutorInfo() {

    super();
  }

}
