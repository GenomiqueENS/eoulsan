/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.r;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.rosuda.JRclient.RBool;
import org.rosuda.JRclient.REXP;
import org.rosuda.JRclient.RFileInputStream;
import org.rosuda.JRclient.RList;
import org.rosuda.JRclient.RSrvException;
import org.rosuda.JRclient.Rconnection;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;

/**
 * This class define an enhanced connection to RServe.
 * @author Laurent Jourdren
 * @author Marion Gaussen
 */
public class RSConnection {

  private String serverName;
  private Rconnection rConnection;
  private String sourceDirectory = "/tmp/rspsources";

  private static final int BUFFER_SIZE = 32 * 1024;

  //
  // Getters
  //

  /**
   * Get the R connection.
   * @return Returns the rConnection
   * @throws RSException
   */
  protected Rconnection getRConnection() throws RSException {

    if (this.rConnection == null)
      connect();

    return rConnection;
  }

  /**
   * Get the source directory
   * @return Returns the sourceDirectory
   */
  public String getSourceDirectory() {
    return sourceDirectory;
  }

  //
  // Setters
  //

  /**
   * Set the source directory
   * @param sourceDirectory The sourceDirectory to set
   */
  public void setSourceDirectory(final String sourceDirectory) {
    this.sourceDirectory = sourceDirectory;
  }

  //
  // Other methods
  //

  /**
   * Assign data to an R variable.
   * @param varName Name of the R variable
   * @param value value of the R variable
   * @throws if an error occurs while assigning data
   */
  public void assign(final String varName, final Object value)
      throws RSException {

    // System.out.println("value=" + value);

    try {

      final Rconnection c = getRConnection();
      if (varName == null || value == null)
        return;

      if (value instanceof Integer)
        c.assign(varName, new int[] {((Integer) value).intValue()});

      if (value instanceof Double)
        c.assign(varName, new double[] {((Double) value).doubleValue()});

      if (value instanceof String)
        c.assign(varName, (String) value);

      if (value instanceof RList)
        c.assign(varName, new REXP(REXP.XT_LIST, value));

      if (value instanceof RBool)
        c.assign(varName, new REXP(REXP.XT_BOOL, value));

      if (value instanceof Boolean) {

        System.out.println("this is a boolean");

        REXP exp =
            new REXP(REXP.XT_BOOL, new RBool(((Boolean) value).booleanValue()));

        System.out.println("exp=" + exp.asBool());

        c.assign(varName, exp);
      }

      if (value instanceof Vector)
        c.assign(varName, new REXP(REXP.XT_VECTOR, value));

      if (value instanceof List || value instanceof Set) {

        Vector v = new Vector();
        v.addAll((Collection) value);

        System.out.println(v.getClass().getName() + "\t" + v.size() + "\t" + v);

        c.assign(varName, new REXP(REXP.XT_VECTOR, v));
      }

      if (value instanceof int[])
        c.assign(varName, (int[]) value);

      if (value instanceof double[])
        c.assign(varName, (double[]) value);

      if (value instanceof String[])
        c.assign(varName, new REXP(REXP.XT_ARRAY_STR, value));

      if (value instanceof RBool[])
        c.assign(varName, new REXP(REXP.XT_ARRAY_BOOL, value));

    } catch (RSrvException e) {
      e.printStackTrace();
      throw new RSException("Error: " + e.getMessage());

    }

  }

  /**
   * Eval an R variable
   * @param varName R variable
   * @return an int variable
   * @throws RSrvException if an error occurs while evaluating the value
   */
  public int evalAsInt(final String varName) throws RSException {

    try {
      final Rconnection c = getRConnection();

      return c.eval(varName).asInt();

    } catch (RSrvException e) {

      throw new RSException("Error: " + e.getMessage());
    }
  }

  /**
   * Eval an R variable
   * @param varName R variable
   * @return a double variable
   * @throws RSrvException if an error occurs while evaluating the value
   */
  public double evalAsDouble(final String varName) throws RSException {

    try {

      final Rconnection c = getRConnection();
      return c.eval(varName).asDouble();

    } catch (RSrvException e) {

      if (e.getRequestReturnCode() == 127)
        return Double.NaN;

      // System.out.println(e.getRequestErrorDescription());

      e.printStackTrace();
      throw new RSException("Error: " + e.getMessage());
    }
  }

