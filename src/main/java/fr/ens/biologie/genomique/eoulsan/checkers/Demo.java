package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.io.DesignFormatFinderInputStream;
import fr.ens.biologie.genomique.eoulsan.design.io.DesignReader;
import fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader;

public class Demo {

    public static void main(String[] args) throws IOException, EoulsanException {

        // find ~/shares-net/sequencages/analyses/*/ -iname "design*.txt" >
        // design-file-list.txt

        List<String> lines =
                Files.readAllLines(Paths.get("/home/bertheli/tmp/design-file-list.txt"),
                        Charset.defaultCharset());

        for (String line : lines) {

            System.out.println("Read file : " + line);
            //DesignReader dr = new Eoulsan2DesignReader(line);
            DesignReader dr = new DesignFormatFinderInputStream(new FileInputStream(line)).getDesignReader();

            Design d = dr.read();

            for (Experiment e : d.getExperiments()) {

                boolean result = DESeq2Checker.checkExperimentDesign(e, true);
                if (result) {
                    System.out.println(e.getName() + "\t" + result);
                } else {
                    System.err.println(e.getName() + "\t" + result);
                }
            }

        }

    }

}