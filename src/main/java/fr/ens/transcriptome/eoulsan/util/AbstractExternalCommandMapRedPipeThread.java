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

package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;

public abstract class AbstractExternalCommandMapRedPipeThread implements
    Runnable {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private Process process;
  private BufferedReader reader;
  private OutputStream os;
  private boolean stop;

  public OutputStream getOutputStream() {

    return this.os;
  }

  protected abstract void processOutput(final String line) throws IOException,
      InterruptedException;

  protected abstract void close() throws IOException, InterruptedException;

  //
  // Run method
  //

  @Override
  public void run() {

    String line = null;

    try {
      do {

        line = this.reader.readLine();

        if (line != null)
          processOutput(line);

      } while (line != null && !this.stop);

      logger.info("End of the output of command.");

      this.reader.close();
      close();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (InterruptedException e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);

    } finally {
      try {
        close();
      } catch (IOException e) {
        System.err.println(e.getMessage());
        e.printStackTrace(System.err);
      } catch (InterruptedException e) {
        System.err.println(e.getMessage());
        e.printStackTrace(System.err);
      }
    }

  }

  public int waitFor() throws InterruptedException {

    return this.process.waitFor();
  }

  //    
  // Constructor
  //

  public AbstractExternalCommandMapRedPipeThread(final String cmd,
      final Charset charset) throws IOException {

    logger.info("execute: " + cmd);
    this.process = Runtime.getRuntime().exec(cmd);

    this.reader =
        new BufferedReader(new InputStreamReader(this.process.getInputStream(),
            charset));
    this.os = this.process.getOutputStream();
  }
}