  /**
   * Eval an R variable
   * @param varName R variable
   * @return a REXP variable
   * @throws RSrvException if an error occurs while evaluating the value
   */
  public REXP evalAsREXP(final String varName) throws RSException {

    try {

      final Rconnection c = getRConnection();
      return c.eval(varName);

    } catch (RSrvException e) {

      throw new RSException("Error: " + e.getMessage());
    }
  }

  /**
   * Eval an R variable
   * @param varName R variable
   * @return an object
   * @throws RSrvException if an error occurs while evaluating the value
   */
  public Object eval(final String varName) throws RSException {

    try {
      // System.out.println("eval=" + varName);
      final Rconnection c = getRConnection();
      return eval(c.eval(varName));

    } catch (RSrvException e) {

      throw new RSException("Error: " + e.getMessage());
    }
  }

  /**
   * Eval a REXP object
   * @param exp Object to eval
   * @return an object
   * @throws RSrvException if an error occurs while evaluating the value
   */
  public Object eval(final REXP exp) {

    if (exp == null)
      return null;

    switch (exp.getType()) {
    case REXP.XT_NULL:
      return null;

    case REXP.XT_INT:
      return exp.asInt();

    case REXP.XT_DOUBLE:
      return new Double(exp.asDouble());

    case REXP.XT_STR:
      return exp.asString();

    case REXP.XT_LANG:
      return exp.asList();

    case REXP.XT_SYM:
      return exp.asString();

    case REXP.XT_BOOL:
      return exp.asBool();

    case REXP.XT_VECTOR:
      return exp.asVector();

    case REXP.XT_LIST:
      return exp.asList();

    case REXP.XT_CLOS:
      return null;

    case REXP.XT_ARRAY_INT:
      return exp.asIntArray();

    case REXP.XT_ARRAY_DOUBLE:
      return exp.asDoubleArray();

    case REXP.XT_ARRAY_STR:
      return null;

    case REXP.XT_ARRAY_BOOL:
      return null;

    default:
      return null;

    }

  }

  private void testAndCreateSourceDirectory() throws RSException {

    try {
      Rconnection c = getRConnection();
      if (c == null)
        throw new RSException("Connection is null");

      REXP exists = c.eval("file.exists(\"" + getSourceDirectory() + "\")");

      if (exists.asBool().isFALSE()) {

        c.voidEval("dir.create(\"" + getSourceDirectory() + "\")");
        exists = c.eval("file.exists(\"" + getSourceDirectory() + "\")");

        if (exists.asBool().isFALSE())
          throw new RSException("Enable to create source directory");
      }
    } catch (RSrvException e) {
      throw new RSException("RServe exception: " + e);
    }

  }

  /**
   * Write a file to the RServer
   * @param outputFilename the filename
   * @param value The content of the file
   * @throws RSException if an error occurs while writing the file
   */
  public void writeStringAsFile(final String outputFilename, final String value)
      throws RSException {

    if (outputFilename == null)
      return;

    PrintWriter pw = new PrintWriter(getFileOutputStream(outputFilename));
    if (value != null)
      pw.write(value);
    pw.close();

  }

  /**
   * Create an inputStream on a file on RServer.
   * @param filename Name of the file on RServer to load
   * @return an inputStream
   * @throws RSException if an exception occurs while reading file
   */
  public InputStream getFileInputStream(final String filename)
      throws RSException {

    Rconnection c = getRConnection();

    try {
      return c.openFile(filename);
    } catch (IOException e) {

      throw new RSException("Error: " + e.getMessage());
    }

  }

  /**
   * Create an outputStream on a file on RServer.
   * @param filename Name of the file on RServer to write
   * @return an outputStream
   * @throws RSException if an exception occurs while reading file
   */
  public OutputStream getFileOutputStream(final String filename)
      throws RSException {

    Rconnection c = getRConnection();

    try {
      return c.createFile(filename);
    } catch (IOException e) {

      throw new RSException("Error: " + e.getMessage());
    }

  }

