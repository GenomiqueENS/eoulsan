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

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.File;
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

  @Override
  public File getSourceAsFile(final DataFile dataFile) {

    if (dataFile == null || dataFile.getSource() == null)
      throw new NullPointerException("The source is null.");

    final String protocolName = dataFile.getProtocolPrefixInSource();

    if (protocolName == null)
      return new File(dataFile.getSource()).getAbsoluteFile();

    return new File(dataFile.getSource().substring(protocolName.length() + 1));
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return FileUtils.createInputStream(getSourceAsFile(src));
  }

  @Override
  public OutputStream putData(final DataFile src) throws IOException {

    return FileUtils.createOutputStream(getSourceAsFile(src));
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src))
      throw new FileNotFoundException("File not found: " + src);

    final File f = getSourceAsFile(src);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(f.length());
    result.setLastModified(f.lastModified());

    final DataFormat format =
        DataFormatRegistry.getInstance().getDataFormatFromFilename(
            src.getName());

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

    if (f.isDirectory())
      result.setDirectory(true);

    return result;
  }

  @Override
  public boolean exists(final DataFile src) {

    return getSourceAsFile(src).exists();
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
