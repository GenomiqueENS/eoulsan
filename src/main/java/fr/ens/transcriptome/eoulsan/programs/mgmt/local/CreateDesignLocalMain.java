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

package fr.ens.transcriptome.eoulsan.programs.mgmt.local;

import java.io.FileNotFoundException;

import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignWriter;

import fr.ens.transcriptome.eoulsan.programs.mgmt.DesignBuilder;

/**
 * Main class for Create Design program.
 * @author Laurent Jourdren
 */
public class CreateDesignLocalMain {

  public static String PROGRAM_NAME = "createdesign";

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    DesignBuilder db = new DesignBuilder(args);

    Design design = db.getDesign();

    try {
      DesignWriter dw = new SimpleDesignWriter("design.txt");
      dw.write(design);

    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + e.getMessage());
      System.exit(1);
    } catch (EoulsanIOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }

  }

}
