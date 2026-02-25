package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import static fr.ens.biologie.genomique.kenetre.bio.io.BioCharsets.SAM_CHARSET;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Splitter;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.JobContext;

/**
 * This class contains methods and classes related to save and load SAM file header in Hadoop
 * mappers and reducers.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SAMHeaderHadoopUtils {

  static final String SAM_HEADER_FILE_PREFIX = "_samheader_";

  /** This class allow to save the SAM header read by a mapper. */
  public static class SAMHeaderWriter {

    private List<String> headers;
    private final String attemptId;

    /**
     * Write the line to the SAM header file if the line is a SAM header.
     *
     * @param context the Hadoop context
     * @param line the line read
     * @return if the line is an header or an empty line
     * @throws IOException if an error occurs while writing the SAM file header
     */
    public boolean writeIfHeaderLine(final JobContext context, final String line)
        throws IOException {

      requireNonNull(line, "line argument cannot be null");

      // Test empty line
      if (line.length() == 0) {
        return true;
      }

      // Test if the line is a SAM header
      if (line.charAt(0) == '@') {

        if (this.headers == null) {
          this.headers = new ArrayList<>();
        }

        this.headers.add(line);

        return true;
      }

      close(context);
      return false;
    }

    /**
     * Close the SAM file header.
     *
     * @param context the Hadoop context
     * @throws IOException if an error occurs while writing the SAM file header
     */
    public void close(final JobContext context) throws IOException {

      // If headers previously found write it in a file
      if (this.headers != null) {

        // Save headers

        requireNonNull(context, "context argument cannot be null");

        final Path outputPath =
            new Path(context.getConfiguration().get("mapreduce.output.fileoutputformat.outputdir"));

        final Path headerPath = new Path(outputPath, SAM_HEADER_FILE_PREFIX + attemptId);
        final Writer writer =
            new OutputStreamWriter(
                PathUtils.createOutputStream(headerPath, context.getConfiguration()), SAM_CHARSET);

        for (String l : this.headers) {
          writer.write(l + "\n");
        }

        writer.close();

        this.headers = null;
      }
    }

    /**
     * Constructor.
     *
     * @param attemptId Hadoop task attempt Id
     */
    public SAMHeaderWriter(final String attemptId) {

      requireNonNull(attemptId, "attemptId argument cannot be null");
      this.attemptId = attemptId;
    }
  }

  /**
   * Load SAM headers.
   *
   * @param context the Hadoop context
   * @return a list of String with the SAM headers
   * @throws IOException if an error occurs while loading the headers
   */
  public static List<String> loadSAMHeaders(final JobContext context) throws IOException {

    requireNonNull(context, "context argument cannot be null");

    final List<String> result = new ArrayList<>();

    // Get the output path of the reducer
    final Path outputPath =
        new Path(context.getConfiguration().get("mapreduce.output.fileoutputformat.outputdir"));

    // Get the file system object
    final FileSystem fs = context.getWorkingDirectory().getFileSystem(context.getConfiguration());

    // Found the complete SAM header file
    Path bestFile = null;
    long maxLen = -1;

    for (FileStatus status : fs.listStatus(outputPath)) {
      if (status.getPath().getName().startsWith(SAM_HEADER_FILE_PREFIX)
          && status.getLen() > maxLen) {
        maxLen = status.getLen();
        bestFile = status.getPath();
      }
    }

    // Check if the SAM header file has been found
    if (bestFile == null) {
      throw new IOException("No SAM header file found in reducer output directory: " + outputPath);
    }

    try (final BufferedReader reader =
        new BufferedReader(new InputStreamReader(fs.open(bestFile), SAM_CHARSET))) {

      String line = null;

      while ((line = reader.readLine()) != null) {

        result.add(line);
      }
    }

    return result;
  }

  /**
   * Create a SAMSequenceDictionary from the SAM header in a list of String.
   *
   * @param headers the list of String
   * @return a new SAMSequenceDictionary object with the SAM headers
   */
  public static SAMSequenceDictionary createSAMSequenceDictionaryFromSAMHeader(
      final List<String> headers) {

    requireNonNull(headers, "headers argument cannot be null");

    final Splitter spliter = Splitter.on('\t');

    // Dictionary for sequences
    final SAMSequenceDictionary result = new SAMSequenceDictionary();

    for (String line : headers) {

      if (line.startsWith("@SQ\t")) {

        // Parse sequence name and length

        String sequenceName = null;
        int sequenceLength = -1;

        for (String f : spliter.split(line)) {
          if (f.startsWith("SN:")) {
            sequenceName = f.substring(3);
          } else if (f.startsWith("LN:")) {
            try {
              sequenceLength = Integer.parseInt(f.substring(3));
            } catch (NumberFormatException e) {
              // Do not handle the case where the value is not an integer
            }
          }
        }

        // Add sequence to SAM header
        if (sequenceName != null && sequenceLength != -1) {
          result.addSequence(new SAMSequenceRecord(sequenceName, sequenceLength));
        }
      }
    }

    return result;
  }
}
