package com.gkaraffa.amati.control;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.gkaraffa.cremona.helper.ChordHelper;
import com.gkaraffa.cremona.helper.ScaleHelper;
import com.gkaraffa.cremona.theoretical.Tone;
import com.gkaraffa.cremona.theoretical.chord.Chord;
import com.gkaraffa.cremona.theoretical.scale.DiatonicScale;
import com.gkaraffa.cremona.theoretical.scale.Scale;
import com.gkaraffa.guarneri.outputform.CSVOutputFormFactory;
import com.gkaraffa.guarneri.outputform.OutputForm;
import com.gkaraffa.guarneri.outputform.OutputFormFactory;
import com.gkaraffa.guarneri.outputform.TabularTextOutputFormFactory;
import com.gkaraffa.guarneri.view.ViewFactory;
import com.gkaraffa.guarneri.view.ViewQuery;
import com.gkaraffa.guarneri.view.ViewQueryBuilder;
import com.gkaraffa.guarneri.view.ViewTable;
import com.gkaraffa.guarneri.view.analytic.key.ParallelModeAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.scale.IntervalAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.scale.RomanNumeralAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.scale.StepPatternAnalyticFactory;
import com.gkaraffa.guarneri.view.instrument.GuitarViewFactory;

public class MainController {

  public static void main(String[] args) {
    MainController mainController = new MainController();
    Arguments arguments = new Arguments();
    JCommander.newBuilder().addObject(arguments).build().parse(args);
    mainController.run(arguments);
  }

  public void run(Arguments arguments) {
    try {
      boolean helpRequest = arguments.getHelpRequest();
      if (helpRequest) {
        displayHelp();
        return;
      }

      String typeRequest = this.trimAndValidateType(arguments.getTypeRequest());
      String formatRequest = this.trimAndValidateFormat(arguments.getFormatRequest());
      OutputFormFactory viewFactory = this.selectAndCreateOuputFormFactory(formatRequest);
      List<ViewTable> viewTables = null;

      switch (typeRequest) {
        case "KEY":
          viewTables = this.parseAndRenderKeyAnalytics(arguments.getKeyRequest());
          break;
        case "SCALE":
          viewTables = this.parseAndRenderScaleAnalytics(arguments.getKeyRequest(),
              arguments.getScaleRequest());
          break;
        case "GUITAR":
          viewTables = this.parseAndRenderGuitarAnalytic(arguments.getKeyRequest(),
              arguments.getScaleRequest(), arguments.getChordRequest());
          break;
        default:
          throw new IllegalArgumentException();
      }

      List<OutputForm> views = this.renderAnalytics(viewTables, viewFactory);

      this.createOutput(arguments.getOutputFileName(), views);
    }
    catch (IllegalArgumentException iAE) {
      iAE.printStackTrace();
    }
  }

  private void displayHelp() {
    String helpText = "Amati - a command line music theory tool\n" + "Build: \n\n"
        + "--help, -h \t help/options screen \n" + "--type, -t \t analytic type {key, scale} \n"
        + "--format, -f \t output format {txt, csv} \n"
        + "--key, -k \t key (required for key, scale, or guitar analytic) \n"
        + "--scale, -s \t scale (required for scale, or guitar analytic) \n"
        + "--chord, -c \t chord (for guitar analytic only)"
        + "--output, -o \t output file path/filename" + "";

    System.out.println(helpText);
  }

  private String trimAndValidateType(String typeRequest) throws IllegalArgumentException {
    if (typeRequest == null) {
      throw new IllegalArgumentException("Run type not specified.");
    }

    typeRequest = typeRequest.trim().toUpperCase();
    if (!(typeRequest.equals("KEY") || typeRequest.equals("SCALE")
        || typeRequest.equals("GUITAR"))) {
      throw new IllegalArgumentException("Unexpected run type.");
    }

    return typeRequest;
  }

  private String trimAndValidateFormat(String formatString) throws IllegalArgumentException {
    if (formatString == null) {
      throw new IllegalArgumentException("Format not specified.");
    }

    formatString = formatString.trim().toUpperCase();
    if (!(formatString.contentEquals("TEXT")) && !(formatString.contentEquals("TXT"))
        && !(formatString.contentEquals("CSV"))) {
      throw new IllegalArgumentException("Unexpected format.");
    }

    return formatString;
  }

  private OutputFormFactory selectAndCreateOuputFormFactory(String outputFormat) {
    switch (outputFormat) {
      case "CSV":
        return new CSVOutputFormFactory();
      case "TXT":
        return new TabularTextOutputFormFactory();
      default:
        return new TabularTextOutputFormFactory();
    }
  }

  private List<ViewTable> parseAndRenderKeyAnalytics(String keyString)
      throws IllegalArgumentException {
    List<ViewTable> viewTables = new ArrayList<>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    vQB.insertCriteria("Key", this.parseAndValidateKey(keyString));
    ViewQuery viewQuery = vQB.compileViewQuery();
    viewTables.add(this.getParallelModeAnalytic(viewQuery));

    return viewTables;
  }

  private List<ViewTable> parseAndRenderScaleAnalytics(String keyString, String scaleString) {
    List<ViewTable> viewTables = new ArrayList<>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();
    Scale scale = this.parseAndValidateScale(keyString, scaleString);

    vQB.insertCriteria("Scale", scale);
    ViewQuery viewQuery = vQB.compileViewQuery();

    viewTables.add(this.getRomanNumeralAnalytic(viewQuery));
    viewTables.add(this.getIntervalAnalytic(viewQuery));
    viewTables.add(this.getStepPatternAnalytic(viewQuery));

    return viewTables;
  }

