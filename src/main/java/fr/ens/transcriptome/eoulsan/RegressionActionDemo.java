package fr.ens.transcriptome.eoulsan;

import java.io.IOException;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.actions.IntegrationTestAction;

public class RegressionActionDemo {

  public static void main(String[] args) throws EoulsanException, IOException {

    RegressionActionDemo.mainbis();

    
  }

  public static void mainbis() {

    final String confpath = "/import/mimir03/lib/it/it_global.conf";

    final String fileTests = "import/mimir03/lib/it/list_tests.txt";
    final String reportDir =
        "/import/geri02/it_eoulsan_results/output/testng-results";

    final String appliToTestPath =
        "/home/sperrin/workspace/eoulsan/target/dist/eoulsan-1.3-SNAPSHOT";
    final String nameTest =
        "000001_mapreads_bowtie-withIndex_SR_plasticite_abdomen_B2012_small_local";
    // Set the default local for all the application
    // Globals.setDefaultLocale();

    IntegrationTestAction action = new IntegrationTestAction();

    // Run action
    int choice = 3;

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
    case 3: // execute IT for one test
      action.action(Lists.newArrayList("-c", confpath, "-exec",
          appliToTestPath, "-t", nameTest, "-o", reportDir));
      break;

    case 4:
      action.action(Lists.newArrayList("-c", confpath, "-exec",
          "/home/sperrin/home-net/eoulsan_newReadsMappers", "-o", reportDir));
      break;
    default:
      System.out.println("Choice INVALID");
    }
  }
}
