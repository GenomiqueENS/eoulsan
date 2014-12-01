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

package fr.ens.transcriptome.eoulsan.util.r;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RserveException;

import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an enhanced connection to RServe.
 * @author Laurent Jourdren
 * @author Marion Gaussen
 * @since 1.2
 */
public class RSConnectionNewImpl implements RSConnection {

  private String serverName;
  private RConnection rconnection;

  private static final int BUFFER_SIZE = 32 * 1024;

  //
  // Getters
  //

  /**
   * Get the R connection.
   * @return Returns the RConnection
   * @throws REngineException
   */
  public RConnection getRConnection() throws REngineException {

    if (this.rconnection == null)
      connect();

    return rconnection;
  }

  public String getServerName() {
    return this.serverName;
  }

  //
  // Other methods
  //

  public void writeStringAsFile(final String outputFilename, final String value)
      throws REngineException {

    if (outputFilename == null)
      return;

    try {

      final Writer writer =
          FileUtils.createBufferedWriter(getFileOutputStream(outputFilename));
      if (value != null) {
        writer.write(value);
        writer.close();
      }
    } catch (IOException e) {
      throw new REngineException(getRConnection(), "Error: " + e.getMessage());
    }

  }

  public InputStream getFileInputStream(final String filename)
      throws REngineException {

    RConnection c = getRConnection();

    try {
      return c.openFile(filename);
    } catch (IOException e) {

      throw new REngineException(c, "Error: " + e.getMessage());
    }

  }

  public OutputStream getFileOutputStream(final String filename)
      throws REngineException {

    RConnection c = getRConnection();

    try {
      return c.createFile(filename);
    } catch (IOException e) {

      throw new REngineException(c, "Error: " + e.getMessage());
    }

  }

  /**
   * Put file on Rserve server
   */
  public void putFile(final File inputFile, final String rServeFilename)
      throws REngineException {

    try {

      InputStream is = new FileInputStream(inputFile);
      OutputStream os = getFileOutputStream(rServeFilename);

      byte[] buf = new byte[BUFFER_SIZE];
      int i = 0;

      while ((i = is.read(buf)) != -1)
        os.write(buf, 0, i);

      is.close();
      os.close();

    } catch (REngineException e) {
      throw new REngineException(rconnection, "Unable to put file: "
          + e.getMessage());
    } catch (FileNotFoundException e) {
      throw new REngineException(rconnection, "file not found: "
          + e.getMessage());
    } catch (IOException e) {
      throw new REngineException(rconnection, "Unable to create report: "
          + e.getMessage());
    }

  }

  public void getFile(final String rServeFilename, final File outputFile)
      throws REngineException {

    try {
      InputStream is = getFileInputStream(rServeFilename);
      OutputStream os = new FileOutputStream(outputFile);

      byte[] buf = new byte[BUFFER_SIZE];
      int i = 0;

      while ((i = is.read(buf)) != -1)
        os.write(buf, 0, i);

      is.close();
      os.close();

    } catch (REngineException e) {
      throw new REngineException(rconnection, "Unable to get file");
    } catch (FileNotFoundException e) {
      throw new REngineException(rconnection, "file not found");
    } catch (IOException e) {
      throw new REngineException(rconnection, "Unable to create report.");
    }
  }

  public void getFilesIntoZip(final List<String> rServeFilenames,
      final File zipFile) throws REngineException {

    try {
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

      final byte[] buf = new byte[BUFFER_SIZE];

      for (String f : rServeFilenames) {
        final InputStream is = getFileInputStream(f);

        // Add Zip entry to output stream.
        out.putNextEntry(new ZipEntry(f));

        int i = 0;

        while ((i = is.read(buf)) != -1)
          out.write(buf, 0, i);

        // Complete the entry
        out.closeEntry();
        is.close();
      }

      // Complete the Zip file
      out.close();

    } catch (REngineException e) {
      throw new REngineException(rconnection, "Unable to get file");
    } catch (FileNotFoundException e) {
      throw new REngineException(rconnection, "File not found");
    } catch (IOException e) {
      throw new REngineException(rconnection, "Unable to get file");
    }
  }

  public void removeFile(final String filename) throws REngineException {

    try {
      // Test if the file exists
      RConnection c = getRConnection();

      REXP exists = c.eval("file.exists(\"" + filename + "\")");
      if (exists.asInteger() == 1)

        c.voidEval("file.remove(\"" + filename + "\")");

    } catch (RserveException | REXPMismatchException e) {
      throw new REngineException(rconnection, "RServe exception: " + e);
    }
  }

  public void removeAllFiles() throws REngineException, REXPMismatchException {

    String[] files = getFileList();

    for (String file : files) {
      removeFile(file);
    }

  }

  public void executeRCode(final String source) throws REngineException {

    if (source == null)
      return;

    try {

      RConnection c = getRConnection();

      // Execute the source
      c.voidEval("source(\"" + source + "\")");

    } catch (RserveException e) {

      throw new REngineException(rconnection, "RServe exception: " + e);
    }
  }

