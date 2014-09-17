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

package fr.ens.transcriptome.eoulsan;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * This class define a buffered handler.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BufferedHandler extends Handler {

  private List<Handler> handlers = new ArrayList<Handler>();
  private Queue<LogRecord> records = new LinkedList<LogRecord>();
  private boolean flushed = false;

  /**
   * Add an Handler.
   * @param handler Handler to had
   */
  public void addHandler(final Handler handler) {

    if (handler != null) {

      if (getLevel() != null)
        handler.setLevel(getLevel());

      if (getFormatter() != null)
        handler.setFormatter(getFormatter());

      this.handlers.add(handler);
    }
  }

  //
  // Handler methods
  //

  @Override
  public void close() throws SecurityException {

    for (Handler h : this.handlers)
      h.close();
  }

  @Override
  public void flush() {

    if (!flushed) {
      LogRecord record = null;

      while ((record = this.records.poll()) != null)
        for (Handler h : this.handlers)
          h.publish(record);

      this.flushed = true;
    }

    for (Handler h : this.handlers)
      h.flush();
  }

  @Override
  public void publish(final LogRecord record) {

    if (!this.flushed)
      this.records.add(record);
    else
      for (Handler h : this.handlers)
        h.publish(record);
  }

  @Override
  public synchronized void setLevel(final Level newLevel)
      throws SecurityException {

    super.setLevel(newLevel);

    for (Handler h : this.handlers)
      h.setLevel(newLevel);
  }

  @Override
  public synchronized void setFormatter(Formatter newFormatter)
      throws SecurityException {

    super.setFormatter(newFormatter);

    for (Handler h : this.handlers)
      h.setFormatter(newFormatter);
  }

}
