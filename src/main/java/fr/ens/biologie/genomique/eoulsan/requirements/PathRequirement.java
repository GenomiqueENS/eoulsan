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

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.kenetre.util.SystemUtils.searchExecutableInPATH;
import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Progress;
import java.util.Collections;
import java.util.Set;

/**
 * This class define a executable requirement.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class PathRequirement extends AbstractRequirement {

  public static final String REQUIREMENT_NAME = "path";

  private static final String EXECUTABLE_NAME_PARAMETER = "executable.name";

  private String executableName;

  @Override
  public String getName() {

    return REQUIREMENT_NAME;
  }

  @Override
  public void configure(Set<Parameter> parameters) throws EoulsanException {

    for (Parameter p : parameters) {

      switch (p.getName()) {
        case EXECUTABLE_NAME_PARAMETER:
          this.executableName = p.getValue();
          break;

        default:
          super.configure(Collections.singleton(p));
          break;
      }
    }
  }

  @Override
  public Set<Parameter> getParameters() {

    final Set<Parameter> result = super.getParameters();

    result.add(new Parameter(EXECUTABLE_NAME_PARAMETER, this.executableName));

    return Collections.unmodifiableSet(result);
  }

  @Override
  public boolean isAvailable() {

    return searchExecutableInPATH(this.executableName) != null;
  }

  @Override
  public void install(Progress progress) throws EoulsanException {

    // Do nothing as this requirement is not installable
  }

  //
  // Constructor
  //

  /**
   * Create a new mandatory executable requirement.
   *
   * @param executableName the executable name
   * @return a new PathRequirement object
   */
  public static Requirement newPathRequirement(final String executableName) {

    return newPathRequirement(executableName, false);
  }

  /**
   * Create a new executable requirement.
   *
   * @param executableName the executable name
   * @param optional true if the executable is a mandatory requirement
   * @return a new PathRequirement object
   */
  public static Requirement newPathRequirement(
      final String executableName, final boolean optional) {

    requireNonNull(executableName, "executableName argument cannot be null");
    checkArgument(!executableName.trim().isEmpty(), "executableName argument cannot be empty");

    final PathRequirement result = new PathRequirement();

    result.executableName = executableName.trim();
    result.setInstallable(false);
    result.setOptionnal(optional);

    return result;
  }

  //
  // Object method
  //

  @Override
  public String toString() {
    return "Executable in PATH: " + this.executableName;
  }
}
