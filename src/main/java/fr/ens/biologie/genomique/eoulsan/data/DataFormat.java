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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.data;

import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.splitermergers.Merger;
import fr.ens.biologie.genomique.eoulsan.splitermergers.Splitter;
import java.util.List;

/**
 * This interface define a DataFormat.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface DataFormat {

  /**
   * Get the name of the format.
   *
   * @return the name of the format
   */
  String getName();

  /**
   * Get the description of the format.
   *
   * @return the name of the format
   */
  String getDescription();

  /**
   * Get the alias of the name of the format. The alias is optional.
   *
   * @return the alias of the format if exist
   */
  String getAlias();

  /**
   * Get DataFormat prefix.
   *
   * @return the DataFormat prefix
   */
  String getPrefix();

  /**
   * Test if there is only one file for this DataType per analysis.
   *
   * @return true if there is only one file for this DataType per analysis
   */
  boolean isOneFilePerAnalysis();

  /**
   * Test if the DataFormat is provided by the design file.
   *
   * @return true if the DataType is provided by the design file
   */
  boolean isDataFormatFromDesignFile();

  /**
   * Get the name of the design metadata key of the design file that can provide the DataFile.
   *
   * @return the sample metadata key name
   */
  String getDesignMetadataKeyName();

  /**
   * Get the name of the sample metadata key of the design file that can provide the DataFile.
   *
   * @return the sample metadata key name
   */
  String getSampleMetadataKeyName();

  /**
   * Get the content type.
   *
   * @return the content type of this format
   */
  String getContentType();

  /**
   * Get the default extension of the DataType.
   *
   * @return the default extension
   */
  String getDefaultExtension();

  /**
   * Get the extensions for the DataType
   *
   * @return an list of strings with the extension of the DataType
   */
  List<String> getExtensions();

  /**
   * Get the extension from Galaxy tool file of the DataType.
   *
   * @return extension from Galaxy tool
   */
  List<String> getGalaxyFormatNames();

  /**
   * Test if a generator is available for this DataFormat.
   *
   * @return true if a generator is available for this DataFormat
   */
  boolean isGenerator();

  /**
   * Test if a checker is available for this DataFormat.
   *
   * @return true if a checker is available for this DataFormat
   */
  boolean isChecker();

  /**
   * Test if a splitter class is available for this DataFormat.
   *
   * @return true if a splitter class is available for this DataFormat
   */
  boolean isSplitter();

  /**
   * Test if a merger class is available for this DataFormat.
   *
   * @return true if a merger class is available for this DataFormat
   */
  boolean isMerger();

  /**
   * Get the step needed to generate the DataType from DataTypes provided by the Design file.
   *
   * @return the Step needed to generated the DataType or null if no Step is available for this task
   */
  Module getGenerator();

  /**
   * Get the checker needed to check data of this type.
   *
   * @return the Checker or null if no Checker is available for this task
   */
  Checker getChecker();

  /**
   * Get the splitter class related to this type.
   *
   * @return The Splitter instanced class of null if no Splitter is available for this task
   */
  Splitter getSplitter();

  /**
   * Get the merger class related to this type.
   *
   * @return The Merger instanced class of null if no Merger is available for this task
   */
  Merger getMerger();

  /**
   * Get the maximal number of files used to store data of this format. This value cannot be lower
   * than 1. Common values are 1 or 2.
   *
   * @return the number of maximal of files used to store data.
   */
  int getMaxFilesCount();
}
