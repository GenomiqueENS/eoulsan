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

import static java.util.Objects.requireNonNull;

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

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.ClassLoaderObjectInputStream;

/**
 * This class define a result for a task context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskResultImpl implements TaskResult, Serializable {

  private static final long serialVersionUID = -1698693204391020077L;

  private final TaskContextImpl context;

  private final Date startTime;
  private final Date endTime;
  private final long duration;
  private final boolean success;
  private final Throwable exception;
  private final String errorMessage;
  private final Map<String, Long> counters = new HashMap<>();
  private final String taskMessage;
  private final String taskDescription;

  TaskContextImpl getContext() {
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

    requireNonNull(file, "file argument cannot be null");

    serialize(new FileOutputStream(file));
  }

  /**
   * Serialize the TaskResult object.
   * @param file output DataFile
   * @throws IOException if an error occurs while creating the file
   */
  public void serialize(final DataFile file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    serialize(file.create());
  }

  /**
   * Serialize the TaskResult object.
   * @param out output stream
   * @throws IOException if an error occurs while creating the file
   */
  public final void serialize(final OutputStream out) throws IOException {

    requireNonNull(out, "out argument cannot be null");

    final ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(this);
    oos.close();
  }

  /**
   * Deserialize the TaskResult object.
   * @param file input DataFile
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskResultImpl deserialize(final File file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    return deserialize(new FileInputStream(file));
  }

  /**
   * Deserialize the TaskResult object.
   * @param file input DataFile
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskResultImpl deserialize(final DataFile file)
      throws IOException {

    requireNonNull(file, "file argument cannot be null");

    return deserialize(file.open());
  }

  /**
   * Deserialize the TaskResult object.
   * @param in input stream
   * @throws IOException if an error occurs while reading the file
   */
  public static TaskResultImpl deserialize(final InputStream in)
      throws IOException {

    requireNonNull(in, "in argument cannot be null");

    try (final ObjectInputStream ois = new ClassLoaderObjectInputStream(in)) {

      // Read TaskContext object
      return (TaskResultImpl) ois.readObject();

    } catch (ClassNotFoundException e) {
      throw new EoulsanRuntimeException(e);
    }
  }

  //
  // Constructor
  //

  TaskResultImpl(final TaskContextImpl context, final Date startTime,
      final Date endTime, final long duration, final String contextMessage,
      final String contextDescription, final Map<String, Long> counters,
      final boolean success) {

    requireNonNull(context, "context argument cannot be null");
    requireNonNull(startTime, "startTime argument cannot be null");
    requireNonNull(endTime, "endTime argument cannot be null");
    requireNonNull(contextDescription,
        "contextDescription argument cannot be null");
    requireNonNull(counters, "counter argument cannot be null");

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

  public TaskResultImpl(final TaskContextImpl context, final Date startTime,
      final Date endTime, final long duration, final Throwable exception,
      final String errorMessage) {

    requireNonNull(context, "context argument cannot be null");

    this.context = context;
    this.startTime = startTime == null ? null : new Date(startTime.getTime());
    this.endTime = endTime == null ? null : new Date(endTime.getTime());
    this.duration = duration;
    this.success = false;
    this.taskMessage = null;
    this.taskDescription = null;
    this.exception = exception;
    this.errorMessage = errorMessage;
  }

}
