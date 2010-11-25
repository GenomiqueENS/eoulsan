package fr.ens.transcriptome.eoulsan.datatypes.protocols;

import java.io.IOException;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.datatypes.DataFile;
import fr.ens.transcriptome.eoulsan.datatypes.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class is an abstract Protocol class that implements generic
 * getIn/OutputStream with length.
 * @author Laurent Jourdren
 */
abstract class AbstractDataProtocol implements DataProtocol {

  @Override
  public OutputStream putData(final DataFile src, final DataFileMetadata md)
      throws IOException {

    if (!isWritable())
      throw new IOException("Writing is not allowed for the source: " + src);

    return putData(src);
  }

  @Override
  public void putData(DataFile src, DataFile dest) throws IOException {

    if (src == null)
      throw new NullPointerException("The source of the data to put is null");

    if (dest == null)
      throw new NullPointerException(
          "The destination of the data to put is null");

    final DataFileMetadata mdSrc = src.getMetaData();

    FileUtils.copy(src.getProtocol().getData(src), dest.getProtocol().putData(
        dest, mdSrc));
  }

}
