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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.util.r;

import static java.util.Objects.requireNonNull;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RserveException;

import fr.ens.biologie.genomique.kenetre.io.FileUtils;

/**
 * This class define an enhanced connection to RServe.
 * @author Laurent Jourdren
 * @author Marion Gaussen
 * @since 1.2
 */
public class RSConnection {

  private final String serverName;
  private RConnection rconnection;

  private static final int BUFFER_SIZE = 32 * 1024;

  //
  // Getters
  //

  /**
   * Get the R connection.
   * @return Returns the RConnection
   * @throws REngineException if an error occurs while connecting to the server
   */
  public RConnection getRConnection() throws REngineException {

    if (this.rconnection == null) {
      connect();
    }

    return this.rconnection;
  }

  /**
   * Get the name of the Rserve server.
   * @return the name of the Rserve server
   */
  public String getServerName() {
    return this.serverName;
  }

  //
  // Other methods
  //

  /**
   * Write a file to the RServer.
   * @param outputFilename the filename
   * @param value The content of the file
   * @throws REngineException if an error occurs while writing the file
   */
  public void writeStringAsFile(final String outputFilename, final String value)
      throws REngineException {

    if (outputFilename == null) {
      return;
    }

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

  /**
   * Create an inputStream on a file on RServer.
   * @param filename Name of the file on RServer to load
   * @return an inputStream
   * @throws REngineException if an exception occurs while reading file
   */
  public InputStream getFileInputStream(final String filename)
      throws REngineException {

    final RConnection c = getRConnection();

    try {
      return c.openFile(filename);
    } catch (IOException e) {

      throw new REngineException(c, "Error: " + e.getMessage());
    }

  }

  /**
   * Create an outputStream on a file on RServer.
   * @param filename Name of the file on RServer to write
   * @return an outputStream
   * @throws REngineException if an exception occurs while reading file
   */
  public OutputStream getFileOutputStream(final String filename)
      throws REngineException {

    final RConnection c = getRConnection();

    try {
      return c.createFile(filename);
    } catch (IOException e) {

      throw new REngineException(c, "Error: " + e.getMessage());
    }

  }

  /**
   * Put a file from the RServer.
   * @param inputFile input file of the file to put
   * @param rServeFilename filename of the file to put
   * @throws REngineException if an error occurs while downloading the file
   */
  public void putFile(final Path inputFile, final String rServeFilename)
      throws REngineException {

    requireNonNull(inputFile, "inputFile argument cannot be null");
    requireNonNull(rServeFilename, "rServeFilename argument cannot be null");

    try {
      putFile(Files.newInputStream(inputFile), rServeFilename);
    } catch (IOException e) {
      throw new REngineException(getRConnection(),
          "file not found: " + e.getMessage());
    }
  }

  /**
   * Put a file from the RServer.
   * @param is input stream of the file to put
   * @param rServeFilename filename of the file to put
   * @throws REngineException if an error occurs while downloading the file
   */
  public void putFile(final InputStream is, final String rServeFilename)
      throws REngineException {

    requireNonNull(is, "inputFile argument cannot be null");
    requireNonNull(rServeFilename, "rServeFilename argument cannot be null");

    try {

      OutputStream os = getFileOutputStream(rServeFilename);

      byte[] buf = new byte[BUFFER_SIZE];
      int i = 0;

      while ((i = is.read(buf)) != -1) {
        os.write(buf, 0, i);
      }

      is.close();
      os.close();

    } catch (REngineException e) {
      throw new REngineException(getRConnection(),
          "Unable to put file: " + e.getMessage());
    } catch (IOException e) {
      throw new REngineException(getRConnection(),
          "Unable to create report: " + e.getMessage());
    }

  }

  /**
   * Get a file from the RServer.
   * @param rServeFilename filename of the file to retrieve
   * @param outputFile output file of the file to retrieve
   * @throws REngineException if an error occurs while downloading the file
   */
  public void getFile(final String rServeFilename, final Path outputFile)
      throws REngineException {

    try (InputStream is = Files.newInputStream(Path.of(rServeFilename));
        OutputStream os = Files.newOutputStream(outputFile)) {

      byte[] buf = new byte[BUFFER_SIZE];
      int i = 0;

      while ((i = is.read(buf)) != -1) {
        os.write(buf, 0, i);
      }

    } catch (FileNotFoundException e) {
      throw new REngineException(getRConnection(), "file not found");
    } catch (IOException e) {
      throw new REngineException(getRConnection(), "Unable to create report.");
    }
  }

  /**
   * Get a list of files from the RServer.
   * @param rServeFilenames list of filenames of the files to retrieve
   * @param zipFile zip output file for the files to retrieve
   * @throws REngineException if an error occurs while downloading the file
   */
  public void getFilesIntoZip(final List<String> rServeFilenames,
      final Path zipFile) throws REngineException {

    try (ZipOutputStream out =
        new ZipOutputStream(Files.newOutputStream(zipFile))) {

      final byte[] buf = new byte[BUFFER_SIZE];

      for (String f : rServeFilenames) {
        try (InputStream is = getFileInputStream(f)) {

          // Add Zip entry to output stream.
          out.putNextEntry(new ZipEntry(f));

          int i = 0;

          while ((i = is.read(buf)) != -1) {
            out.write(buf, 0, i);
          }

          // Complete the entry
          out.closeEntry();
        }
      }

      // Complete the Zip file
      // out.close();

    } catch (REngineException e) {
      throw new REngineException(getRConnection(), "Unable to get file");
    } catch (FileNotFoundException e) {
      throw new REngineException(getRConnection(), "File not found");
    } catch (IOException e) {
      throw new REngineException(getRConnection(), "Unable to get file");
    }
  }

  /**
   * Remove a file on the RServer.
   * @param filename File to remove
   * @throws REngineException if connection cannot be established
   */
  public void removeFile(final String filename) throws REngineException {

    // Test if the file exists
    final RConnection c = getRConnection();

    try {

      REXP exists = c.eval("file.exists(\"" + filename + "\")");
      if (exists.asInteger() == 1) {
        c.voidEval("file.remove(\"" + filename + "\")");
      }

    } catch (RserveException | REXPMismatchException e) {
      throw new REngineException(c, "RServe exception: " + e);
    }
  }

  /**
   * Remove all the files of the working directory.
   * @throws REngineException if an error occurs while removing the file
   * @throws REXPMismatchException if an error occurs while removing the file
   */
  public void removeAllFiles() throws REngineException, REXPMismatchException {

    for (String file : listFiles()) {
      removeFile(file);
    }
  }

  /**
   * Override the commandArg() R function.
   * @param arguments the arguments
   * @throws REngineException if an error occurs while executing R code
   */
  public void setCommandArgs(final List<String> arguments)
      throws REngineException {

    if (arguments == null) {
      throw new NullPointerException("arguments argument cannot be null");
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("f <- function(trailingOnly = FALSE) { c(");

    boolean first = true;
    for (String arg : arguments) {

      if (first) {
        first = false;
      } else {
        sb.append(",");
      }
      sb.append('\'');
      sb.append(arg);
      sb.append('\'');
    }

    sb.append(") }");

    final RConnection c = getRConnection();

    try {

      // Execute the source
      c.voidEval(sb.toString());

    } catch (RserveException e) {
      throw new REngineException(c, "RServe exception: " + e);
    }
  }

  /**
   * Execute a R code.
   * @param source code to execute
   * @throws REngineException if an error while executing the code
   */
  public void executeRCode(final String source) throws REngineException {

    if (source == null) {
      return;
    }

    executeR("source(\"" + source + "\")");
  }

  /**
   * Execute a R code.
   * @param code code to execute
   * @throws REngineException if an error while executing the code
   */
  public void executeR(final String code) throws REngineException {

    if (code == null) {
      return;
    }

    final RConnection c = getRConnection();

    try {

      // Execute the source
      c.voidEval(code);

    } catch (RserveException e) {
      throw new REngineException(c, "RServe exception: " + e);
    }
  }

  /**
   * Execute a R Sweave code.
   * @param source code to execute
   * @throws REngineException if an error while executing the code
   */
  public void executeRnwCode(final String source) throws REngineException {

    executeRnwCode(source, null);
  }

  /**
   * Execute a R Sweave code.
   * @param source code to execute
   * @param latexOutput output latex filename
   * @throws REngineException if an error while executing the code
   */
  public void executeRnwCode(final String source, final String latexOutput)
      throws REngineException {

    if (source == null) {
      return;
    }

    final RConnection c = getRConnection();

    final StringBuilder sb = new StringBuilder();
    sb.append("Sweave(\"");
    sb.append(source);
    sb.append('\"');

    if (latexOutput != null) {
      sb.append(", output=\"");
      sb.append(latexOutput);
      sb.append('\"');
    }
    sb.append(')');

    try {
      c.voidEval(sb.toString());
    } catch (RserveException e) {
      throw new REngineException(c, "Rserve exception: " + e);
    }
  }

  /**
   * Load an image.
   * @param filename filename of the image on the server
   * @return an Image object
   * @throws REngineException if an error while loading the image
   */
  public Image loadImage(final String filename) throws REngineException {

    if (filename == null) {
      return null;
    }

    final RConnection c = getRConnection();

    if (c == null) {
      throw new REngineException(null, "Connection is null");
    }

    try {
      RFileInputStream is = c.openFile(filename);
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
        if (n > 0) {
          imgLength += n;
        }
        if (n < bufSize) {
          break;
        }
      }
      if (imgLength < 10) { // this shouldn't be the case actually,
        // because we did some error checking, but
        // for those paranoid ...
        throw new REngineException(c,
            "Cannot load image, check R output, probably R didn't produce anything.");

      }

      // now let's join all the chunks into one, big array ...
      byte[] imgCode = new byte[imgLength];
      int imgPos = 0;

      for (byte[] b : buffers) {
        System.arraycopy(b, 0, imgCode, imgPos, bufSize);
        imgPos += bufSize;
      }
      if (n > 0) {
        System.arraycopy(buf, 0, imgCode, imgPos, n);
      }

      // ... and close the file ... and remove it - we have what we need :)
      is.close();
      c.removeFile("test.jpg");

      // now this is pretty boring AWT stuff, nothing to do with R ...
      return Toolkit.getDefaultToolkit().createImage(imgCode);
    } catch (IOException e) {
      throw new REngineException(c, "Error while load image");
    } catch (RserveException e) {
      throw new REngineException(c, "Error while removing image from server");
    }

  }

  /**
   * Get a file as a byte array.
   * @param filename filename of the file on the server
   * @return a byte array
   * @throws REngineException if an error while loading the file
   */
  public byte[] getFileAsArray(final String filename) throws REngineException {

    final RConnection c = getRConnection();

    if (c == null) {
      throw new REngineException(null, "Connection is null");
    }

    try {
      RFileInputStream is = c.openFile(filename);
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
        if (n > 0) {
          imgLength += n;
        }
        if (n < bufSize) {
          break;
        }
      }
      if (imgLength < 10) {
        throw new REngineException(c,
            "Cannot load files, check R output, probably R didn't produce anything.");

      }

      byte[] imgCode = new byte[imgLength];
      int imgPos = 0;

      for (byte[] b : buffers) {
        System.arraycopy(b, 0, imgCode, imgPos, bufSize);
        imgPos += bufSize;
      }
      if (n > 0) {
        System.arraycopy(buf, 0, imgCode, imgPos, n);
      }

      is.close();

      return imgCode;

    } catch (IOException e) {
      throw new REngineException(c, "Error while loading files");
    }

  }

