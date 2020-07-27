package com.gkaraffa.amati.control;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.gkaraffa.cremona.helper.ScaleHelper;
import com.gkaraffa.cremona.theoretical.Tone;
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

public class MainController {

  public static void main(String[] args) {
    MainController mainController = new MainController();
    Arguments arguments = new Arguments();
    JCommander.newBuilder().addObject(arguments).build().parse(args);
    mainController.run(arguments);
  }

  public void run(Arguments arguments) {
    try {
      String typeRequest = this.parseAndValidateType(arguments.getTypeRequest());
      String formatRequest = this.parseAndValidateFormat(arguments.getFormatRequest());

      OutputFormFactory viewFactory = this.selectCreateOutputFormFactory(formatRequest);
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

  private String parseAndValidateType(String typeRequest) throws IllegalArgumentException {
    if (typeRequest == null) {
      throw new IllegalArgumentException("Run type not specified.");
    }

    typeRequest = typeRequest.trim().toUpperCase();
    if (!(typeRequest.equals("KEY") || typeRequest.equals("SCALE"))) {
      throw new IllegalArgumentException("Unexpected run type.");
    }

    return typeRequest;
  }

  private String parseAndValidateFormat(String formatString) throws IllegalArgumentException {
    if (formatString == null) {
      throw new IllegalArgumentException("Format not specified.");
    }

    formatString = formatString.trim().toUpperCase();
    if (!(formatString.contentEquals("TXT")) && !(formatString.contentEquals("CSV"))) {
      throw new IllegalArgumentException("Unexpected format.");
    }

    return formatString;
  }

  private OutputFormFactory selectCreateOutputFormFactory(String outputFormat) {
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
    List<ViewTable> viewTables = new ArrayList<ViewTable>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    vQB.insertCriteria("Key", this.parseAndValidateKey(keyString));
    ViewQuery viewQuery = vQB.compileViewQuery();
    viewTables.add(this.getParallelModeAnalytic(viewQuery));

    return viewTables;
  }

  private List<ViewTable> parseAndRenderScaleAnalytics(String keyString, String scaleString) {
    List<ViewTable> viewTables = new ArrayList<ViewTable>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();
    Scale scale = this.parseAndValidateScale(keyString, scaleString);

    vQB.insertCriteria("Scale", scale);
    ViewQuery viewQuery = vQB.compileViewQuery();

    viewTables.add(this.getRomanNumeralAnalytic(viewQuery));
    viewTables.add(this.getIntervalAnalytic(viewQuery));
    viewTables.add(this.getStepPatternAnalytic(viewQuery));

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

  private Scale parseAndRenderScale(String keyRequest, String scaleRequest) {
    ScaleHelper helper = ScaleHelper.getInstance();
    Scale scaleRendered = helper.getScale(keyRequest, scaleRequest);

    return scaleRendered;
  }

  private List<OutputForm> renderAnalytics(List<ViewTable> modelsRendered,
      OutputFormFactory viewFactory) {
    List<OutputForm> analytics = new ArrayList<OutputForm>();

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
