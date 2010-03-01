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

package fr.ens.transcriptome.eoulsan.hadoop;

import org.apache.hadoop.util.ProgramDriver;

public class PipelineDriver {

  public static void main(final String[] args) {

    int exitCode = -1;

    ProgramDriver pgd = new ProgramDriver();

    try {
      pgd.addClass("create_local_soap_index", CreateLocalSoapIndex.class,
          "Create local SOAP index.");
      pgd.addClass("import_data", ImportData.class,
          "Import local data to hdfs.");
      pgd.addClass("filter_reads", FilterReads.class, "Filter reads.");
      pgd.addClass("map_reads", MapReads.class, "Map reads.");
      

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