  /**
   * Put a file from the RServer.
   * @param rServeFilename filename of the file to put
   * @param outputfile output file of the file to put
   * @throws RSException if an error occurs while downloading the file
   */
  public void putFile(final File inputFile, final String rServeFilename)
      throws RSException {

    try {

      InputStream is = new FileInputStream(inputFile);
      OutputStream os = getFileOutputStream(rServeFilename);

      byte[] buf = new byte[BUFFER_SIZE];
      int i = 0;

      while ((i = is.read(buf)) != -1)
        os.write(buf, 0, i);

      is.close();
      os.close();

    } catch (RSException e) {
      throw new RSException("Unable to get the normalized bioAssay");
    } catch (FileNotFoundException e) {
      throw new RSException("Unable to create report.");
    } catch (IOException e) {
      throw new RSException("Unable to create report.");
    }

  }

  /**
   * Get a file from RServe
   * @param filename file to load
   * @return a file object
   * @throws RSException if an error occurs while loading the file
   */
  public File getTable(final String filename) throws RSException {

    final Rconnection connection = getRConnection();

    if (connection == null)
      throw new RSException("Connection is null");

    try {

      File input = new File(filename);

      FileReader fluxLectureTexte = new FileReader(input);
      BufferedReader bufferreader = new BufferedReader(fluxLectureTexte);

      String ligne;
      StringBuffer content = new StringBuffer();
      while ((ligne = bufferreader.readLine()) != null) {
        System.out.print(ligne);
        content.append(ligne);
        content.append("\r\n");
      }

      bufferreader.close();
      fluxLectureTexte.close();

      File output = new File(filename);
      FileWriter fluxEcritureTexte = new FileWriter(output);
      BufferedWriter bufferwriter = new BufferedWriter(fluxEcritureTexte);
      bufferwriter.write(content.toString());
      bufferwriter.flush();
      bufferwriter.close();
      fluxEcritureTexte.close();

      return output;

    } catch (IOException e) {
      throw new RSException("Error while loading file");
    }

  }

  /**
   * Get a file from the RServer.
   * @param rServeFilename filename of the file to retrieve
   * @param outputfile output file of the file to retrieve
   * @throws RSException if an error occurs while downloading the file
   */
  public void getFile(final String rServeFilename, final File outputfile)
      throws RSException {

    try {
      InputStream is = getFileInputStream(rServeFilename);

      OutputStream fos = new FileOutputStream(outputfile);

      final byte[] buf = new byte[BUFFER_SIZE];
      int i = 0;

      while ((i = is.read(buf)) != -1)
        fos.write(buf, 0, i);

      is.close();
      fos.close();

    } catch (RSException e) {
      throw new RSException("Unable to get the file: " + rServeFilename);
    } catch (FileNotFoundException e) {
      throw new RSException("Unable to get the file: " + rServeFilename);
    } catch (IOException e) {
      throw new RSException("Unable to get the file: " + rServeFilename);
    }

  }

