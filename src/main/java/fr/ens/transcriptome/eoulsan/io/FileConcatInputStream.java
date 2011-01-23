package fr.ens.transcriptome.eoulsan.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * This class define an InputStream that concatenate files in an InputStream.
 * @author Laurent Jourdren
 */
public class FileConcatInputStream extends AbstractConcatInputStream {

  private final Iterator<File> it;

  @Override
  protected boolean hasNextInputStream() {

    return it.hasNext();
  }

  @Override
  protected InputStream nextInputStream() throws IOException {

    return new FileInputStream(it.next());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param files files to concatenate in the InputStream.
   */
  public FileConcatInputStream(final List<File> files) {

    Preconditions.checkNotNull(files, "files is null");

    this.it = files.iterator();
  }

}
