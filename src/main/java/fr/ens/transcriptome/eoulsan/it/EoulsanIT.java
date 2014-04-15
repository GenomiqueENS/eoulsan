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

package fr.ens.transcriptome.eoulsan.it;

import org.testng.annotations.Test;

import fr.ens.transcriptome.eoulsan.EoulsanITRuntimeException;

/**
 * This class define an integration test.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class EoulsanIT {

  // Demo parameter
  private final int max;

  /**
   * Launch the integration test.
   */
  @Test
  public void dummyTest() {

    // Dummy code
    for (int i = 0; i < this.max; i++) {
      if (i > 60)
        throw new EoulsanITRuntimeException("Error: i="
            + i + " (max=" + this.max + ")");

    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param numberOfTimes demo parameter
   */
  public EoulsanIT(int numberOfTimes) {

    this.max = numberOfTimes;
  }

}
