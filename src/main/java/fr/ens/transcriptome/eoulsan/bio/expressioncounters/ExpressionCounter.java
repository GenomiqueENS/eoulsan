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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import java.io.IOException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define an interface for a wrapper on an ExpressionCounter.
 * @since 1.2
 * @author Claire Wallon
 */
public interface ExpressionCounter {

  //
  // Getters
  //

  /**
   * Get the counter name.
   * @return a string with the counter name
   */
  String getCounterName();

  /**
   * Get the strand usage for the ExpressionCounter.
   * @return the StrandUsage
   */
  StrandUsage getStranded();

  /**
   * Get the overlap mode for the ExpressionCounter.
   * @return the OverlapMode
   */
  OverlapMode getOverlapMode();

  /**
   * Test if ambiguous cases must be removed.
   * @return true if ambiguous cases must be removed
   */
  boolean isRemoveAmbiguousCases();

  /**
   * Get the genomic type on which to count expression.
   * @return a string with the genomic type name
   */
  String getGenomicType();

  /**
   * Get the GFF attribute ID to be used as feature ID.
   * @return a string with the attribute ID
   */
  String getAttributeId();

  /**
   * Get the temporary directory.
   * @return a string with the absolute path of the temporary directory
   */
  String getTempDirectory();

  //
  // Setters
  //

  /**
   * Set the strand usage for the ExpressionCounter.
   * @param stranded the StrandUsage object
   */
  void setStranded(StrandUsage stranded);

  /**
   * Set the overlap mode for the ExpressionCounter.
   * @param mode the OverlapMode object
   */
  void setOverlapMode(OverlapMode mode);

  /**
   * Set if ambiguous cases musr be removed.
   * @param removeAmbigousCases true if ambiguous cases must be removed
   */
  void setRemoveAmbiguousCases(boolean removeAmbigousCases);

  /**
   * Set the genomic type on which to count expression.
   * @param genomicType string with the genomic type name
   */
  void setGenomicType(String genomicType);

  /**
   * Set the attribute ID to be used as feature ID.
   * @param attributeId string with the attribute ID
   */
  void setAttributeId(String attributeId);

  /**
   * Set the temporary directory.
   * @param tempDirectory a string with the absolute path of the temporary
   *          directory
   */
  void setTempDirectory(String tempDirectory);

  //
  // Counting methods
  //

  /**
   * This method runs the ExpressionCounter.
   * @param alignmentFile : file containing SAM alignments
   * @param annotationFile : file containing the reference genome annotation
   * @param expressionFile : output file for the expression step
   * @param genomeDescFile : file containing the genome description
   * @throws IOException
   */
  void count(DataFile alignmentFile, DataFile annotationFile,
      DataFile expressionFile, DataFile genomeDescFile) throws IOException,
      EoulsanException, BadBioEntryException;

  //
  // Other methods
  //

  /**
   * This method initializes the ExpressionCounter.
   * @param genomicType : a string with the genomic type on which to count
   *          expression
   * @param attributeId GFF attribute to be used as feature ID
   * @param reporter : the Reporter object of the Eoulsan run
   * @param counterGroup : string with the counter name group for the expression
   *          step
   */
  void init(String genomicType, String attributeId, Reporter reporter,
      String counterGroup);

}
