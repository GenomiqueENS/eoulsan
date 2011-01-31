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

import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsIndexGeneratorStep;

/**
 * This class define a SOAP zip index DataFormat.
 * @author Laurent Jourdren
 */
public final class SOAPIndexZipDataFormat extends AbstractDataFormat {

  public static final String FORMAT_NAME = "soap_index_zip";

  public DataType getType() {

    return DataTypes.SOAP_INDEX;
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

    return new ReadsIndexGeneratorStep("soap");
  }

}