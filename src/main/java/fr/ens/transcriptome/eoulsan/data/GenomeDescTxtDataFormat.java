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

package fr.ens.transcriptome.eoulsan.data;

import fr.ens.transcriptome.eoulsan.steps.GenomeDescriptionGeneratorStep;
import fr.ens.transcriptome.eoulsan.steps.Step;

/**
 * This class define a genome description DataFormat.
 * @author Laurent Jourdren
 */
public final class GenomeDescTxtDataFormat extends AbstractDataFormat {

  public static final String FORMAT_NAME = "genome_desc_txt";

  public DataType getType() {

    return DataTypes.GENOME_DESC;
  }

  @Override
  public String getDefaultExtention() {

    return ".txt";
  }

  @Override
  public String getFormatName() {

    return FORMAT_NAME;
  }

  @Override
  public Step getGenerator() {

    return new GenomeDescriptionGeneratorStep();
  }

  @Override
  public boolean isGenerator() {

    return true;
  }

}