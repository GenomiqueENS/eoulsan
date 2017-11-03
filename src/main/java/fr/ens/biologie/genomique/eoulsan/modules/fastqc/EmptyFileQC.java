package fr.ens.biologie.genomique.eoulsan.modules.fastqc;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import uk.ac.babraham.FastQC.Modules.AbstractQCModule;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.Sequence;

/**
 * Empty file FastQC module.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class EmptyFileQC extends AbstractQCModule {

  private final String filename;

  @SuppressWarnings("serial")
  private class ResultsTable extends AbstractTableModel {

    private final String[] rowNames =
        new String[] {"Filename", "Total Sequences",};

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public int getRowCount() {
      return rowNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      switch (columnIndex) {
      case 0:
        return rowNames[rowIndex];
      case 1:
        switch (rowIndex) {
        case 0:
          return filename;
        case 1:
          return "0";

        }
      }
      return null;
    }

    @Override
    public String getColumnName(int columnIndex) {
      switch (columnIndex) {
      case 0:
        return "Measure";
      case 1:
        return "Value";
      }
      return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
      case 0:
        return String.class;
      case 1:
        return String.class;
      }
      return null;
    }
  }

  @Override
  public String description() {
    return "Calculates some basic statistics about the file";
  }

  @Override
  public boolean ignoreFilteredSequences() {
    return false;
  }

  @Override
  public JPanel getResultsPanel() {
    JPanel returnPanel = new JPanel();
    returnPanel.setLayout(new BorderLayout());
    returnPanel.add(new JLabel("Basic sequence stats", JLabel.CENTER),
        BorderLayout.NORTH);

    TableModel model = new ResultsTable();
    returnPanel.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

    return returnPanel;

  }

  public void reset() {
    // Do nothing
  }

  public String name() {
    return "Basic Statistics";
  }

  public void processSequence(final Sequence sequence) {
    // Do nothing
  }

  public boolean raisesError() {
    return true;
  }

  public boolean raisesWarning() {
    return false;
  }

  public boolean ignoreInReport() {
    return false;
  }

  public void makeReport(final HTMLReportArchive report)
      throws XMLStreamException, IOException {
    super.writeTable(report, new ResultsTable());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param file The file
   */
  public EmptyFileQC(final DataFile file) {
    this.filename = file.getName();
  }

}
