#!/bin/bash

GWT_PATH=/home/sperrin/Programmes/gwt-2.5.1
PROJECT_NAME=DesignValidator
GIT_REVISION=`git log -n 1 --pretty='%h (%ad)' --date=short `

BASEDIR=`dirname $0`
if [ ! -d $BASEDIR/target ]; then
  mkdir $BASEDIR/target
fi
cd $BASEDIR/target

EOULSAN_DIR=..
EOULSAN_PACKAGE=fr.ens.transcriptome.eoulsan
EOULSAN_PACKAGE_PATH=`echo $EOULSAN_PACKAGE | sed 's/\./\//g'`
PACKAGE=fr.ens.transcriptome.cdv
PACKAGE_PATH=`echo $PACKAGE | sed 's/\./\//g'`


rm -rf $PROJECT_NAME
$GWT_PATH/webAppCreator -out $PROJECT_NAME $PACKAGE.$PROJECT_NAME
rm  $PROJECT_NAME/src/fr/ens/transcriptome/cdv/client/*
rm  $PROJECT_NAME/src/fr/ens/transcriptome/cdv/server/*
rm  $PROJECT_NAME/src/fr/ens/transcriptome/cdv/shared/*

cp $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml.ori
head -n 20 $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml > $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml.tmp

cat >> $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml.tmp << EOF
  <inherits name='com.google.gwt.http.HTTP' />
EOF

tail -n +21 $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml >> $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml.tmp

rm $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml
mv $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml.tmp $PROJECT_NAME/src/fr/ens/transcriptome/cdv/$PROJECT_NAME.gwt.xml

for f in `echo EoulsanException.java`
do
	sed "s/package $EOULSAN_PACKAGE/package $PACKAGE.client/" $EOULSAN_DIR/src/main/java/$EOULSAN_PACKAGE_PATH/$f > $PROJECT_NAME/src/$PACKAGE_PATH/client/$f
done

for f in `echo CasavaDesign.java CasavaDesignUtil.java CasavaSample.java`
do
	sed "s/package $EOULSAN_PACKAGE.illumina/package $PACKAGE.client/" $EOULSAN_DIR/src/main/java/$EOULSAN_PACKAGE_PATH/illumina/$f | sed "s/import $EOULSAN_PACKAGE.illumina.io/import $PACKAGE.client/" | sed "s/import $EOULSAN_PACKAGE.illumina/import $PACKAGE.client/" |  sed "s/import $EOULSAN_PACKAGE/import $PACKAGE.client/"  > $PROJECT_NAME/src/$PACKAGE_PATH/client/$f
done

for f in `echo CasavaDesignReader.java AbstractCasavaDesignTextReader.java`
do
	sed "s/package $EOULSAN_PACKAGE.illumina.io/package $PACKAGE.client/" $EOULSAN_DIR/src/main/java/$EOULSAN_PACKAGE_PATH/illumina/io/$f | sed "s/import $EOULSAN_PACKAGE.illumina.io/import $PACKAGE.client/" | sed "s/import $EOULSAN_PACKAGE.illumina/import $PACKAGE.client/" |  sed "s/import $EOULSAN_PACKAGE/import $PACKAGE.client/" > $PROJECT_NAME/src/$PACKAGE_PATH/client/$f
done

#
# Add main class 
#

cat > $PROJECT_NAME/src/$PACKAGE_PATH/client/$PROJECT_NAME.java << EOF
package $PACKAGE.client;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.core.client.GWT;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class $PROJECT_NAME implements EntryPoint {

  private static final String DEFAULT_INDEXES = "#Index id, Sequence\n"
      + "I1=ATCACG\n" + "I2=CGATGT\n" + "I3=TTAGGC\n" + "I4=TGACCA\n"
      + "I5=ACAGTG\n" + "I6=GCCAAT\n" + "I7=CAGATC\n" + "I8=ACTTGA\n"
      + "I9=GATCAG\n" + "I10=TAGCTT\n" + "I11=GGCTAC\n" + "I12=CTTGTA\n"
      + "I13=AGTCAA\n" + "I14=AGTTCC\n" + "I15=ATGTCA\n" + "I16=CCGTCC\n"
      + "I17=GTAGAG\n" + "I18=GTCCGC\n" + "I19=GTGAAA\n" + "I20=GTGGCC\n"
      + "I21=GTTTCG\n" + "I22=CGTACG\n" + "I23=GAGTGG\n" + "I24=GGTAGC\n"
      + "I25=ACTGAT\n" + "I26=ATGAGC\n" + "I27=ATTCCT\n" + "I28=CAAAAG\n"
      + "I29=CAACTA\n" + "I30=CACCGG\n" + "I31=CACGAT\n" + "I32=CACTCA\n"
      + "I33=CAGGCG\n" + "I34=CATGGC\n" + "I35=CATTTT\n" + "I36=CCAACA\n"
      + "I37=CGGAAT\n" + "I38=CTAGCT\n" + "I39=CTATAC\n" + "I40=CTCAGA\n"
      + "I41=GACGAC\n" + "I42=TAATCG\n" + "I43=TACAGC\n" + "I44=TATAAT\n"
      + "I45=TCATTC\n" + "I46=TCCCGA\n" + "I47=TCGAAG\n" + "I48=TCGGCA\n"
      + "B1=CGATGT\n"
      + "B2=TGACCA\n" + "B3=ACAGTG\n" + "B4=GCCAAT\n" + "B5=CAGATC\n"
      + "B6=CTTGTA\n" + "B7=ATCACG\n" + "B8=TTAGGC\n" + "B9=ACTTGA\n"
      + "B10=GATCAG\n" + "B11=TAGCTT\n" + "B12=GGCTAC\n" + "B13=AGTCAA\n"
      + "B14=AGTTCC\n" + "B15=ATGTCA\n" + "B16=CCGTCC\n" + "B17=GTAGAG\n"
      + "B18=GTCCGC\n" + "B19=GTGAAA\n" + "B20=GTGGCC\n" + "B21=GTTTCG\n"
      + "B22=CGTACG\n" + "B23=GAGTGG\n" + "B24=GGTAGC\n" + "B25=ACTGAT\n"
      + "B26=ATGAGC\n" + "B27=ATTCCT\n" + "B28=CAAAAG\n" + "B29=CAACTA\n"
      + "B30=CACCGG\n" + "B31=CACGAT\n" + "B32=CACTCA\n" + "B33=CAGGCG\n"
      + "B34=CATGGC\n" + "B35=CATTTT\n" + "B36=CCAACA\n" + "B37=CGGAAT\n"
      + "B38=CTAGCT\n" + "B39=CTATAC\n" + "B40=CTCAGA\n" + "B41=GCGCTA\n"
      + "B42=TAATCG\n" + "B43=TACAGC\n" + "B44=TATAAT\n" + "B45=TCATTC\n"
      + "B46=TCCCGA\n" + "B47=TCGAAG\n" + "B48=TCGGCA\n" + "E1=ATCACG\n"
      + "E2=CGATGT\n" + "E3=TTAGGC\n" + "E4=TGACCA\n" + "E5=ACAGTG\n"
      + "E6=GCCAAT\n" + "E7=CAGATC\n" + "E8=ACTTGA\n" + "E9=GATCAG\n"
      + "E10=TAGCTT\n" + "E11=GGCTAC\n" + "E12=CTTGTA\n"
      + "M1=AACCAG\n" + "M2=TGGTGA\n" + "M3=AGTGAG\n" + "M4=GCACTA\n"
      + "M5=ACCTCA\n" + "M6=GTGCTT\n" + "M7=AAGCCT\n" + "M8=GTCGTA\n"
      + "M9=AAGAGG\n" + "M10=GGAGAA\n" + "M11=AGCATG\n" + "M12=GAGTCA\n"
      + "M13=CGTAGA\n" + "M14=TCAGAG\n" + "M15=CACAGT\n" + "M16=TTGGCA\n";


  private static String DEFAULT_RESULT_MSG = "<pre>No valid design entered.</pre>";
  // if none 
  private static String DEFAULT_GENOMES_MSG = "[No genomes list received.]";

  private final TextArea inputTextarea = new TextArea();
  private final TextArea indexesTextarea = new TextArea();
  private final TextArea genomesTextarea = new TextArea();
  private final TextArea helpTextarea = new TextArea();
  private final HTML outputHTML = new HTML();
  private final TextBox flowcellTextBox = new TextBox();
  private final Button button = new Button("Check the Casava design");

  private boolean first = true;

  public static final void updateDesignWithIndexes(final CasavaDesign design,
      final String indexes) {

    if (design == null || indexes == null)
      return;

    final Map<String, String> map = new HashMap<String, String>();

    String[] lines = indexes.split("\n");

    for (String line : lines) {

      if (line.trim().startsWith("#"))
        continue;

      String[] fields = line.split("=");
      if (fields.length != 2)
        continue;
      map.put(fields[0].trim(), fields[1].trim());
    }

    for (CasavaSample sample : design) {
      if (map.containsKey(sample.getIndex()))
        sample.setIndex(map.get(sample.getIndex()));
    }

  }

  public static final List<String> checkGenomesCasavaDesign(final CasavaDesign design, final String genomesFromForm) {
    
    if (genomesFromForm == null)
      return Collections.emptyList();
    
    List<String> availablGenomes = new ArrayList<String>();
    List<String> warnings = new ArrayList<String>();
    boolean first = true;
  
    for (String line : genomesFromForm.trim().split("\n")) {
      if (line.indexOf("=") > -1) {
        String[] l = line.split("=");
        availablGenomes.add(trimSpecificString(l[0]));
        availablGenomes.add(trimSpecificString(l[1]));
      }
    }

    Set<String> genomesDesign = new HashSet<String>();
    for (CasavaSample sample : design) {
      genomesDesign.add(sample.getSampleRef());
    }
    
    for (String genomeSample : genomesDesign) {
      if (!(availablGenomes.contains(trimSpecificString(genomeSample)))){
        if (first)
          warnings.add("\n");
        warnings.add(genomeSample + " not available for detection contaminant.");
        first = false;
      }
    }
    
    return warnings;
  }

  private static String trimSpecificString(final String s) {

    StringBuilder rep = new StringBuilder();
    String trimmed = s.trim().toLowerCase();
    
    String carToKeep = "abcdefghijklmnopqrstuvwxyz0123456789";
    for (int i = 0; i < s.length(); i++) {
      if (carToKeep.indexOf(trimmed.charAt(i)) != -1)
        rep.append(trimmed.charAt(i));
    }
    return rep.toString();
  }
  
  private String createListGenomes(final String genomesContentFile){
    StringBuilder s = new StringBuilder();
    // Header columns
    s.append("#latin name, reference name\n");
    for (String line : genomesContentFile.split("\n")) {
    
      if (! line.startsWith("#")){
        String[] tab = line.split("\t");
    
        if (tab.length > 4) {
          if (tab[4].trim().length() > 0){
            s.append(tab[4].trim());
            s.append("=");
          }  
          if (tab[2].trim().length() > 0)
            s.append(tab[2].trim());
            s.append("\n");
        }
      }
    }

    return s.toString();
  }

  private String getFlowcellId(final String s) {

    if (s == null || s.trim().length() == 0)
      return null;

    if (s.indexOf('_') == -1)
      return s.trim();

    String[] fields = s.split("_");

    if (fields==null || fields.length!=4)
      return null;

    String flowcellId = fields[3];
    if (flowcellId==null)
      return null;

    flowcellId = flowcellId.trim();
    if (flowcellId.length()<2)
      return null;

    return flowcellId.substring(1);
  }

  private String createWarningMessage(List<String> warnings) {

    final StringBuilder sb = new StringBuilder();
    sb.append("Warnings:\n");

    for (String warn : warnings) {
      sb.append("  - ");
      sb.append(warn);
      sb.append('\n');
    } 

    sb.append("\nAre you sure that your design is correct?");

    return sb.toString();
  }

  
  private void retrieveList(final TextArea textArea, final String list, final String url, final String defaultMsg) {
    
    final String txt = list == null || list.trim().length() == 0 ? defaultMsg : list.trim();
    
    if (url == null || url.trim().length() == 0){
      textArea.setText(txt);
    
    } else {
      try {
        
        loadFile(textArea, url); 
       
      } catch(Exception e){
        // Fail load file, used list send by param genomes
        Window.alert("Couldn't retrieve list: " + e.getMessage()); 
        textArea.setText(txt);
      }
    }    
  }
  
  private void loadFile(final TextArea textArea, final String url) throws Exception {
  
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
    
    Request request = builder.sendRequest(null, new RequestCallback() {
      
      public void onError(Request request, Throwable exception) {
        Window.alert("Couldn't retrieve list url (" + url + ")");
        
      }

      public void onResponseReceived(Request request, Response response) {
        if (200 == response.getStatusCode()) {
          
          String rep = response.getText();
          
          // EOF mark end of line
          rep = rep.replaceAll("EOL", "\n");
          
          textArea.setText("#latin name, reference name\n"+rep);
    
        } else {
          Window.alert("Couldn't retrieve list status (" + response.getStatusText() + ")");          
        }
      }
    });
  }
  
  
  public void onModuleLoad() {

    // Set the layouts
    final TabLayoutPanel tp = new TabLayoutPanel(1.5, Unit.EM);
    tp.add(new ScrollPanel(inputTextarea), "[Input Casava design]");
    tp.add(outputHTML, "[CSV Casava design]");
    tp.add(new ScrollPanel(indexesTextarea), "[Indexes Alias]");
    tp.add(new ScrollPanel(genomesTextarea), "[References Alias]");
    tp.add(new ScrollPanel(helpTextarea), "[Help]");
    
    tp.setHeight("100%");
    tp.setWidth("100%");

    //RootLayoutPanel rp = RootLayoutPanel.get();
    //rp.add(dlp);

    RootPanel.get("flowcellidFieldContainer").add(flowcellTextBox);
    RootPanel.get("sendButtonContainer").add(button);
    RootPanel.get("tabsContainer").add(tp);

    
    retrieveList(indexesTextarea, Window.Location.getParameter("indexlist"), Window.Location.getParameter("indexurl"), DEFAULT_INDEXES);
    
    // Initialize widget values
    // indexesTextarea.setText(DEFAULT_INDEXES);
    indexesTextarea.setVisibleLines(40);
    indexesTextarea.setSize("99%","100%");
    //indexesTextarea.setCharacterWidth(150);
    
    retrieveList(genomesTextarea, Window.Location.getParameter("genomeslist"),  Window.Location.getParameter("genomesurl"),DEFAULT_GENOMES_MSG);
    
    genomesTextarea.setVisibleLines(40);
    genomesTextarea.setSize("99%","100%");
    //genomesTextarea.setCharacterWidth(150);
    
    
    helpTextarea.setVisibleLines(40);
    helpTextarea.setSize("99%","100%");
    helpTextarea.setText("Help for the validator design file.");
    
    flowcellTextBox.setText(Window.Location.getParameter("id"));
    // loadDesignFile(Window.Location.getParameter("genomesurl"), flowcellTextBox.getText());
    
    inputTextarea.setText("[Paste here your Casava design]");
    //inputTextarea.setCharacterWidth(150);
    inputTextarea.setVisibleLines(40);
    inputTextarea.setSize("99%","100%");
    
    outputHTML.setHTML(DEFAULT_RESULT_MSG);

    // Set the action on button click
    this.button.addClickHandler(new ClickHandler() {

      public void onClick(ClickEvent event) {

        // Get input text
        String inputText = inputTextarea.getText();

        // Clear ouput
        outputHTML.setHTML(DEFAULT_RESULT_MSG);

        tp.selectTab(0);

        try {
          final CasavaDesign design;

          if (inputText.indexOf('\t')!=-1)
	    design = CasavaDesignUtil.parseTabulatedDesign(inputText);
          else
            design = CasavaDesignUtil.parseCSVDesign(inputText);

          updateDesignWithIndexes(design,
              indexesTextarea.getText());

          
              
          // Get the flowcell id
          final String flowcellId = getFlowcellId(flowcellTextBox.getText());
          //if (flowcellId == null)
          //  throw new EoulsanException("Invalid run id: " + flowcellTextBox.getText());

          // Check Casava design
          final List<String> warnings = 
            CasavaDesignUtil.checkCasavaDesign(design, flowcellId);
            
          // Check genomes Casava design
          warnings.addAll(checkGenomesCasavaDesign(design, genomesTextarea.getText()));
  
          if (warnings.size()==0 || Window.confirm(createWarningMessage(warnings))) {
       
            outputHTML.setHTML("<pre>"
                + CasavaDesignUtil.toCSV(design) + "</pre>");
            tp.selectTab(1);
          }
        } catch (IOException e) {
          Window.alert("Invalid design: " + e.getMessage());
        } catch (EoulsanException e) {
          Window.alert("Invalid design: " + e.getMessage());
        }

      }

    });

    // Clear tip message in input text area
    this.inputTextarea.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {

        if (first) {

          inputTextarea.setText("");
          first = false;
        }
      }
    });
  }
}
EOF

cat > $PROJECT_NAME/war/$PROJECT_NAME.html.tmp << EOF
<!doctype html>
<!-- The DOCTYPE declaration above will set the     -->
<!-- browser's rendering engine into                -->
<!-- "Standards Mode". Replacing this declaration   -->
<!-- with a "Quirks Mode" doctype is not supported. -->

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">

    <!--                                                               -->
    <!-- Consider inlining CSS to reduce the number of requested files -->
    <!--                                                               -->
    <link type="text/css" rel="stylesheet" href="$PROJECT_NAME.css">

    <!--                                           -->
    <!-- Any title is fine                         -->
    <!--                                           -->
    <title>CASAVA/BCL2FASTQ samplesheet validator</title>
    
    <!--                                           -->
    <!-- This script loads your compiled module.   -->
    <!-- If you add any GWT meta tags, they must   -->
    <!-- be added before this line.                -->
    <!--                                           -->
    <script language="javascript">
      document.domain = "idunn.ens.fr";
    </script>
    <script type="text/javascript" language="javascript" src="designvalidator/designvalidator.nocache.js"></script>
  </head>

  <!--                                           -->
  <!-- The body can have arbitrary html, or      -->
  <!-- you can leave the body empty if you want  -->
  <!-- to create a completely dynamic UI.        -->
  <!--                                           -->
  <body>

    <!-- OPTIONAL: include this if you want history support -->
    <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>
    
    <!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
    <noscript>
      <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
      </div>
    </noscript>

<h4 align="right">__VERSION__</h4>
<h1>CASAVA/BCL2FASTQ samplesheet validator</h1>
    <a href="http://www.transcriptome.ens.fr" id="bannerLeft"><img src="http://www.transcriptome.ens.fr/aozan/images/logo_genomicpariscentre-90pxh.png" alt="logo genomic paris centre"/></a>
    <table align="center">
      <!--tr>
        <td colspan="2" style="font-weight:bold;">Please enter your name:</td>        
      </tr-->
      <tr>
        <td>Flow cell id or run id (optional):</td>
        <td id="flowcellidFieldContainer"></td>
        <td id="nameFieldContainer"></td>
        <td id="sendButtonContainer"></td>
      </tr>
      <tr>
        <!td colspan="2" style="color:red;" id="errorLabelContainer"></td-->
      </tr>
    </table>
   
    <!--p/--> 
  
    <table align="center" width="90%" >
      <tr><td id="tabsContainer" height="700px"/></tr>
    </table>

    <!--p/--> 
        
    <!--table align="center" >
      <tr><td id="sendButtonContainer/></tr>
    </table-->


 
  </body>
</html>
EOF

if [ -z "$GIT_REVISION" ]; then
	GIT_REVISION=""
else
	GIT_REVISION="Revision $GIT_REVISION"
fi
sed "s/__VERSION__/$GIT_REVISION/" $PROJECT_NAME/war/$PROJECT_NAME.html.tmp   > $PROJECT_NAME/war/$PROJECT_NAME.html 
rm $PROJECT_NAME/war/$PROJECT_NAME.html.tmp

# Compile
cd $PROJECT_NAME
ant build

mv war/$PROJECT_NAME.html war/index.html
mv war ../$PROJECT_NAME-tmp
rm -rf ../$PROJECT_NAME-tmp/WEB-INF
cd ..
rm -rf $PROJECT_NAME
mv $PROJECT_NAME-tmp `echo $PROJECT_NAME | tr '[A-Z]' '[a-z]'`

