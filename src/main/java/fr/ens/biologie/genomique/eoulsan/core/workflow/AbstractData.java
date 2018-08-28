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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import fr.ens.biologie.genomique.eoulsan.core.FileNaming;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This class define an abstract data.
 * @since 2.0
 * @author Laurent Jourdren
 */
abstract class AbstractData implements Data, Serializable {

  private static final long serialVersionUID = 2363270050921101143L;

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final int id;
  private String name;
  private boolean defaultName = true;
  private final DataFormat format;
  private int part = -1;

  @Override
  public String getName() {

    if (this.name == null) {
      return "data" + id;
    }

    return this.name;
  }

  @Override
  public DataFormat getFormat() {
    return this.format;
  }

  @Override
  public int getPart() {
    return this.part;
  }

  @Override
  public int size() {
    return getListElements().size();
  }

  @Override
  public boolean isEmpty() {
    return getListElements().isEmpty();
  }

  /**
   * Set the name of the data.
   * @param name the new name of the data
   */
  void setName(final String name) {

    setName(name, false);
  }

  /**
   * Set the name of the data.
   * @param name the new name of the data
   * @param defaultName true if the name of the data is a default name
   */
  void setName(final String name, final boolean defaultName) {

    checkNotNull(name, "The name of the data cannot be null");
    checkArgument(FileNaming.isDataNameValid(name),
        "The name of data can only contains letters and digit: " + name);

    this.name = name;
    this.defaultName = defaultName;
  }

  void setPart(final int part) {

    checkArgument(part >= -1, "Part argument must equals or greater tha -1");

    this.part = part;
  }

  /**
   * Test if the name of the data is the default name.
   * @return true if the name of the data is the default name
   */
  boolean isDefaultName() {
    return this.defaultName;
  }

  /**
   * Test if a name has been set for the data.
   * @return true if a name has been set for the data
   */
  boolean isNameSet() {
    return this.name != null;
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

    this.id = instanceCount.incrementAndGet();

    this.name = "data" + this.id;
    this.format = format;
  }

}
