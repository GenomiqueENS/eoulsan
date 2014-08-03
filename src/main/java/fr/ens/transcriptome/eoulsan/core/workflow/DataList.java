package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define a data list.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class DataList extends AbstractData {

  private final List<Data> list = Lists.newArrayList();

  // Field required for multi-files Data creation
  private final WorkflowOutputPort port;

  @Override
  public Data addDataToList(final String name) {

    Preconditions.checkNotNull(name, "name argument cannot be null");

    if (this.port == null)
      throw new UnsupportedOperationException();

    final AbstractData result = new DataElement(this.port);
    result.setName(name);
    this.list.add(result);

    return result;
  }

  @Override
  public boolean isList() {
    return true;
  }

  @Override
  public List<Data> getListElements() {
    return Collections.unmodifiableList(this.list);
  }

  /**
   * Get the modifiable list of data object.
   * @return a list object
   */
  List<Data> getModifiableList() {
    return this.list;
  }

  @Override
  public Map<String, String> getMetadata() {
    return Collections.emptyMap();
  }

  @Override
  public String getDataFilename() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDataFilename(int fileIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataFile getDataFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataFile getDataFile(int fileIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getDataFileCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getDataFileCount(boolean existingFiles) {
    throw new UnsupportedOperationException();
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   * @param port input port
   */
  DataList(final WorkflowInputPort port) {

    super(port.getFormat());

    this.port = null;
  }

  /**
   * Constructor.
   * @param port output port
   */
  DataList(final WorkflowOutputPort port) {

    super(port.getFormat());

    this.port = port;
  }

}
