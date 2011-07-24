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

package fr.ens.transcriptome.eoulsan.util.locker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a lock to prevent execution of a process simultaneously on
 * multiples JVM.
 * @author Laurent Jourdren
 */
public class ExecLock {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final String LOCK_EXTENSION = ".lock";
  private static final String PID_EXTENSION = ".pid";
  private static final String PID_LOCK_EXTENSION = ".pidlock";

  private static final int pid = getPid();

  private String execName;
  private File tmpDir;
  private File lockFile;
  private File pidLockFile;
  private File pidFile;
  private boolean lock;

  private static int getPid() {

    final String beanName = ManagementFactory.getRuntimeMXBean().getName();

    final int index = beanName.indexOf('@');

    return Integer.parseInt(beanName.substring(0, index));
  }

  /**
   * Wait the token and then lock the resource.
   */
  public void lock() {

    while (lock)
      sleep(5000);

    try {

      pidFile.createNewFile();
      int count = 0;

      do {

        if (count == 0)
          checkLockJVMAlive();

        if (!lockFile.exists())
          if (checkPid()) {

            lockFile.createNewFile();
            pidLockFile.createNewFile();
            lock = true;
            return;
          }

        if (count == 12)
          count = 0;

        count++;
        sleep(5000);

      } while (true);

    } catch (IOException e) {

      e.printStackTrace();
    }
  }

  /**
   * Unlock the ressource.
   */
  public void unlock() {

    if (!lockFile.delete())
      logger.warning("Can not delete lock file: " + lockFile.getAbsolutePath());
    if (!pidLockFile.delete())
      logger.warning("Can not delete pid lock file: "
          + pidLockFile.getAbsolutePath());
    if (!pidFile.delete())
      logger.warning("Can not delete pid file: " + pidFile.getAbsolutePath());
    lock = false;
    sleep(10000);
  }

  /**
   * Check the pid that wait the resources are alive and if this JVM has the
   * oldest registered pid.
   * @return true if the JVM has the oldest registered pid.
   */
  private boolean checkPid() {

    File[] files = this.tmpDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File arg0, String arg1) {

        return arg1.startsWith(execName + "-") && arg1.endsWith(PID_EXTENSION);
      }

    });

    if (files == null)
      return true;

    Set<Integer> jvmPids = getJVMsPids();

    int oldestPid = -1;
    long oldestPidFileDate = Long.MAX_VALUE;

    for (File f : files) {

      String basename = StringUtils.basename(f.getName());
      int fPid;

      try {
        fPid = Integer.parseInt(basename.substring(execName.length() + 1));
      } catch (NumberFormatException e) {
        continue;
      }

      if (!jvmPids.contains(fPid)) {
        f.delete();
        continue;
      }

      if (f.lastModified() < oldestPidFileDate) {
        oldestPidFileDate = f.lastModified();
        oldestPid = fPid;
      }

    }

    return oldestPid == pid;
  }

  /**
   * Return a set withs pid of existing JVMs.
   * @return a set of integers with pid of existing JVMs
   */
  private Set<Integer> getJVMsPids() {

    return ProcessUtils.getExecutablePids("java");
  }

  /**
   * Sleep for n milliseconds
   * @param duration milliseconds to wait
   */
  private void sleep(final int duration) {

    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
    }

  }

  /**
   * Check that the JVM that lock the resource is alive.
   */
  private void checkLockJVMAlive() {

    File[] files = this.tmpDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File arg0, String arg1) {

        return arg1.startsWith(execName + "-")
            && arg1.endsWith(PID_LOCK_EXTENSION);
      }

    });

    if (files == null || files.length == 0) {
      this.lockFile.delete();
      return;
    }

    final Set<Integer> jvmsPids = getJVMsPids();
    int count = 0;

    for (File f : files) {

      String basename = StringUtils.basename(f.getName());
      int fPid;
      try {
        fPid = Integer.parseInt(basename.substring(execName.length() + 1));
      } catch (NumberFormatException e) {
        f.delete();
        continue;
      }

      if (jvmsPids.contains(fPid))
        count++;
      else
        f.delete();

    }

    if (count == 0)
      this.lockFile.delete();
  }

  /**
   * Get the number of processes waiting.
   * @return the number of process waiting
   */
  public int getProcessesWaiting() {

    File[] files = this.tmpDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File arg0, String arg1) {

        return arg1.startsWith(execName + "-") && arg1.endsWith(PID_EXTENSION);
      }

    });

    if (files == null)
      return 0;

    return files.length;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param execName resource name
   * @param tmpDir temporary directory where to create lock files
   */
  public ExecLock(final String execName, final File tmpDir) {

    this.execName = execName;
    this.tmpDir = tmpDir;
    this.lockFile = new File(this.tmpDir, this.execName + LOCK_EXTENSION);
    this.pidLockFile =
        new File(this.tmpDir, this.execName + "-" + pid + PID_LOCK_EXTENSION);
    this.pidFile =
        new File(this.tmpDir, this.execName + "-" + pid + PID_EXTENSION);
  }

  /**
   * Public constructor.
   * @param execName resource name
   */
  public ExecLock(final String execName) {

    this(execName, new File("/tmp"));
  }

}
