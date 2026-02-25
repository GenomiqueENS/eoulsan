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

package fr.ens.biologie.genomique.eoulsan.requirements;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class define an abstract requirement.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractRequirement implements Requirement {

  public static final String NAME_PARAMETER = "name";
  protected static final String OPTIONAL_PARAMETER = "optional";
  protected static final String INSTALLABLE_PARAMETER = "installable";

  private boolean optional;
  private boolean installable;

  //
  // Getters
  //

  @Override
  public boolean isOptional() {

    return this.optional;
  }

  @Override
  public boolean isInstallable() {

    return this.installable;
  }

  //
  // Setters
  //

  protected void setOptionnal(final boolean optional) {

    this.optional = optional;
  }

  protected void setInstallable(final boolean installable) {

    this.installable = installable;
  }

  //
  // Other methods
  //

  @Override
  public Set<Parameter> getParameters() {

    final Set<Parameter> result = new LinkedHashSet<>();

    result.add(new Parameter(NAME_PARAMETER, getName()));
    result.add(new Parameter(OPTIONAL_PARAMETER, "" + isOptional()));
    result.add(new Parameter(INSTALLABLE_PARAMETER, "" + isInstallable()));

    return result;
  }

  @Override
  public void configure(final Set<Parameter> parameters) throws EoulsanException {

    requireNonNull(parameters, "parameter argument cannot be null");

    for (Parameter p : parameters) {

      switch (p.getName()) {
        case NAME_PARAMETER:
          // Nothing to do
          break;

        case OPTIONAL_PARAMETER:
          setOptionnal(p.getBooleanValue());
          break;

        case INSTALLABLE_PARAMETER:
          setInstallable(p.getBooleanValue());
          break;

        default:
          throw new EoulsanException("Unknown requirement parameter: " + p.getName());
      }
    }
  }
}
