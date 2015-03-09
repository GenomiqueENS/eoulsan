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

import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This class define a simple implementation of the DataFileMetadata interface.
 * @since 1.0
 * @author Laurent Jourdren
 */
class SimpleDataFileMetadata implements DataFileMetadata {

  private long contentLength = -1;
  private String contentType;
  private String contentEncoding;
  private String contentMD5;
  private long lastModified = -1;
  private DataFormat dataFormat;
  private boolean directory;
  private boolean symbolicLink;

  //
  // Getters
  //

  @Override
  public long getContentLength() {

    return this.contentLength;
  }

  @Override
  public String getContentType() {

    return this.contentType;
  }

  @Override
  public String getContentEncoding() {

    return this.contentEncoding;
  }

  @Override
  public String getContentMD5() {

    return this.contentMD5;
  }

  @Override
  public long getLastModified() {

    return this.lastModified;
  }

  @Override
  public DataFormat getDataFormat() {

    return this.dataFormat;
  }

  @Override
  public boolean isDir() {

    return this.directory;
  }

  @Override
  public boolean isSymbolicLink() {

    return this.symbolicLink;
  }

  //
  // Setters
  //

  public void setContentLength(final long contentLength) {

    this.contentLength = contentLength;
  }

  public void setContentType(final String contentType) {

    this.contentType = contentType;
  }

  public void setContentEncoding(final String contentEncoding) {

    this.contentEncoding = contentEncoding;
  }

  public void setContentMD5(final String contentMD5) {

    this.contentMD5 = contentMD5;
  }

  public void setLastModified(final long lastModified) {

    this.lastModified = lastModified;
  }

  public void setDataFormat(final DataFormat dataFormat) {

    this.dataFormat = dataFormat;
  }

  public void setDirectory(final boolean directory) {

    this.directory = directory;
  }

  public void setSymbolicLink(final boolean symbolicLink) {

    this.symbolicLink = symbolicLink;
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{contentLength=" + this.contentLength + ", contentType="
        + this.contentType + ", contentEncoding=" + this.contentEncoding
        + ", contentMD5=" + this.contentMD5 + ", lastModified="
        + this.lastModified + ", dataFormat=" + this.dataFormat
        + ", directory=" + this.directory + ", symbolicLink=" + symbolicLink
        + "}";
  }

  //
  // Constructors
  //

  public SimpleDataFileMetadata() {
  }

  public SimpleDataFileMetadata(final DataFileMetadata md) {

    if (md == null) {
      return;
    }

    setContentLength(md.getContentLength());
    setContentType(md.getContentType());
    setContentEncoding(md.getContentEncoding());
    setContentMD5(md.getContentMD5());
    setLastModified(md.getLastModified());
  }

}
