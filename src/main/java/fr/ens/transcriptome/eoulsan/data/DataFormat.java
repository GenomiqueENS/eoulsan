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

import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;

public interface DataFormat {

  /**
   * Get the name of the format.
   * @return the name of the format
   */
  String getFormatName();

  /**
   * Get the data type.
   * @return the DataType of this format
   */
  DataType getType();

  /**
   * Get the content type.
   * @return the content type of this format
   */
  String getContentType();

  /**
   * Get the default extension of the DataType.
   * @return the default extension
   */
  String getDefaultExtention();

  /**
   * Get the extensions for the DataType
   * @return an array of strings with the extension of the DataType
   */
  String[] getExtensions();

  /**
   * Get the source path for a DataType from a sample object.
   * @param sample sample to use to create the source
   * @param ExecInfo information object
   * @return the source for the DataType as a String
   */
  String getSourcePathForSample(final Sample sample, final String ExecInfo);

  /**
   * Test if a generator is available for this DataFormat.
   * @return true if a generator is available for this DataFormat
   */
  boolean isGenerator();

  /**
   * Test if a checker is available for this DataFormat.
   * @return true if a checker is available for this DataFormat
   */
  boolean isChecker();

  /**
   * Get the step needed to generate the DataType from DataTypes provided by the
   * Design file.
   * @return the Step needed to generated the DataType or null if no Step is
   *         available for this task
   */
  Step getGenerator();

  /**
   * Get the checker needed to check data of this type
   * @return the Checker or null if no Checker is available for this task
   */
  Checker getChecker();

  /**
   * Test if the data for this is contained in several files 
   * @return true if the DataFormat is multifiles
   */
  boolean isMultiFiles();
}
