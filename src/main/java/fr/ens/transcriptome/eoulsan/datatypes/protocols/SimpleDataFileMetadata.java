package fr.ens.transcriptome.eoulsan.datatypes.protocols;

import fr.ens.transcriptome.eoulsan.datatypes.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormat;


class SimpleDataFileMetadata implements DataFileMetadata{

  private long contentLength = -1;
  private String contentType;
  private String contentEncoding;
  private String contentMD5;
  private long lastModified = -1;
  private DataFormat dataFormat;

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
  
  //
  // Constructors
  //
  
  public SimpleDataFileMetadata() {
  }
  
  public SimpleDataFileMetadata(final DataFileMetadata md) {
    
    if (md==null)
      return;
    
    setContentLength(md.getContentLength());
    setContentType(md.getContentType());
    setContentEncoding(md.getContentEncoding());
    setContentMD5(md.getContentMD5());
    setLastModified(md.getLastModified());
  }
  
}
