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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules;

import static com.google.common.base.Preconditions.checkNotNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.workflow.StepObserverRegistry;

/**
 * This class contains useful methods for writing Step classes.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class Modules {

  /**
   * Show a message for deprecated parameters.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   */
  public static void deprecatedParameter(final StepConfigurationContext context,
      final Parameter parameter) {

    try {
      deprecatedParameter(context, parameter, false);
    } catch (EoulsanException e) {
      // The EoulsanException cannot be thrown
    }
  }

  /**
   * Show a message for deprecated parameters.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   * @throws EoulsanException throw an exception if required
   */
  public static void deprecatedParameter(final StepConfigurationContext context,
      final Parameter parameter, final boolean throwException)
          throws EoulsanException {

    checkNotNull(context, "context argument cannot be null");
    checkNotNull(parameter, "parameter argument cannot be null");

    final String message = "The parameter \""
        + parameter.getName() + "\" in the \""
        + context.getCurrentStep().getId() + "\" step is now deprecated";

    if (throwException) {
      throw new EoulsanException(message);
    }

    printWarning(message);
  }

  /**
   * Show a message for deprecated parameters that has been renamed. This method
   * do not throw an exception.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   * @param newParameterName the new parameter name
   * @throws EoulsanException throw an exception if required
   */
  public static void renamedParameter(final StepConfigurationContext context,
      final Parameter parameter, final String newParameterName) {

    try {
      renamedParameter(context, parameter, newParameterName, false);
    } catch (EoulsanException e) {
      // The EoulsanException cannot be thrown
    }
  }

  /**
   * Show a message for deprecated parameters that has been renamed.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   * @param newParameterName the new parameter name
   * @param throwException throw an exception
   * @throws EoulsanException throw an exception if required
   */
  public static void renamedParameter(final StepConfigurationContext context,
      final Parameter parameter, final String newParameterName,
      final boolean throwException) throws EoulsanException {

    checkNotNull(context, "context argument cannot be null");
    checkNotNull(parameter, "parameter argument cannot be null");
    checkNotNull(newParameterName, "newName argument cannot be null");

    final String message = "The parameter \""
        + parameter.getName() + "\" in the \""
        + context.getCurrentStep().getId()
        + "\" step is now deprecated. Please use the \"" + newParameterName
        + "\" parameter instead";

    if (throwException) {
      throw new EoulsanException(message);
    }

    printWarning(message);
  }

  /**
   * Throw a exception for removed parameters.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   */
  public static void removedParameter(final StepConfigurationContext context,
      final Parameter parameter) throws EoulsanException {

    checkNotNull(context, "context argument cannot be null");
    checkNotNull(parameter, "parameter argument cannot be null");

    throw new EoulsanException("The parameter \""
        + parameter.getName() + "\" in the \""
        + context.getCurrentStep().getId() + "\" step no more exists");
  }

  /**
   * Throw a exception for unknown parameters.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   */
  public static void unknownParameter(final StepConfigurationContext context,
      final Parameter parameter) throws EoulsanException {

    checkNotNull(context, "context argument cannot be null");
    checkNotNull(parameter, "parameter argument cannot be null");

    throw new EoulsanException("Unknown \""
        + parameter.getName() + "\" parameter for the \""
        + context.getCurrentStep().getId() + "\" step");
  }

  /**
   * Throw a exception for bad parameter value.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   * @param message error message
   */
  public static void badParameterValue(final StepConfigurationContext context,
      final Parameter parameter, final String message) throws EoulsanException {

    checkNotNull(context, "context argument cannot be null");
    checkNotNull(parameter, "parameter argument cannot be null");
    checkNotNull(message, "message argument cannot be null");

    throw new EoulsanException("The invalid value ("
        + parameter.getValue() + ") for \"" + parameter.getName()
        + "\" parameter in the \"" + context.getCurrentStep().getId()
        + "\" step: " + message);
  }

  /**
   * Throw a exception for an invalid configuration.
   * @param context the step configuration context
   * @param message error message
   */
  public static void invalidConfiguration(
      final StepConfigurationContext context, final String message)
          throws EoulsanException {

    checkNotNull(context, "context argument cannot be null");
    checkNotNull(message, "message argument cannot be null");

    throw new EoulsanException("The invalid configuration for the \""
        + context.getCurrentStep().getId() + "\" step: " + message);
  }

  /**
   * Print warning.
   * @param message message to print
   */
  private static void printWarning(final String message) {

    if (message == null) {
      return;
    }

    // Currently only print warning messages when no UI has been set
    if (StepObserverRegistry.getInstance().getObservers().isEmpty()) {
      System.err.println(message);
    }
  }

  //
  // Constructor
  //

  private Modules() {
  }

}
