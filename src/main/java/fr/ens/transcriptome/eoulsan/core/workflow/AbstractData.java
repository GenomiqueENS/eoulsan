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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import com.google.common.base.CharMatcher;

import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This class define an abstract data.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractData implements Data, Serializable {

  private static final long serialVersionUID = 2363270050921101143L;

  private static int instanceCount;

  private String name;
  private boolean defaultName = true;
  private final DataFormat format;

  @Override
  public String getName() {

    return name;
  }

  @Override
  public DataFormat getFormat() {
    return this.format;
  }

  /**
   * Set the name of the data.
   * @param name the new name of the data
   */
  void setName(final String name) {

    checkNotNull(name, "The name of the data cannot be null");
    checkArgument(CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(name),
        "The name of data can only contains letters or digit");

    this.name = name;
    this.defaultName = false;
  }

  /**
   * Test if the name of the data is the default name.
   * @return true if the name of the data is the default name
   */
  boolean isDefaultName() {
    return this.defaultName;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param format format of the data
   */
  protected AbstractData(final DataFormat format) {

    checkNotNull(format, "format argument cannot be null");

    this.name = "data" + (++instanceCount);
    this.format = format;
  }

}
