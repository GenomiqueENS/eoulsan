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

package fr.ens.biologie.genomique.eoulsan.util.locker;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

/**
 * This class define a lock to prevent execution of a process simultaneously on
 * multiples JVM.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class ExecLock implements Locker {

  private static final String LOCK_EXTENSION = ".lock";
  private static final String PID_EXTENSION = ".pid";
  private static final String PID_LOCK_EXTENSION = ".pidlock";

  private static final int pid = getPid();

  private final String execName;
  private final Path tmpDir;
  private final Path lockFile;
  private final Path pidLockFile;
  private final Path pidFile;
  private boolean lock;

  private static int getPid() {

    final String beanName = ManagementFactory.getRuntimeMXBean().getName();

    final int index = beanName.indexOf('@');

    return Integer.parseInt(beanName.substring(0, index));
  }

  @Override
  public void lock() {

    while (this.lock) {
      sleep(5000);
    }

    try {

      if (!this.pidFile.toFile().createNewFile()) {
        throw new IOException(
            "Can not create pid file: " + this.pidFile.toAbsolutePath());
      }
      int count = 0;

      do {

        if (count == 0) {
          checkLockJVMAlive();
        }

        if (!Files.exists(this.lockFile)) {
          if (checkPid()) {

            if (!this.lockFile.toFile().createNewFile()) {
              throw new IOException("Can not create lock file: "
                  + this.lockFile.toAbsolutePath());
            }
            if (!this.pidLockFile.toFile().createNewFile()) {
              throw new IOException("Can not create pid lock file: "
                  + this.lockFile.toAbsolutePath());
            }
            this.lock = true;
            return;
          }
        }

        if (count == 12) {
          count = 0;
        }

        count++;
        sleep(5000);

      } while (true);

    } catch (IOException e) {

      e.printStackTrace();
    }
  }

  @Override
  public void unlock() {

    if (!this.lockFile.toFile().delete()) {
      getLogger().warning(
          "Can not delete lock file: " + this.lockFile.toAbsolutePath());
    }
    if (!this.pidLockFile.toFile().delete()) {
      getLogger().warning("Can not delete pid lock file: "
          + this.pidLockFile.toAbsolutePath());
    }
    if (!this.pidFile.toFile().delete()) {
      getLogger().warning(
          "Can not delete pid file: " + this.pidFile.toAbsolutePath());
    }
    this.lock = false;
    sleep(10000);
  }

  /**
   * Check the pid that wait the resources are alive and if this JVM has the
   * oldest registered pid.
   * @return true if the JVM has the oldest registered pid.
   */
  private boolean checkPid() {

    File[] files = this.tmpDir.toFile()
        .listFiles((arg0, arg1) -> arg1.startsWith(ExecLock.this.execName + "-")
            && arg1.endsWith(PID_EXTENSION));

    if (files == null) {
      return true;
    }

    Set<Integer> jvmPids = getJVMsPIDs();

    int oldestPid = -1;
    long oldestPidFileDate = Long.MAX_VALUE;

    for (File f : files) {

      String basename = StringUtils.basename(f.getName());
      int fPid;

      try {
        fPid = Integer.parseInt(basename.substring(this.execName.length() + 1));
      } catch (NumberFormatException e) {
        continue;
      }

      if (!jvmPids.contains(fPid)) {
        if (!f.delete()) {
          getLogger()
              .warning("Can not delete pid file: " + f.getAbsolutePath());
        }
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
  private Set<Integer> getJVMsPIDs() {

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

    File[] files = this.tmpDir.toFile()
        .listFiles((arg0, arg1) -> arg1.startsWith(ExecLock.this.execName + "-")
            && arg1.endsWith(PID_LOCK_EXTENSION));

    if (files == null || files.length == 0) {
      if (!this.lockFile.toFile().delete()) {
        getLogger().warning(
            "Can not delete lock file: " + this.lockFile.toAbsolutePath());
      }
      return;
    }

    final Set<Integer> jvmsPIDs = getJVMsPIDs();
    int count = 0;

    for (File f : files) {

      String basename = StringUtils.basename(f.getName());
      int fPid;
      try {
        fPid = Integer.parseInt(basename.substring(this.execName.length() + 1));
      } catch (NumberFormatException e) {
        if (!f.delete()) {
          getLogger()
              .warning("Can not delete pid file: " + f.getAbsolutePath());
        }
        continue;
      }

      if (jvmsPIDs.contains(fPid)) {
        count++;
      } else if (!f.delete()) {
        getLogger().warning("Can not delete pid file: " + f.getAbsolutePath());
      }

    }

    if (count == 0) {
      if (!this.lockFile.toFile().delete()) {
        getLogger().warning(
            "Can not delete lock file: " + this.lockFile.toAbsolutePath());
      }
    }
  }

  /**
   * Get the number of processes waiting.
   * @return the number of process waiting
   */
  public int getProcessesWaiting() {

    File[] files = this.tmpDir.toFile()
        .listFiles((arg0, arg1) -> arg1.startsWith(ExecLock.this.execName + "-")
            && arg1.endsWith(PID_EXTENSION));

    if (files == null) {
      return 0;
    }

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
  public ExecLock(final String execName, final Path tmpDir) {

    this.execName = execName;
    this.tmpDir = tmpDir;
    this.lockFile = this.tmpDir.resolve(this.execName + LOCK_EXTENSION);
    this.pidLockFile =
        this.tmpDir.resolve(this.execName + "-" + pid + PID_LOCK_EXTENSION);
    this.pidFile =
        this.tmpDir.resolve(this.execName + "-" + pid + PID_EXTENSION);
  }

  /**
   * Public constructor.
   * @param execName resource name
   */
  public ExecLock(final String execName) {

    this(execName, Path.of("/tmp"));
  }

}
