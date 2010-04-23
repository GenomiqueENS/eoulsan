/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.hadoop.expression;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.KeyValueTextInputFormat;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

@SuppressWarnings("deprecation")
public class ExpressionMain {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static void main(final String[] args) throws Exception {

    System.out.println("Expression arguments:\t" + Arrays.toString(args));

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length <= 1)
      throw new IllegalArgumentException(
          "Expression need three or more arguments");

    final String gffURI = args[0];
    final String expressionType = args[1];
    final String parentType = args[2];
    final String alignmentFileURI = args[3];
    final String outputURI = args[4];

    // Create Configuration Object
    final Configuration conf = new Configuration();

    final Path alignmentPath = new Path(alignmentFileURI);
    final Path gffPath = new Path(gffURI);
    final Path outputPath = new Path(outputURI);

    // Base directory path
    final Path basePath = gffPath.getParent();

    // Create exons index
    final Path exonsIndexPath =
        createExonsIndex(gffPath, basePath, conf, expressionType);

    RunningJob rj =
        JobClient.runJob(createExpressionJob(exonsIndexPath, alignmentPath,
            outputPath, parentType));

    System.exit(rj.isSuccessful() ? 0 : 1);
  }

  private static final Path createExonsIndex(final Path gffPath,
      final Path basePath, final Configuration conf, String expressionType)
      throws IOException {

    final ExonFinder ef = new ExonFinder(gffPath, conf, expressionType);
    final File exonIndexFile = FileUtils.createTempFile("exonsindex-", ".data");
    ef.save(exonIndexFile);
    final Path exonsIndexPath = new Path(basePath, exonIndexFile.getName());
    PathUtils.copyLocalFileToPath(exonIndexFile, exonsIndexPath, conf);

    return exonsIndexPath;
  }

  private static final JobConf createExpressionJob(final Path exonsIndexPath,
      final Path alignmentPath, final Path outputPath,
      final String parentTypeType) {

    final JobConf conf = new JobConf(ExpressionMain.class);

    
    System.out.println("====" + conf.get("mapred.child.java.opts") + "====");
    conf.set("mapred.child.java.opts","-Xmx800m");
    System.out.println("====" + conf.get("mapred.child.java.opts") + "====");
    
    // Set the path to the exons index
    conf.set(Globals.PARAMETER_PREFIX + ".expression.exonsindex.path",
        exonsIndexPath.toString());

    // Set the parent type
    conf.set(Globals.PARAMETER_PREFIX + ".expression.parent.type",
        parentTypeType);

    // Debug
    //conf.set("mapred.job.tracker", "local");

    // Set input path
    FileInputFormat.setInputPaths(conf, alignmentPath);

    // Set the input format
    conf.setInputFormat(TextInputFormat.class);

    // Set the Mapper class
    conf.setMapperClass(ExpressionMapper.class);

    // Set the reducer class
    conf.setReducerClass(ExpressionReducer.class);

    // Set the output key class
    conf.setOutputKeyClass(Text.class);

    // Set the output value class
    conf.setOutputValueClass(Text.class);

    // Set the number of reducers
    conf.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(conf, outputPath);

    return conf;
  }

}
