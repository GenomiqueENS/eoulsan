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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * This class define a lock file.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class ExecFileLock implements Locker {

  private final File lockFile;
  private FileLock lock;
  private FileChannel lockChannel;
  private boolean released;

  @SuppressWarnings("resource")
  @Override
  public void lock() throws IOException {

    this.lockChannel = new RandomAccessFile(this.lockFile, "rw").getChannel();
    this.lock = this.lockChannel.lock();

    // lockFile.deleteOnExit();
  }

  @Override
  public void unlock() throws IOException {

    if (this.released) {
      return;
    }

    if (this.lock.isValid()) {
      this.lock.release();
      this.released = true;

      if (this.lockChannel.isOpen()) {
        this.lockChannel.close();
      }
    }

    // lockFile.delete();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param lockFile lock file to use
   */
  public ExecFileLock(final File lockFile) {

    this.lockFile = lockFile;
  }
}
