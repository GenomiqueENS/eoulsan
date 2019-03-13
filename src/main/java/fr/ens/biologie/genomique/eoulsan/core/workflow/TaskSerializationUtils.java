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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.Globals.TASK_DATA_EXTENSION;
import static fr.ens.biologie.genomique.eoulsan.Globals.TASK_RESULT_EXTENSION;
import static java.util.Objects.requireNonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.schedulers.TaskSchedulerFactory;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class contains utility methods for serialization, deserialization and
 * execution of Task objects.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskSerializationUtils {

  /**
   * Execute a task context serialization file.
   * @param taskContextFile input task context file
   * @return the task result
   * @throws IOException if an error occurs while reading or writing serialized
   *           files
   * @throws EoulsanException if an error occurs while executing the task
   */
  public static final TaskResultImpl execute(final DataFile taskContextFile)
      throws IOException, EoulsanException {

    requireNonNull(taskContextFile, "contextFile argument cannot be null");

    return execute(taskContextFile, taskContextFile.getParent());
  }

  /**
   * Execute a task context serialization file.
   * @param taskContextFile input task context file
   * @param outputDir output directory for results file
   * @return the task result
   * @throws IOException if an error occurs while reading or writing serialized
   *           files
   * @throws EoulsanException if an error occurs while executing the task
   */
  public static final TaskResultImpl execute(final DataFile taskContextFile,
      final DataFile outputDir) throws IOException, EoulsanException {

    requireNonNull(taskContextFile, "contextFile argument cannot be null");
    requireNonNull(outputDir, "taskResultFile argument cannot be null");

    if (!taskContextFile.exists()) {
      throw new FileNotFoundException(
          "The context file does not exists: " + taskContextFile);
    }

    // Load context file
    final TaskContextImpl context =
        TaskContextImpl.deserialize(taskContextFile);

    // Get TaskResult
    final TaskResultImpl result = executeContext(context);

    // Save TaskResult
    saveTaskResult(taskContextFile, context, result);

    return result;
  }

  /**
   * Execute a context.
   * @param context context to execute
   * @return a TaskResult object
   * @throws EoulsanException if an error occurs while executing the task
   */
  private static TaskResultImpl executeContext(final TaskContextImpl context)
      throws EoulsanException {

    // Load module instance
    final Module module =
        StepInstances.getInstance().getModule(context.getCurrentStep());

    final long startTime = System.currentTimeMillis();

    // Configure step
    try {
      module.configure(context, context.getCurrentStep().getParameters());
    } catch (Throwable t) {

      final long endTime = System.currentTimeMillis();

      // An exception has occured while configuring the step
      getLogger().severe("Exception while configuring task: " + t.getMessage());

      return new TaskResultImpl(context, new Date(startTime), new Date(endTime),
          endTime - startTime, t, t.getMessage());
    }

    // Create the context runner
    final TaskRunner runner = new TaskRunner(context);

    // Force TaskRunner to res-use the step instance that just has been
    // created
    runner.setForceStepInstanceReuse(true);

    // Initialize scheduler
    TaskSchedulerFactory.initialize();

    // Get the result
    return runner.run();
  }

  /**
   * Save a TaskResult object.
   * @param taskContextFile the task context file
   * @param context the Eoulsan context
   * @param result the TaskResult to save
   * @throws IOException if an error occurs while reading or writing serialized
   *           files
   */
  private static void saveTaskResult(final DataFile taskContextFile,
      final TaskContextImpl context, final TaskResultImpl result)
      throws IOException {

    // Get the prefix for the task files and the base dir
    final String taskPrefix = context.getTaskFilePrefix();
    final DataFile baseDir = taskContextFile.getParent();

    // Save task result
    result.serialize(new DataFile(baseDir, taskPrefix + TASK_RESULT_EXTENSION));

    // Save task output data
    context.serializeOutputData(
        new DataFile(baseDir, taskPrefix + TASK_DATA_EXTENSION));

    // Create done file
    new DataFile(baseDir, taskPrefix + Globals.TASK_DONE_EXTENSION).create()
        .close();
  }

}
