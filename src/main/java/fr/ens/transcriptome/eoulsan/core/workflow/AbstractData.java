package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define an abstract data
 * @since 1.3
 * @author Laurent Jourdren
 */
public abstract class AbstractData implements Data {

  private static int instanceCount;

  private String name;
  private boolean defaultName = true;
  private final DataFormat format;

  @Override
  public String getName() {

    return name;
  }

  @Override
  public DataFormat getFormat() {
    return this.format;
  }

  /**
   * Set the name of the data.
   * @param name the new name of the data
   */
  void setName(final String name) {

    Preconditions.checkNotNull("The name of the data cannot be null");

    Preconditions.checkArgument(
        CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(name),
        "The name of data can only contains letters or digit");

    this.name = name;
    this.defaultName = false;
  }

  /**
   * Test if the name of the data is the default name.
   * @return true if the name of the data is the default name
   */
  boolean isDefaultName() {
    return this.defaultName;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param format format of the data
   */
  protected AbstractData(final DataFormat format) {

    Preconditions.checkNotNull(name, "name argument cannot be null");
    Preconditions.checkNotNull(name, "format argument cannot be null");

    this.name = "data" + (++instanceCount);
    this.format = format;
  }

}
