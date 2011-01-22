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

package fr.ens.transcriptome.eoulsan.actions;

import java.io.FileNotFoundException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignBuilder;
import fr.ens.transcriptome.eoulsan.design.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;

/**
 * This class define an action to create design file.
 * @author Laurent Jourdren
 */
public class CreateDesignAction implements Action {

  @Override
  public String getName() {
    return "createdesign";
  }

  @Override
  public String getDescription() {
    return "create a design file from a list of files.";
  }

  /**
   * Create soap index action.
   * @param args command line parameters for exec action
   */
  @Override
  public void action(final String[] args) {

    DesignBuilder db = new DesignBuilder(args);

    Design design = db.getDesign();

    if (design.getSampleCount() == 0) {
      Common
          .showErrorMessageAndExit("Error: Nothing to create, no file found.\n"
              + "  Use the -h option to get more information.\n" + "usage: "
              + Globals.APP_NAME_LOWER_CASE + " createdesign files");

    }

    try {
      DesignWriter dw = new SimpleDesignWriter("design.txt");

      dw.write(design);

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (EoulsanIOException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    }

  }

}
