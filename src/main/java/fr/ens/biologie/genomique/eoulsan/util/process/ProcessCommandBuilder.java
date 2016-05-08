package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;

/**
 * This class define a command builder.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class ProcessCommandBuilder {

  protected static final String TMP_DIR_ENV_VARIABLE = "TMPDIR";

  private int uid = -1;

  private int gid = -1;

  private final Map<String, String> environment = new HashMap<>();

  private File directory;

  private File temporaryDirectory;

  private final Set<File> mountDirectories = new HashSet<>();

  private final List<String> arguments = new ArrayList<>();

  private File stdoutFile;

  private File stderrFile;

  private boolean redirectStderr;

  //
  // Getters
  //

  /**
   * Get the uid of the process.
   * @return the uid of the process
   */
  public int uid() {
    return this.uid;
  }

  /**
   * Get the gid of the process.
   * @return the gid of the process
   */
  public int gid() {
    return this.gid;
  }

  /**
   * Get the environment variables of the process.
   * @return the environment variables of the process
   */
  public Map<String, String> environment() {
    return this.environment;
  }

  /**
   * Get the arguments of the command.
   * @return the arguments of the command
   */
  public List<String> arguments() {
    return this.arguments;
  }

  /**
   * Get the execution directory of the command.
   * @return the execution directory of the command
   */
  public File directory() {
    return this.directory;
  }

  /**
   * Get the temporary directory of the command.
   * @return the temporary directory of the command
   */
  public File temporaryDirectory() {
    return this.temporaryDirectory;
  }

  /**
   * Get the directories to mount.
   * @return the directories to mount
   */
  public Set<File> mountDirectories() {
    return this.mountDirectories;
  }

  /**
   * Get the stdout file.
   * @return the stdout file
   */
  public File stdOutFile() {
    return this.stdoutFile;
  }

  /**
   * Get the stderr file.
   * @return the stderr file
   */
  public File stdErrFile() {
    return this.stderrFile;
  }

  /**
   * Test if stderr must be redirected in stdout.
   * @return true if stderr must be redirected in stdout
   */
  public boolean redirectStderr() {
    return this.redirectStderr;
  }

  //
  // Setters
  //

  /**
   * Set the uid of the command.
   * @param uid the uid of the command
   * @return the builder instance
   */
  public ProcessCommandBuilder uid(final int uid) {

    this.uid = uid;
    return this;
  }

  /**
   * Set the uid of the command using the current user uid.
   * @return the builder instance
   */
  public ProcessCommandBuilder currentUserUid() {

    return uid(SystemUtils.uid());
  }

  /**
   * Set the gid of the command.
   * @param uid the gid of the command
   * @return the builder instance
   */
  public ProcessCommandBuilder gid(final int gid) {

    this.gid = gid;
    return this;
  }

  /**
   * Set the gid of the command using the current user gid.
   * @return the builder instance
   */
  public ProcessCommandBuilder currentUserGid() {

    return gid(SystemUtils.gid());
  }

  /**
   * Set the arguments of the command.
   * @param arguments the arguments of the command
   * @return the builder instance
   */
  public ProcessCommandBuilder arguments(final List<String> arguments) {

    if (arguments == null) {
      throw new NullPointerException("arguments argument cannot be null");
    }

    this.arguments.clear();
    this.arguments.addAll(arguments);
    return this;
  }

  /**
   * Set the arguments of the command.
   * @param arguments the arguments of the command
   * @return the builder instance
   */
  public ProcessCommandBuilder arguments(final String... arguments) {

    if (arguments == null) {
      throw new NullPointerException("arguments argument cannot be null");
    }

    return arguments(Arrays.asList(arguments));
  }

  /**
   * Set the directory of the command.
   * @param directory the directory of the command
   * @return the builder instance
   */
  public ProcessCommandBuilder directory(final File directory) {

    this.directory = directory;
    return this;
  }

  /**
   * Set the temporary directory of the command.
   * @param temporaryDirectory the temporary directory of the command
   * @return the builder instance
   */
  public ProcessCommandBuilder temporaryDirectory(
      final File temporaryDirectory) {

    this.temporaryDirectory = temporaryDirectory;
    return this;
  }

  /**
   * Redirect stdout to a file.
   * @param stdoutFile the stdout file
   * @return the builder instance
   */
  public ProcessCommandBuilder redirectOutput(final File stdoutFile) {

    this.stdoutFile = stdoutFile;
    return this;
  }

  /**
   * Redirect stderr to a file.
   * @param stdoutFile the stderr file
   * @return the builder instance
   */
  public ProcessCommandBuilder redirectError(final File stderrFile) {

    this.stderrFile = stdoutFile;
    return this;
  }

  /**
   * Enable redirection of the stderr in stdout
   * @param redirectErrorStream true if the redirection must be enable
   * @return the builder instance
   */
  public ProcessCommandBuilder redirectErrorStream(
      final boolean redirectErrorStream) {

    this.redirectStderr = redirectErrorStream;
    return this;
  }

  /**
   * Add a directory to mount.
   * @param directory the directory to mount
   * @return the builder instance
   */
  public ProcessCommandBuilder addMountDirectory(File directory) {

    if (directory != null) {

      this.mountDirectories.add(directory);
    }

    return this;
  }

  /**
   * Clear the environment variables.
   * @return the builder instance
   */
  public ProcessCommandBuilder clearEnvironment() {

    this.environment.clear();

    return this;
  }

  /**
   * Add an environment variable.
   * @param name the name of the environment variable
   * @param value the value of the environment variable
   * @return the builder instance
   */
  public ProcessCommandBuilder addEnvironmentVariable(final String name,
      final String value) {

    if (name != null && value != null) {
      this.environment.put(name, value);
    }

    return this;
  }

  //
  // Other methods
  //

  /**
   * Clear the builder.
   * @return the builder instance
   */
  public ProcessCommandBuilder clear() {

    this.uid = -1;
    this.gid = -1;
    this.environment.clear();
    this.directory = null;
    this.temporaryDirectory = null;
    this.mountDirectories.clear();
    this.arguments.clear();
    this.stdoutFile = null;
    this.stderrFile = null;
    this.redirectStderr = false;

    return this;
  }

  /**
   * Create the process command.
   * @return a Process command object
   */
  public ProcessCommand create() {

    return internalCreate();
  }

  //
  // Abstract methods
  //

  /**
   * Internal creation of the command.
   * @return a Process command object
   */
  protected abstract ProcessCommand internalCreate();

}
