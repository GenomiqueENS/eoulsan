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

package fr.ens.biologie.genomique.eoulsan.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;

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

    Common.printWarning(message);
  }

  /**
   * Show a message for deprecated parameters that has been renamed. This method
   * do not throw an exception.
   * @param context the step configuration context
   * @param parameter the deprecated parameter
   * @param newParameterName the new parameter name
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

    Common.printWarning(message);
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
   * Get the last parameter which name match for a parameter set.
   * @param parameters the parameter set
   * @param parameterName the name of the parameter to look for
   * @return the last parameter that match or null if the parameter is not found
   */
  public static Parameter getParameter(final Set<Parameter> parameters,
      final String parameterName) {

    checkNotNull(parameters, "parameters argument cannot be null");
    checkNotNull(parameterName, "parameterName argument cannot be null");

    Parameter result = null;

    for (Parameter p : parameters) {
      if (parameterName.equals(p.getName())) {
        result = p;
      }
    }

    return result;
  }

  /**
   * test if a set of parameters contains a parameter.
   * @param parameters the parameter set
   * @param parameterName the name of the parameter to look for
   * @return true if the set of parameter contains the parameter
   */
  public static boolean containsParameter(final Set<Parameter> parameters,
      final String parameterName) {

    checkNotNull(parameters, "parameters argument cannot be null");
    checkNotNull(parameterName, "parameterName argument cannot be null");

    for (Parameter p : parameters) {
      if (parameterName.equals(p.getName())) {
        return true;
      }
    }

    return false;
  }

  //
  // Constructor
  //

  private Modules() {
  }

}
