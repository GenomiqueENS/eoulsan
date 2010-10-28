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

package fr.ens.transcriptome.eoulsan;

import org.apache.hadoop.util.ProgramDriver;

import fr.ens.transcriptome.eoulsan.core.action.HadoopExecAction;

/**
 * Main class in Hadoop mode.
 * @author Laurent Jourdren
 */
public class MainHadoop {

  public static void main(final String[] args) {

    final ProgramDriver pgd = new ProgramDriver();

    try {

      pgd.addClass("exec", HadoopExecAction.class, "Execute "
          + Globals.APP_NAME);

      pgd.driver(args);

    } catch (Throwable e) {
      System.err.println(e);
      e.printStackTrace();
    }

  }

}
