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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DATA_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_RESULT_EXTENSION;

import java.io.FileNotFoundException;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.schedulers.TaskSchedulerFactory;
import fr.ens.transcriptome.eoulsan.data.DataFile;

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
   * @param outputDir output directory for results file
   * @return the task result
   * @throws IOException if an error occurs while reading or writing serialized
   *           files
   * @throws EoulsanException if an error occurs while executing the task
   */
  public static final TaskResult execute(final DataFile taskContextFile)
      throws IOException, EoulsanException {

    checkNotNull(taskContextFile, "contextFile argument cannot be null");

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
  public static final TaskResult execute(final DataFile taskContextFile,
      final DataFile outputDir) throws IOException, EoulsanException {

    checkNotNull(taskContextFile, "contextFile argument cannot be null");
    checkNotNull(outputDir, "taskResultFile argument cannot be null");

    if (!taskContextFile.exists()) {
      throw new FileNotFoundException(
          "The context file does not exists: " + taskContextFile);
    }

    // Load context file
    final TaskContext context = TaskContext.deserialize(taskContextFile);

    // Create the context runner
    final TaskRunner runner = new TaskRunner(context);

    // Load step instance
    final Step step =
        StepInstances.getInstance().getStep(context.getCurrentStep());

    // Configure step
    step.configure(context, context.getCurrentStep().getParameters());

    // Force TaskRunner to res-use the step instance that just has been
    // created
    runner.setForceStepInstanceReuse(true);

    // Initialize scheduler
    TaskSchedulerFactory.initialize();

    // Get the result
    final TaskResult result = runner.run();

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

    return result;
  }

}
