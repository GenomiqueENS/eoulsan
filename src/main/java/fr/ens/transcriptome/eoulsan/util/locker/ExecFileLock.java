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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * This class define a lock file.
 * @author Laurent Jourdren
 */
public class ExecFileLock {

  private final FileLock lock;
  private final FileChannel lockChannel;
  private boolean released;

  /**
   * Releases the lock.
   * @throws IOException if an error occurs while release lock
   */
  public void release() throws IOException {

    if (released)
      return;

    if (lock.isValid()) {
      lock.release();
      released = true;

      if (lockChannel.isOpen())
        lockChannel.close();
    }

    // lockFile.delete();
  }

  //
  // Constructor
  //

  /**
   * Create a lock file.
   * @param lockFile The lock file to create
   */
  public ExecFileLock(final File lockFile) throws IOException {

    lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
    lock = lockChannel.lock();

    // lockFile.deleteOnExit();
  }

}