  /**
   * Open a file to read on Rserve server.
   * @param filename the filename
   * @return an input stream
   * @throws REngineException if an error occurs while creating the input stream
   */
  public RFileInputStream openFile(final String filename)
      throws REngineException {

    final RConnection c = getRConnection();

    RFileInputStream file;
    try {
      file = c.openFile(filename);
    } catch (IOException e) {
      throw new REngineException(c, "Error while opening file");
    }

    return file;
  }

  /**
   * Get all the file on the Rserve server.
   * @param outPath the output path
   * @throws REngineException if an error occurs while retrieving the files
   * @throws REXPMismatchException if an error occurs while retrieving the files
   */
  public void getAllFiles(final Path outPath)
      throws REngineException, REXPMismatchException {

    if (outPath == null) {
      throw new NullPointerException("outPath argument cannot be null");
    }

    for (String file : listFiles()) {
      getFile(file, outPath.resolve(file));
    }
  }

  /**
   * List files on Rserve server.
   * @return files a String list of files names
   * @throws REngineException if an error occurs with the server
   * @throws REXPMismatchException if an error occurs with the server
   */
  public List<String> listFiles()
      throws REngineException, REXPMismatchException {

    final RConnection c = getRConnection();

    String[] files = c.eval("list.files()").asStrings();

    if (files == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(files);
  }

  /**
   * Connect to the Rserve server.
   * @throws REngineException if an error occurs while connecting to the server
   */
  private void connect() throws REngineException {

    try {
      this.rconnection = new RConnection(this.serverName);
    } catch (RserveException e) {
      throw new REngineException(this.rconnection,
          "Unable to connect to the server: " + e.getMessage());
    }

  }

  /**
   * Destroy the connection to the Rserve server.
   */
  public void disConnect() {

    if (this.rconnection != null) {
      this.rconnection.close();
      this.rconnection = null;
    }
  }

  //
  // Constructor
  //

  /**
   * Default constructor. Connect to the localhost.
   */
  public RSConnection() {
    this(null);
  }

  /**
   * Public constructor.
   * @param serverName RServe server to use
   */
  public RSConnection(final String serverName) {

    this.serverName = serverName == null ? "127.0.0.1" : serverName.trim();
  }

}
