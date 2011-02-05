package fr.ens.transcriptome.eoulsan.data;

/**
 * This class define source metadata
 * @author Laurent Jourdren
 */
public interface DataFileMetadata {

  /**
   * Get the content length of the file.
   * @return the content length or -1 if unavailable
   */
  long getContentLength();

  /**
   * Get the content type of the file.
   * @return the content type or null if unavailable
   */
  String getContentType();

  /**
   * Get the content type of the file.
   * @return the content type or null if unavailable
   */
  String getContentEncoding();

  /**
   * Get the content MD5 of the file.
   * @return the content MD5 or null if unavailable
   */
  String getContentMD5();

  /**
   * Get the date of the last modification of the file.
   * @return the last modified date in seconds since epoch of -1 if unavailable
   */
  long getLastModified();

  /**
   * Get if the file is a directory.
   * @return true if the file is a directory
   */
  boolean isDir();

  /**
   * Get the DataFormat of the file.
   * @return the DataFormat of the source
   */
  DataFormat getDataFormat();

}
