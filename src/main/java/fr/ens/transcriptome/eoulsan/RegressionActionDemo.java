package fr.ens.transcriptome.eoulsan;

import java.io.IOException;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.actions.IntegrationTestAction;

public class RegressionActionDemo {

  /**
   * Logger
   * @throws IOException
   */

  public static void main(String[] args) throws EoulsanException, IOException {

    RegressionActionDemo.mainbis();

  }

  public static void mainbis() {

    final String confpath = "/import/mimir03/lib/it/it_global.conf";

    final String fileTests = "import/mimir03/lib/it/list_tests.txt";
    final String reportDir =
        "/import/geri02/it_eoulsan_results/output/testng-results";

    // Set the default local for all the application
    Globals.setDefaultLocale();

    IntegrationTestAction action = new IntegrationTestAction();

    // Run action
    int choice = 0;

    switch (choice) {
    case 0:
      action.action(Lists.newArrayList("-c", confpath, "-expected", "new",
          "-exec", "/home/sperrin/home-net/eoulsan-1.2.2", "-o", reportDir));
      break;

    case 1:
      action.action(Lists.newArrayList("-c", confpath, "-expected", "all",
          "-exec", "/home/sperrin/home-net/eoulsan-1.2.2", "-o", reportDir));
      break;
    case 2:

      action.action(Lists.newArrayList("-c", confpath, "-exec",
          "/home/sperrin/home-net/eoulsan_newReadsMappers", "-f", fileTests,
          "-o", reportDir));
      break;
    case 3:
      action.action(Lists.newArrayList("-c", confpath, "-exec",
          "/home/sperrin/home-net/eoulsan_newReadsMappers", "-o", reportDir));
      break;
    default:
      System.out.println("Choice INVALID");
    }
  }
}
