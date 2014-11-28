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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define a result for a task context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskResult implements StepResult, Serializable {

  private static final long serialVersionUID = -1698693204391020077L;

  private final TaskContext context;

  private final Date startTime;
  private final Date endTime;
  private final long duration;
  private final boolean success;
  private final Throwable exception;
  private final String errorMessage;
  private final Map<String, Long> counters = new HashMap<>();
  private final String taskMessage;
  private final String taskDescription;

  TaskContext getContext() {
    return this.context;
  }

  Date getStartTime() {
    return this.startTime;
  }

  Date getEndTime() {
    return this.endTime;
  }

  Map<String, Long> getCounters() {
    return Collections.unmodifiableMap(this.counters);
  }

  String getDescription() {
    return this.taskDescription;
  }

  String getMessage() {
    return this.taskMessage;
  }

  @Override
  public long getDuration() {
    return this.duration;
  }

  @Override
  public boolean isSuccess() {
    return this.success;
  }

  @Override
  public Throwable getException() {
    return this.exception;
  }

  @Override
  public String getErrorMessage() {
    return this.errorMessage;
  }

  //
  // Serialization methods
  //

  /**
   * Serialize the TaskResult object.
   * @param file output DataFile
   * @throws IOException if an error occurs while creating the file
   */
  public void serialize(final File file) throws IOException {

    checkNotNull(file, "file argument cannot be null");

    serialize(new FileOutputStream(file));
  }

  /**
   * Serialize the TaskResult object.
   * @param file output DataFile
   * @throws IOException if an error occurs while creating the file
   */
  public void serialize(final DataFile file) throws IOException {

    checkNotNull(file, "file argument cannot be null");

    serialize(file.create());
  }

  /**
   * Serialize the TaskResult object.
   * @param out output stream
   * @throws IOException if an error occurs while creating the file
   */
  public final void serialize(final OutputStream out) throws IOException {

    checkNotNull(out, "out argument cannot be null");

    final ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(this);
    oos.close();
  }

  /**
   * Deserialize the TaskResult object.
   * @param file input DataFile
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskResult deserialize(final File file) throws IOException {

    checkNotNull(file, "file argument cannot be null");

    return deserialize(new FileInputStream(file));
  }

  /**
   * Deserialize the TaskResult object.
   * @param file input DataFile
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskResult deserialize(final DataFile file) throws IOException {

    checkNotNull(file, "file argument cannot be null");

    return deserialize(file.open());
  }

  /**
   * Deserialize the TaskResult object.
   * @param in input stream
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskResult deserialize(final InputStream in) throws IOException {

    checkNotNull(in, "in argument cannot be null");

    try {
      final ObjectInputStream ois = new ObjectInputStream(in);

      // Read TaskContext object
      final TaskResult result = (TaskResult) ois.readObject();

      ois.close();

      return result;

    } catch (ClassNotFoundException e) {
      throw new EoulsanRuntimeException(e.getMessage());
    }
  }

  //
  // Constructor
  //

  TaskResult(final TaskContext context, final Date startTime,
      final Date endTime, final long duration, final String contextMessage,
      final String contextDescription, final Map<String, Long> counters,
      final boolean success) {

    Preconditions.checkNotNull(context, "context argument cannot be null");
    Preconditions.checkNotNull(startTime, "startTime argument cannot be null");
    Preconditions.checkNotNull(endTime, "endTime argument cannot be null");
    Preconditions.checkNotNull(contextDescription,
        "contextDescription argument cannot be null");
    Preconditions.checkNotNull(counters, "counter argument cannot be null");

    this.context = context;
    this.startTime = startTime;
    this.endTime = endTime;
    this.duration = duration;
    this.success = success;
    this.taskMessage = contextMessage;
    this.taskDescription = contextDescription;
    this.counters.putAll(counters);
    this.exception = null;
    this.errorMessage = null;
  }

  TaskResult(final TaskContext context, final Date startTime,
      final Date endTime, final long duration, final Throwable exception,
      final String errorMessage) {

    Preconditions.checkNotNull(context, "context argument cannot be null");

    this.context = context;
    this.startTime = startTime;
    this.endTime = endTime;
    this.duration = duration;
    this.success = false;
    this.taskMessage = null;
    this.taskDescription = null;
    this.exception = exception;
    this.errorMessage = errorMessage;
  }

}
