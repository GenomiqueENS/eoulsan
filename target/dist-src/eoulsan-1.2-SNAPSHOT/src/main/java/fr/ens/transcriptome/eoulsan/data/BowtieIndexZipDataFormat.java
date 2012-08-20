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

package fr.ens.transcriptome.eoulsan.data;

import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeMapperIndexGeneratorStep;

/**
 * This class define a Bowtie zip index DataFormat.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class BowtieIndexZipDataFormat extends AbstractDataFormat {

  public static final String FORMAT_NAME = "bowtie_index_zip";

  public DataType getType() {

    return DataTypes.BOWTIE_INDEX;
  }

  @Override
  public String getDefaultExtention() {

    return ".zip";
  }

  @Override
  public String getFormatName() {

    return FORMAT_NAME;
  }

  @Override
  public String getContentType() {

    return "application/zip";
  }

  @Override
  public boolean isGenerator() {

    return true;
  }

  @Override
  public Step getGenerator() {

    return new GenomeMapperIndexGeneratorStep("bowtie");
  }

}