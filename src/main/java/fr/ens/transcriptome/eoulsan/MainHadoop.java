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

import fr.ens.transcriptome.eoulsan.programs.expression.hadoop.ExpressionHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.hadoop.FilterAndSoapMapReadsHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.hadoop.FilterReadsHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.hadoop.SoapMapReadsHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mgmt.hadoop.UploadDesignDataHadoopMain;

/**
 * Main class in Hadoop mode.
 * @author Laurent Jourdren
 */
public class MainHadoop {

  public static void main(final String[] args) {

    int exitCode = -1;

    System.out.println(Globals.APP_NAME
        + " version " + Globals.APP_VERSION + " (" + Globals.APP_BUILD_NUMBER
        + ")");

    final ProgramDriver pgd = new ProgramDriver();

    try {

      pgd.addClass("uploaddesigndata", UploadDesignDataHadoopMain.class,
          "Upload design data.");
      pgd.addClass("filterreads", FilterReadsHadoopMain.class, "Filter reads.");
      pgd.addClass("soapmapreads", SoapMapReadsHadoopMain.class,
          "Map reads on genome with SOAP.");
      pgd.addClass("filterandsoapmapreads", FilterAndSoapMapReadsHadoopMain.class,
          "Filter and map reads on genome with SOAP.");
      pgd.addClass("expression", ExpressionHadoopMain.class, "Expression.");

      pgd.driver(args);

      // success
      exitCode = 0;
    } catch (Throwable e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    System.exit(exitCode);
  }

}
