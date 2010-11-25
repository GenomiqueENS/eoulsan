package fr.ens.transcriptome.eoulsan.datatypes.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.datatypes.DataFile;
import fr.ens.transcriptome.eoulsan.datatypes.DataFileMetadata;

/**
 * This interface define a protocol.
 * @author Laurent Jourdren
 */
public interface DataProtocol {

  /**
   * Get Protocol name.
   * @return the name of the protocol
   */
  String getName();

  /**
   * Create an InputStream from the source.
   * @param src source to use
   * @return an InputStream
   * @throws IOException if an error occurs while creating the InputStream
   */
  InputStream getData(DataFile src) throws IOException;

  /**
   * Create an OutputStream from the source.
   * @param src source to use
   * @return an OutputStream
   * @throws IOException if an error occurs while creating the OutputStream
   */
  OutputStream putData(DataFile dest) throws IOException;

  /**
   * Create an OutputStream from the source.
   * @param dest source to use
   * @param md metadata for the stream to write
   * @return an OutputStream
   * @throws IOException if an error occurs while creating the OutputStream
   */
  OutputStream putData(DataFile dest, DataFileMetadata md) throws IOException;

  /**
   * Copy data from a source to a destination source
   * @param src source source
   * @param dest destination source
   * @throws IOException if an error occurs while copying data
   */
  void putData(DataFile src, DataFile dest) throws IOException;

  /**
   * Test a source exists.
   * @param src source to use
   * @return true if the source exists
   */
  boolean exists(DataFile src);

  /**
   * Get the metadata for the source.
   * @param src source to use
   * @return always a metadataObject
   * @throws IOException if an error occurs while getting metadata
   */
  DataFileMetadata getMetadata(DataFile src) throws IOException;

  /**
   * Test if source is readable with this protocol.
   * @return true if the source is readable
   */
  boolean isReadable();

  /**
   * Test if source is writable with this protocol.
   * @return true if the source is writable
   */
  boolean isWritable();

}