  /**
   * Get a list of files from the RServer.
   * @param rServeFilenames list of filenames of the files to retrieve
   * @param zipFile zip output file for the files to retrieve
   * @throws RSException if an error occurs while downloading the file
   */
  public void getFilesIntoZip(final List<String> rServeFilenames,
      final File zipFile) throws RSException {

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

    } catch (RSException e) {
      throw new RSException("Unable to get the normalized bioAssay");
    } catch (FileNotFoundException e) {
      throw new RSException("Unable to create report.");
    } catch (IOException e) {
      throw new RSException("Unable to create report.");
    }
  }

  /**
   * Remove a file on the RServer
   * @param filename File to remove
   */
  public void removeFile(final String filename) throws RSException {

    try {
      // Test if the file exists
      Rconnection c = getRConnection();

      REXP exists = c.eval("file.exists(\"" + filename + "\")");
      if (exists.asBool().isTRUE())

        c.voidEval("file.remove(\"" + filename + "\")");

    } catch (RSrvException e) {
      e.printStackTrace();
      throw new RSException("RServe exception: " + e);
    }
  }

  /**
   * Execute a R code.
   * @param source code to execute
   * @throws RSException if an error while executing the code
   */
  public void executeRCode(final String source) throws RSException {

    if (source == null)
      return;

    int hashCode = source.hashCode();
    String filename = getSourceDirectory() + "/" + hashCode + ".R";

    try {

      // Test if the file exists
      Rconnection c = getRConnection();
      REXP exists = c.eval("file.exists(\"" + filename + "\")");

      // Create the file on the server if doesn't exists
      // if (exists.asList().at(0).asBool().isFALSE()) {
      if (exists.asBool().isFALSE())
        writeStringAsFile(filename, source);

      // Execute the source
      c.voidEval("source(\"" + filename + "\")");

    } catch (RSrvException e) {
      e.printStackTrace();
      throw new RSException("RServe exception: " + e);
    }

  }

  /**
   * Load an image from RServe
   * @param filename file to load
   * @return an image object
   * @throws RSException if an error occurs while loading the image
   */
  public Image loadImage(final String filename) throws RSException {

    if (filename == null)
      return null;

    final Rconnection connection = getRConnection();

    if (connection == null)
      throw new RSException("Connection is null");

    try {
      RFileInputStream is = connection.openFile(filename);
      ArrayList<byte[]> buffers = new ArrayList<byte[]>();
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
        throw new RSRuntimeException(
            "Cannot load image, check R output, probably R didn't produce anything.");

      }
      System.out.println("The image file is " + imgLength + " bytes big.");

      // now let's join all the chunks into one, big array ...
      byte[] imgCode = new byte[imgLength];
      int imgPos = 0;

      final int nbBuffers = buffers.size();

      for (int i = 0; i < nbBuffers; i++) {
        byte[] b = buffers.get(i);
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
      throw new RSException("Error while load image");
    } catch (RSrvException e) {
      throw new RSException("Error while removing image from server");
    }

  }

  /**
   * Get file from RServe
   * @param filename file to load
   * @return an byte[] object
   * @throws RSException if an error occurs while loading the image
   */
  public byte[] getFileAsArray(final String filename) throws RSException {

    final Rconnection connection = getRConnection();

    if (connection == null)
      throw new RSException("Connection is null");

    try {
      RFileInputStream is = connection.openFile(filename);
      ArrayList<byte[]> buffers = new ArrayList<byte[]>();

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
        throw new RSRuntimeException(
            "Cannot load image, check R output, probably R didn't produce anything.");

      }
      System.out.println("The image file is " + imgLength + " bytes big.");

      byte[] imgCode = new byte[imgLength];
      int imgPos = 0;

      final int nbBuffers = buffers.size();

      for (int i = 0; i < nbBuffers; i++) {
        byte[] b = buffers.get(i);
        System.arraycopy(b, 0, imgCode, imgPos, bufSize);
        imgPos += bufSize;
      }
      if (n > 0)
        System.arraycopy(buf, 0, imgCode, imgPos, n);

      is.close();

      return imgCode;

    } catch (IOException e) {
      throw new RSException("Error while load image");
    }

  }

  /**
   * Show an image.
   * @param image Image to show
   */
  public static void showImage(final java.awt.Image image) {

    javax.swing.JFrame f = new javax.swing.JFrame("Test image");
    javax.swing.JLabel b =
        new javax.swing.JLabel(new javax.swing.ImageIcon(image));
    f.getContentPane().add(b);
    f.pack();
    f.setVisible(true);
  }

  /**
   * Create the connection to the Rserve server
   * @throws RSException if an error occurs while connecting to Rserve
   */
  private void connect() throws RSException {

    try {
      this.rConnection = new Rconnection(this.serverName);
    } catch (RSrvException e) {
      throw new RSException("Unable to connect to the server: "
          + e.getMessage());
    }

    testAndCreateSourceDirectory();
  }

  /**
   * Destroy the connection to the Rserve server
   * @throws RSException if an error occurs while deleting to Rserve
   */
  public void disConnect() {

    this.rConnection.close();
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

    if (serverName == null) {

      this.serverName =
          EoulsanRuntime.getRuntime().getSettings().getRServeServername();
      if (this.serverName == null)
        this.serverName = "127.0.0.1";

    } else
      this.serverName = serverName.trim();
  }

}
