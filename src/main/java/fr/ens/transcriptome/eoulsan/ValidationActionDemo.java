package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.actions.ValidationAction;
import fr.ens.transcriptome.eoulsan.io.CompareFiles;
import fr.ens.transcriptome.eoulsan.io.LogCompareFiles;

public class ValidationActionDemo {

  /**
   * Logger
   * @throws IOException
   */
  // private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static void main(String[] args) throws EoulsanException, IOException {
    ValidationActionDemo.mainbis();

  }

  public static void mainbis() {

    final String jobDescription = "validation_test";
    final String confpath =
        "/home/sperrin/Documents/test_eoulsan/tests_fonctionnels/test_fonctionnel.conf";

    final String fileTests =
        "/home/sperrin/Documents/test_eoulsan/tests_fonctionnels/list_tests.txt";

    // Set the default local for all the application
    Globals.setDefaultLocale();

    Main main = new MainCLI(new String[] {"validation"});

    Action action0 = ActionService.getInstance().getAction("validation");
    ValidationAction action = (ValidationAction) action0;

    // Get the Eoulsan settings
    final Settings settings = EoulsanRuntime.getSettings();

    // Run action
    // action.action(new String[] {"-c", confpath, "-generate", "new", "-d",
    // jobDescription});

    // action.action(new String[] {"-c", confpath, "-generate", "all", "-d",
    // jobDescription});

    // action.action(new String[] {"-c", confpath, "-f", fileTests, "-d",
    // jobDescription});

    action.action(new String[] {"-c", confpath, "-d", jobDescription});
  }

  public static void testLogCompare() {

    // Map<Object, Object> map = System.getProperties();
    // System.out.println("print properties \n"
    // + Joiner.on("\n").withKeyValueSeparator("\t").join(map));
    CompareFiles comp = new LogCompareFiles();
    String dir =
        new File(
            "/home/sperrin/Documents/test_eoulsan/dataset_source/test_expected/")
            .listFiles(new FileFilter() {

              @Override
              public boolean accept(final File pathname) {
                return pathname.getName().startsWith("eoulsan-");
              }
            })[0].getAbsolutePath();

    System.out.println("dir log " + dir);

    String fileA =
        "/home/sperrin/Documents/test_eoulsan/dataset_source/expected/eoulsan-20140124-112847/filtersam.log";
    String fileB = dir + "/filtersam.log";
    try {
      System.out.println("result " + comp.compareFiles(fileA, fileB));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
