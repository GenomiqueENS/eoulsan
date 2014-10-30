package fr.ens.transcriptome.eoulsan.toolgalaxy;

public interface ToolElement {

  public final static String SEP = ".";

  String getName();

  void setParameterEoulsan(final String value);

  boolean isSetting();

  String getParameterEoulsan();

}
