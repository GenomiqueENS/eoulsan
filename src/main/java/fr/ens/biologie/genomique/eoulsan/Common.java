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

package fr.ens.biologie.genomique.eoulsan;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import fr.ens.biologie.genomique.eoulsan.core.workflow.StepObserverRegistry;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define common constants.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class Common {

  /**
   * Write log data.
   * @param os OutputStream of the log file
   * @param data data to write
   * @throws IOException if an error occurs while writing log file
   */
  public static void writeLog(final OutputStream os, final long startTime,
      final String data) throws IOException {

    final long endTime = System.currentTimeMillis();
    final long duration = endTime - startTime;

    final Writer writer = new OutputStreamWriter(os, Globals.DEFAULT_CHARSET);
    writer.write("Start time: "
        + new Date(startTime) + "\nEnd time: " + new Date(endTime)
        + "\nDuration: " + StringUtils.toTimeHumanReadable(duration) + "\n");
    writer.write(data);
    writer.close();
  }

  /**
   * Write log data.
   * @param file the log file
   * @param data data to write
   * @throws IOException if an error occurs while writing log file
   */
  public static void writeLog(final File file, final long startTime,
      final String data) throws IOException {

    if (file == null) {
      throw new NullPointerException("File for log file is null.");
    }

    writeLog(FileUtils.createOutputStream(file), startTime, data);
  }

  /**
   * Show a message and then exit.
   * @param message the message to show
   */
  public static void showMessageAndExit(final String message) {

    System.out.println(message);
    exit(0);
  }

  /**
   * Show and log an error message.
   * @param message message to show and log
   */
  public static void showAndLogErrorMessage(final String message) {

    getLogger().severe(message);
    System.err.println(message);
  }

  /**
   * Show a message and then exit.
   * @param message the message to show
   */
  public static void showErrorMessageAndExit(final String message) {

    System.err.println(message);
    exit(1);
  }

  /**
   * Print error message to the user and exits the application.
   * @param e Exception
   * @param message message to show to the use
   */
  public static void errorExit(final Throwable e, final String message) {

    errorExit(e, message, true);
  }

  /**
   * Print error message to the user and exits the application.
   * @param e Exception
   * @param message message to show to the use
   * @param logMessage true if message must be logged
   */
  public static void errorExit(final Throwable e, final String message,
      final boolean logMessage) {

    if (logMessage) {
      getLogger().severe(message);
    }

    System.err.println("\n=== " + Globals.APP_NAME + " Error ===");
    System.err.println(message);

    if (!EoulsanRuntime.isRuntime()
        || EoulsanRuntime.getSettings().isPrintStackTrace()) {
      printStackTrace(e);
    }

    exit(1);
  }

  /**
   * Print error message to the user and halts the application.
   * @param e Exception
   * @param message message to show to the use
   */
  public static void errorHalt(final Throwable e, final String message) {

    errorHalt(e, message, true);
  }

  /**
   * Print error message to the user and halts the application.
   * @param e Exception
   * @param message message to show to the use
   * @param logMessage true if message must be logged
   */
  public static void errorHalt(final Throwable e, final String message,
      final boolean logMessage) {

    if (logMessage) {
      getLogger().severe(message);
    }

    System.err.println("\n=== " + Globals.APP_NAME + " Error ===");
    System.err.println(message);

    if (!EoulsanRuntime.isRuntime()
        || EoulsanRuntime.getSettings().isPrintStackTrace()) {
      printStackTrace(e);
    }

    halt(1);
  }

  /**
   * Print the stack trace for an exception.
   * @param e Exception
   */
  private static void printStackTrace(final Throwable e) {

    System.err.println("\n=== " + Globals.APP_NAME + " Debug Stack Trace ===");
    e.printStackTrace();
    System.err.println();
  }

  /**
   * Exit the application.
   * @param exitCode exit code
   */
  public static void exit(final int exitCode) {

    System.exit(exitCode);
  }

  /**
   * Exit the application.
   * @param exitCode exit code
   */
  public static void halt(final int exitCode) {

    Runtime.getRuntime().halt(exitCode);
  }

  public static void sendMail(final String subject, final String message) {

    if (!EoulsanRuntime.isRuntime()) {
      return;
    }

    final Settings settings = EoulsanRuntime.getSettings();

    final boolean sendMail = settings.isSendResultMail();
    final Properties properties = settings.getJavaMailSMTPProperties();
    final String userMail = settings.getResultMail();

    if (!sendMail) {
      return;
    }

    if (!properties.containsKey("mail.smtp.host")) {
      getLogger().warning("No SMTP server set");
      return;
    }

    if (userMail == null) {
      getLogger().warning("No user mail set");
      return;
    }

    final Session session = Session.getInstance(properties);

    try {
      // Instantiate a message
      Message msg = new MimeMessage(session);

      // Set message attributes
      msg.setFrom(new InternetAddress(userMail));
      InternetAddress[] address = {new InternetAddress(userMail)};
      msg.setRecipients(Message.RecipientType.TO, address);
      msg.setSubject(subject);
      msg.setSentDate(new Date());

      // Set message content
      msg.setText(message);

      // Send the message
      Transport.send(msg);

    } catch (MessagingException mex) {

      getLogger().warning("Error while sending mail: " + mex.getMessage());

      // Prints all nested (chained) exceptions as well
      if (!EoulsanRuntime.isRuntime()
          || EoulsanRuntime.getSettings().isPrintStackTrace()) {
        mex.printStackTrace();
      }
    }

  }

  /**
   * Get the number of threads to use from localThreads, maxLocalThreads and
   * global threads number.
   * @param localThreads number of threads
   * @param maxLocalThreads maximum number of threads
   * @return the number of threads to use
   */
  public static int getThreadsNumber(final int localThreads,
      final int maxLocalThreads) {

    int threads = EoulsanRuntime.getSettings().getLocalThreadsNumber();

    if (localThreads > 0) {
      threads = localThreads;
    }

    if (maxLocalThreads > 0) {
      threads = Math.min(threads, maxLocalThreads);
    }

    return threads;
  }

  /**
   * Print warning.
   * @param message message to print
   */
  public static void printWarning(final String message) {

    if (message == null) {
      return;
    }

    // Currently only print warning messages when no UI has been set
    if (StepObserverRegistry.getInstance().isNoObserverRegistered()) {
      System.err.println(message);
    }
  }

  //
  // Constructor
  //

  private Common() {

    throw new IllegalStateException();
  }

}
