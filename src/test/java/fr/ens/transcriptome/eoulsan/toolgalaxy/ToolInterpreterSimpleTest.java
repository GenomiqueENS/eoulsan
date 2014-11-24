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

package fr.ens.transcriptome.eoulsan.toolgalaxy;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class ToolInterpreterSimpleTest {

  private File dir = new File(new File(".").getAbsolutePath(),
      "src/main/java/files/toolshed/");

  public final static Splitter TOKEN = Splitter.on(" ").trimResults()
      .omitEmptyStrings();

  private static boolean VERBOSE = false;

  @Test
  public void parseSam2Bam() throws FileNotFoundException, EoulsanException {
    final File toolFile = new File(dir, "sam2bam/1.1.4/sam_to_bam.xml");
    String name = "sam_to_bam";
    String version = "1.1.4";
    String desc = "converts SAM format to BAM format";
    String interpreter = "python";

    // type \t name \t value
    String paramTabular_SR = "";
    paramTabular_SR += "input\tsource.input1\tinput_value\n";
    paramTabular_SR += "input\tsource.index_source\tno_selected\n";
    paramTabular_SR += "input\tsource.ref_file\tref_file_value\n";
    paramTabular_SR += "input\tsource.index.fields.path\tindex_fields_value\n";
    paramTabular_SR += "output\toutput1\toutput_value\n";

    List<String> cmd_SR =
        TOKEN
            .splitToList("sam_to_bam.py --input1=input_value --index=index_fields_value --output1=output_value");

    final MockEoulsan mock =
        new MockEoulsan(name, version, desc, interpreter, cmd_SR,
            paramTabular_SR);

    checkInterperter(toolFile, mock);

    // type \t name \t value
    // type \t name \t value
    String paramTabular_withHistory = "";
    paramTabular_withHistory += "input\tsource.input1\tinput_value\n";
    paramTabular_withHistory += "input\tsource.index_source\thistory\n";
    paramTabular_withHistory += "input\tsource.ref_file\tref_file_value\n";
    paramTabular_withHistory +=
        "input\tsource.index.fields.path\tindex_fields_value\n";
    paramTabular_withHistory += "output\toutput1\toutput_value\n";

    List<String> cmd_withHistory =
        TOKEN
            .splitToList("sam_to_bam.py --input1=input_value --ref_file=ref_file_value --output1=output_value");
    mock.setCommand(cmd_withHistory, paramTabular_withHistory);

    checkInterperter(toolFile, mock);
  }

  @Test
  public void parseSamtoolsRmdup() throws FileNotFoundException,
      EoulsanException {
    final File toolFile =
        new File(dir, "samtools_rmdup/1.0.1/samtools_rmdup.xml");
    String name = "rmdup";
    String version = "1.0.1";
    String desc = "remove PCR duplicates";
    String interpreter = "";

    // type \t name \t value
    String paramTabular_SR = "";
    paramTabular_SR += "input\tinput1\tinput_value\n";
    paramTabular_SR += "output\toutput1\toutput_value\n";
    paramTabular_SR +=
        "param\tbam_paired_end_type.bam_paired_end_type_selector\tSE\n";

    List<String> cmd_SR =
        TOKEN.splitToList("samtools rmdup -s input_value output_value "
            + "2>&1 || echo 'Error running samtools rmdup.' >&2");

    // "samtools rmdup -s input_value output_value "
    // + "2&gt;&amp;1 || echo 'Error running samtools rmdup.' &gt;&amp;2";

    final MockEoulsan mock =
        new MockEoulsan(name, version, desc, interpreter, cmd_SR,
            paramTabular_SR);

    checkInterperter(toolFile, mock);

    // type \t name \t value
    String paramTabular_PE = "";
    paramTabular_PE += "input\tinput1\tinput_value\n";
    paramTabular_PE += "output\toutput1\toutput_value\n";
    paramTabular_PE +=
        "param\tbam_paired_end_type.bam_paired_end_type_selector\tPE\n";

    List<String> cmd_PE =
        TOKEN.splitToList("samtools rmdup -S input_value output_value "
            + "2>&1 || echo 'Error running samtools rmdup.' >&2");
    mock.setCommand(cmd_PE, paramTabular_PE);

    checkInterperter(toolFile, mock);
  }

  @Test
  public void parseSimpleToolFileTest() throws FileNotFoundException,
      EoulsanException {
    final File toolFile = new File(dir, "ToolGalaxyBasic.xml");

    // type \t name \t value
    String paramTabular = "";
    paramTabular += "input\tinput\tinput_value\n";
    paramTabular += "output\toutput\toutput_value\n";

    final MockEoulsan mock =
        new MockEoulsan("Compute GC content", "",
            "for each sequence in a file", "perl",
            TOKEN.splitToList("toolExample.pl input_value output_value"),
            paramTabular);

    checkInterperter(toolFile, mock);
  }

  private void checkInterperter(final File toolFile, final MockEoulsan mock)
      throws FileNotFoundException, EoulsanException {

    // Create input stream
    final InputStream is = FileUtils.createInputStream(toolFile);

    // Init interpreter tool galaxy
    final ToolInterpreter itg = new ToolInterpreter(is);

    // Configure
    itg.configure(mock.getParameters());

    if (VERBOSE)
      System.out.println(itg.toString());

    assertEquals("Tool name equals ? ", itg.getToolName(), mock.getName());
    assertEquals("Tool version equals ? ", itg.getToolVersion(),
        mock.getVersion());
    assertEquals("Tool description equals ? ", itg.getDescription(),
        mock.getDescription());
    assertEquals("Tool interpreter equals ? ", itg.getInterpreter(),
        mock.getInterpreter());

    // Set ports
    itg.setPortInput(mock.getInputsPort());
    itg.setPortOutput(mock.getOutputsPort());

    // Create command line
    final List<String> cmdExpected = TOKEN.splitToList(itg.createCommandLine());

    if (VERBOSE)
      System.out.println("\n\ncommand create\n\t " + cmdExpected);

    int n = 0;
    for (String token : mock.getCommand())
      assertEquals("Token in command line equals ", token, cmdExpected.get(n++));
  }

  //
  //
  //

  //
  // Internal class
  //

  final class MockEoulsan {
    private String name;
    private String version;
    private String description;
    private String interpreter;
    private List<String> command;

    private Map<String, String> parameters;
    private Map<String, String> inputs;
    private Map<String, String> outputs;

    public String getDescription() {
      return description;
    }

    public String getInterpreter() {
      return interpreter;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }

    public Map<String, String> getParameters() {
      return this.parameters;
    }

    public List<String> getCommand() {
      return command;
    }

    public Map<String, String> getInputsPort() {
      return this.inputs;
    }

    public Map<String, String> getOutputsPort() {
      return this.outputs;
    }

    void initMapPortsAndParameters(final String paramTabular)
        throws EoulsanException {
      Splitter splitLine = Splitter.on('\n').trimResults().omitEmptyStrings();

      for (String line : splitLine.splitToList(paramTabular)) {
        String[] tokens = line.split("\t");

        switch (tokens[0]) {
        case "param":
          this.parameters.put(tokens[1], tokens[2]);
          break;

        case "input":
          this.inputs.put(tokens[1], tokens[2]);
          break;

        case "output":
          this.outputs.put(tokens[1], tokens[2]);
          break;

        default:
          throw new EoulsanException("Entry unknown: " + tokens[0]);
        }
      }
    }

    public void setCommand(final List<String> cmd, final String paramTabular)
        throws EoulsanException {
      this.command = cmd;

      // Re initialize map value
      this.parameters.clear();
      this.inputs.clear();
      this.outputs.clear();

      initMapPortsAndParameters(paramTabular);
    }

    public MockEoulsan(final String name, final String version,
        final String description, final String interpreter,
        final List<String> command, final String paramTabular)
        throws EoulsanException {
      this.name = name;
      this.version = version;
      this.description = description;
      this.interpreter = interpreter;
      this.command = command;

      this.parameters = Maps.newHashMap();
      this.inputs = Maps.newHashMap();
      this.outputs = Maps.newHashMap();

      initMapPortsAndParameters(paramTabular);
    }
  }
}
