package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class implements a File Protocol.
 * @author Laurent jourdren
 */
@HadoopCompatible
public class FileDataProtocol extends AbstractDataProtocol {

  @Override
  public String getName() {

    return "file";
  }

  /**
   * Get Convert a DataFile object to a File object
   * @param dataFile DataFile to convert
   * @return a File object
   */
  public File getFile(final DataFile dataFile) {

    if (dataFile == null || dataFile.getSource() == null)
      throw new NullPointerException("The source is null.");

    final String protocolName = dataFile.getProtocolPrefixInSource();

    if (protocolName == null)
      return new File(dataFile.getSource()).getAbsoluteFile();

    return new File(dataFile.getSource().substring(protocolName.length() + 1));
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    // TODO Why channel don't works ?
    // return FileUtils.createInputStream(getFile(src));
    return new FileInputStream(getFile(src));
  }

  @Override
  public OutputStream putData(final DataFile src) throws IOException {

    return FileUtils.createOutputStream(getFile(src));
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src))
      throw new FileNotFoundException("File not found: " + src);

    File f = getFile(src);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(f.length());
    result.setLastModified(f.lastModified());

    final DataFormat format =
        DataFormatRegistry.getInstance().getDataFormat(src.getName());

    result.setDataFormat(format);

    if (format != null)
      result.setContentType(format.getContentType());
    else
      result.setContentType(StringUtils
          .getCommonContentTypeFromExtension(StringUtils
              .extensionWithoutCompressionExtension(src.getName())));

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(src.getSource());

    if (ct != null)
      result.setContentEncoding(ct.getContentEncoding());

    return result;
  }

  @Override
  public boolean exists(final DataFile src) {

    return getFile(src).exists();
  }

  @Override
  public boolean isReadable() {

    return true;
  }

  @Override
  public boolean isWritable() {

    return true;
  }

}