  private String getQueryType(String keyString, String scaleString, String chordString)
      throws IllegalArgumentException {

    if (keyString.equals("null")) {
      throw new IllegalArgumentException("Key must be specified for Guitar analytic.");
    }

    String secondSpec = scaleString + chordString;
    if (secondSpec.equals("nullnull") || !secondSpec.contains("null")) {
      throw new IllegalArgumentException(
          "Either a scale, or a chord must be specified for a Guitar analytic.");
    }

    if (!scaleString.contains("null")) {
      return "SCALE";
    }

    if (!chordString.contains("null")) {
      return "CHORD";
    }

    throw new IllegalArgumentException("Invalid arguments for Guitar analytic.");
  }

  private List<ViewTable> parseAndRenderGuitarAnalytic(String keyString, String scaleString,
      String chordString) throws IllegalArgumentException {
    String queryType = getQueryType(keyString, scaleString, chordString);
    ViewQueryBuilder vQB = new ViewQueryBuilder();
    ViewQuery viewQuery = null;

    switch (queryType) {
      case "CHORD":
        Chord chord = this.parseAndValidateChord(keyString, chordString);
        vQB.insertCriteria("ToneGroupObject", chord);
        break;
      case "SCALE":
        Scale scale = this.parseAndValidateScale(keyString, scaleString);
        vQB.insertCriteria("ToneGroupObject", scale);
        break;
      default:
        throw new IllegalArgumentException("No query parameters specified");
    }

    viewQuery = vQB.compileViewQuery();

    List<ViewTable> viewTables = new ArrayList<>();
    viewTables.add(this.getGuitarAnalytic(viewQuery));

    return viewTables;
  }

  private Tone parseAndValidateKey(String keyString) throws IllegalArgumentException {
    if (keyString == null) {
      throw new IllegalArgumentException("Key not specified.");
    }

    Tone keyTone = Tone.stringToTone(keyString.trim().toUpperCase());

    return keyTone;
  }

  private Scale parseAndValidateScale(String keyString, String scaleString)
      throws IllegalArgumentException {
    if (scaleString == null) {
      throw new IllegalArgumentException("Scale not specified.");
    }

    Scale scale = this.parseAndRenderScale(keyString, scaleString);

    return scale;
  }

  private Chord parseAndValidateChord(String keyString, String chordString)
      throws IllegalArgumentException {
    if (chordString == null) {
      throw new IllegalArgumentException("Chord not specified.");
    }

    Chord chord = this.parseAndRenderChord(keyString, chordString);

    return chord;
  }

  private Scale parseAndRenderScale(String keyRequest, String scaleRequest) {
    ScaleHelper helper = ScaleHelper.getInstance();
    Scale scaleRendered = helper.getScale(keyRequest, scaleRequest);

    return scaleRendered;
  }

  private Chord parseAndRenderChord(String keyRequest, String chordRequest) {
    ChordHelper helper = ChordHelper.getInstance();
    Chord chordRendered = helper.getChord(keyRequest, chordRequest);

    return chordRendered;
  }

  private List<OutputForm> renderAnalytics(List<ViewTable> modelsRendered,
      OutputFormFactory viewFactory) {
    List<OutputForm> analytics = new ArrayList<>();

    for (ViewTable modelTable : modelsRendered) {
      analytics.add(viewFactory.renderView(modelTable));
    }

    return analytics;
  }


  private ViewTable getParallelModeAnalytic(ViewQuery viewQuery) {
    ViewFactory viewFactory = new ParallelModeAnalyticViewFactory();

    return viewFactory.createView(viewQuery);
  }

  private ViewTable getRomanNumeralAnalytic(ViewQuery viewQuery) throws IllegalArgumentException {
    if ((Scale) viewQuery.getCriteria("Scale") instanceof DiatonicScale) {
      ViewFactory viewFactory = new RomanNumeralAnalyticViewFactory();
      return viewFactory.createView(viewQuery);
    }
    else {
      throw new IllegalArgumentException("RomanNumeralAnalytic cannot be rendered for this scale");
    }
  }

  private ViewTable getIntervalAnalytic(ViewQuery viewQuery) throws IllegalArgumentException {
    if ((Scale) viewQuery.getCriteria("Scale") instanceof DiatonicScale) {
      ViewFactory viewFactory = new IntervalAnalyticViewFactory();
      return viewFactory.createView(viewQuery);
    }
    else {
      throw new IllegalArgumentException("IntervalAnalytic view cannot be rendered for this scale");
    }
  }

  private ViewTable getGuitarAnalytic(ViewQuery viewQuery) throws IllegalArgumentException {
    ViewFactory viewFactory = new GuitarViewFactory();
    return viewFactory.createView(viewQuery);
  }

  private ViewTable getStepPatternAnalytic(ViewQuery viewQuery) {
    ViewFactory viewFactory = new StepPatternAnalyticFactory();
    return viewFactory.createView(viewQuery);
  }

  private void createOutput(String outputFileName, List<OutputForm> views) {
    if ((outputFileName == null) || (outputFileName.trim().equals(""))) {
      this.writeOutputToStdOut(views);
    }
    else {
      this.writeOutputToFile(outputFileName, views);
    }
  }

  private void writeOutputToStdOut(List<OutputForm> views) {
    for (OutputForm view : views) {
      System.out.println(view.toString());
    }
  }

  private void writeOutputToFile(String outputFileName, List<OutputForm> views) {
    File file = new File(outputFileName);

    try (FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream writer = new BufferedOutputStream(fileOutputStream)) {

      for (OutputForm view : views) {
        byte[] buffer = view.getByteArray();
        writer.write(buffer, 0, buffer.length);
      }
    }
    catch (IOException iOE) {
      iOE.printStackTrace();
    }
  }
  
}