  public void executeRnwCode(final String source) throws REngineException {

    if (source == null)
      return;

    try {
      RConnection rc = getRConnection();
      rc.voidEval("Sweave(\"" + source + "\")");
    } catch (RserveException e) {
      throw new REngineException(getRConnection(), "Rserve exeption: " + e);
    }
  }

  public Image loadImage(final String filename) throws REngineException {

    if (filename == null)
      return null;

    final RConnection connection = getRConnection();

    if (connection == null)
      throw new REngineException(null, "Connection is null");

    try {
      RFileInputStream is = connection.openFile(filename);
      ArrayList<byte[]> buffers = new ArrayList<>();
      int bufSize = 65536;
      byte[] buf = new byte[bufSize];
      int imgLength = 0;
      int n = 0;
      while (true) {
        n = is.read(buf);
        if (n == bufSize) {
          buffers.add(buf);
          buf = new byte[bufSize];
        }
        if (n > 0)
          imgLength += n;
        if (n < bufSize)
          break;
      }
      if (imgLength < 10) { // this shouldn't be the case actually,
        // because we did some error checking, but
        // for those paranoid ...
        throw new REngineException(connection,
            "Cannot load image, check R output, probably R didn't produce anything.");

      }

      // now let's join all the chunks into one, big array ...
      byte[] imgCode = new byte[imgLength];
      int imgPos = 0;

      final int nbBuffers = buffers.size();

      for (byte[] b : buffers) {
        System.arraycopy(b, 0, imgCode, imgPos, bufSize);
        imgPos += bufSize;
      }
      if (n > 0)
        System.arraycopy(buf, 0, imgCode, imgPos, n);

      // ... and close the file ... and remove it - we have what we need :)
      is.close();
      connection.removeFile("test.jpg");

      // now this is pretty boring AWT stuff, nothing to do with R ...
      Image img = Toolkit.getDefaultToolkit().createImage(imgCode);

      return img;

    } catch (IOException e) {
      throw new REngineException(connection, "Error while load image");
    } catch (RserveException e) {
      throw new REngineException(connection,
          "Error while removing image from server");
    }

  }

  public byte[] getFileAsArray(final String filename) throws REngineException {

    final RConnection connection = getRConnection();

    if (connection == null)
      throw new REngineException(null, "Connection is null");

    try {
      RFileInputStream is = connection.openFile(filename);
      ArrayList<byte[]> buffers = new ArrayList<>();

      int bufSize = 65536;
      byte[] buf = new byte[bufSize];

      int imgLength = 0;
      int n = 0;
      while (true) {
        n = is.read(buf);
        if (n == bufSize) {
          buffers.add(buf);
          buf = new byte[bufSize];
        }
        if (n > 0)
          imgLength += n;
        if (n < bufSize)
          break;
      }
      if (imgLength < 10) {
        throw new REngineException(connection,
            "Cannot load files, check R output, probably R didn't produce anything.");

      }

      byte[] imgCode = new byte[imgLength];
      int imgPos = 0;

      final int nbBuffers = buffers.size();

      for (byte[] b : buffers) {
        System.arraycopy(b, 0, imgCode, imgPos, bufSize);
        imgPos += bufSize;
      }
      if (n > 0)
        System.arraycopy(buf, 0, imgCode, imgPos, n);

      is.close();

      return imgCode;

    } catch (IOException e) {
      throw new REngineException(connection, "Error while loading files");
    }

  }

  /**
   * Open a file to read on Rserve server
   * @param filename
   * @return file
   * @throws REngineException
   */
  public RFileInputStream openFile(String filename) throws REngineException {

    final RConnection connection = getRConnection();
    RFileInputStream file;
    try {
      file = this.rconnection.openFile(filename);
    } catch (IOException e) {
      throw new REngineException(connection, "Error while opening file");
    }

    return file;
  }

  public void getAllFiles(String outPath) throws REngineException,
      REXPMismatchException {
    String[] files = getFileList();

    for (String file : files) {
      getFile(file, new File(outPath + file));
    }
  }

  /**
   * List files on Rserve server
   * @return files a String list of files names
   * @throws REngineException
   * @throws REXPMismatchException
   */
  private String[] getFileList() throws REngineException, REXPMismatchException {
    RConnection connection = getRConnection();
    String[] files;
    files = connection.eval("list.files()").asStrings();
    return files;
  }

  private void connect() throws REngineException {

    try {
      this.rconnection = new RConnection(this.serverName);
    } catch (RserveException e) {
      throw new REngineException(this.rconnection,
          "Unable to connect to the server: " + e.getMessage());
    }

  }

  public void disConnect() {

    this.rconnection.close();
  }

  //
  // Constructor
  //

  /**
   * Default constructor. Connect to the localhost.
   */
  public RSConnectionNewImpl() {
    this(null);
  }

  /**
   * Public constructor.
   * @param serverName RServe server to use
   */
  public RSConnectionNewImpl(final String serverName) {

    this.serverName = serverName == null ? "127.0.0.1" : serverName.trim();
  }

}
