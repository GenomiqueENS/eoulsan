package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import org.python.google.common.base.Preconditions;

public class ToolExecutorResult {

  private final String commandLineTool;

  private int exitValue = -100000;
  private Throwable exception = null;

  public void setException(final Throwable exception) {
    this.exception = exception;
  }

  public void setExitValue(final int exitValue) {
    this.exitValue = exitValue;
  }

  public Throwable getException() {
    return this.exception;
  }

  public boolean asThrowedException() {
    return this.exception != null;
  }

  public int getExitValue() {
    return this.exitValue;
  }

  public String getCommandLine() {
    return this.commandLineTool;
  }

  @Override
  public String toString() {
    return "GalaxyToolResult [commandLineTool="
        + commandLineTool + ", exitValue=" + exitValue + ", exception="
        + exception + "]";
  }

  //
  // Constructor
  //

  public ToolExecutorResult(final String commandLineTool) {

    Preconditions
        .checkNotNull(commandLineTool, "Command line anc not be null.");

    this.commandLineTool = commandLineTool;
  }
}
