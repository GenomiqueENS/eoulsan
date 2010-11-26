package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;

public abstract class URLDataProtocol extends AbstractDataProtocol {

  private URLConnection createConnection(final DataFile src) throws IOException {

    if (src == null)
      throw new NullPointerException("The source is null.");

    try {
      return new URL(src.getSource()).openConnection();
    } catch (MalformedURLException e) {
      throw new IOException("Invalid URL: " + src);
    }

  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return createConnection(src).getInputStream();
  }

  @Override
  public OutputStream putData(final DataFile src) throws IOException {

    return createConnection(src).getOutputStream();
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src))
      throw new FileNotFoundException("File not found: " + src);

    final URLConnection con = createConnection(src);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(con.getContentLength());
    result.setLastModified(con.getLastModified());
    result.setContentType(con.getContentType());
    result.setContentEncoding(con.getContentEncoding());

    return result;
  }

  @Override
  public boolean isReadable() {

    return true;
  }

  @Override
  public boolean isWritable() {

    return true;
  }

  @Override
  public boolean exists(final DataFile src) {

    try {
      createConnection(src);
    } catch (IOException e) {
      return false;
    }

    return false;
  }

}
