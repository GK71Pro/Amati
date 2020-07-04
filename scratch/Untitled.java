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
import com.gkaraffa.cremona.theoretical.ToneGroupObject;
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
      RuntimeObject runtimeObject = this.argumentParseAndValidation(arguments);
      OutputFormFactory viewFactory =
          this.selectCreateOutputFormFactory(runtimeObject.formatRequest);
      List<ViewTable> viewTables = null;

      switch (runtimeObject.typeRequest) {
        case "KEY":
          viewTables = this.parseAndRenderKeyAnalytics(runtimeObject);
          break;
        case "SCALE":
          viewTables = this.parseAndRenderScaleAnalytics(runtimeObject);
          break;
        case "GUITAR":
          break;
        default:
          throw new IllegalArgumentException();
      }

      List<OutputForm> views = this.renderAnalytics(viewTables, viewFactory);

      this.createOutput(runtimeObject, views);
    }
    catch (IllegalArgumentException iAE) {
      iAE.printStackTrace();
    }
  }

  private RuntimeObject argumentParseAndValidation(Arguments arguments)
      throws IllegalArgumentException {
    RuntimeObject runtimeObject = new RuntimeObject();

    runtimeObject.typeRequest = this.parseAndValidateType(arguments);

    switch (runtimeObject.typeRequest) {
      case "SCALE":
        runtimeObject.keyRequest = this.parseAndValidateKey(arguments);
        runtimeObject.scaleRequest = this.parseAndValidateScale(arguments);
        break;
      case "KEY":
        runtimeObject.keyRequest = this.parseAndValidateKey(arguments);
        break;
      case "GUITAR":

      default:
        throw new IllegalArgumentException();
    }

    runtimeObject.formatRequest = this.parseAndValidateFormat(arguments);
    runtimeObject.outputFileName =
        this.parseAndValidateOutputFile(arguments, runtimeObject.formatRequest);

    return runtimeObject;
  }

  private String parseAndValidateType(Arguments arguments) throws IllegalArgumentException {
    String typeRequest = arguments.getTypeRequest();
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

  private Tone parseAndValidateKey(Arguments arguments) throws IllegalArgumentException {
    String keyString = arguments.getKeyRequest();

    if (keyString == null) {
      throw new IllegalArgumentException("Key not specified.");
    }

    Tone keyTone = Tone.stringToTone(keyString.trim().toUpperCase());

    return keyTone;
  }

  private Scale parseAndValidateScale(Arguments arguments) throws IllegalArgumentException {
    String scaleString = arguments.getScaleRequest();

    if (scaleString == null) {
      throw new IllegalArgumentException("Scale not specified.");
    }

    Scale scale = this.parseAndRenderScale(arguments.getKeyRequest(), arguments.getScaleRequest());

    return scale;
  }

  private OutputFormat parseAndValidateFormat(Arguments arguments) throws IllegalArgumentException {
    String formatString = arguments.getFormatRequest();

    if (formatString == null) {
      throw new IllegalArgumentException("Format not specified");
    }

    formatString = formatString.trim().toUpperCase();
    OutputFormat outputFormat = OutputFormat.getOutputFormat(formatString);

    return outputFormat;
  }

  private String parseAndValidateOutputFile(Arguments arguments, OutputFormat outputFormat)
      throws IllegalArgumentException {
    String outputFileName = arguments.getOutputFileName();

    if (outputFileName == null) {
      if ((outputFormat == OutputFormat.XLS) || (outputFormat == OutputFormat.XLSX)) {
        throw new IllegalArgumentException("External file must be specified for given format.");
      }
      else {
        return null;
      }
    }

    return outputFileName.trim();
  }


  private List<ViewTable> parseAndRenderKeyAnalytics(RuntimeObject runtimeObject) {
    List<ViewTable> viewTables = new ArrayList<ViewTable>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    vQB.insertCriteria("Key", runtimeObject.keyRequest);
    ViewQuery viewQuery = vQB.compileViewQuery();
    viewTables.add(this.getParallelModeAnalyticFactory(viewQuery));

    return viewTables;
  }

  private List<ViewTable> parseAndRenderScaleAnalytics(RuntimeObject runtimeObject) {
    List<ViewTable> viewTables = new ArrayList<ViewTable>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    vQB.insertCriteria("Scale", runtimeObject.scaleRequest);
    ViewQuery viewQuery = vQB.compileViewQuery();

    viewTables.add(this.getRomanNumeralAnalytic(viewQuery));
    viewTables.add(this.getIntervalAnalytic(viewQuery));
    viewTables.add(this.getStepPatternAnalytic(viewQuery));

    return viewTables;
  }

  private List<ViewTable> parseAndRenderGuitarAnalytics(RuntimeObject runtimeObject) {
    List<ViewTable> viewTables = new ArrayList<ViewTable>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    // vQB.insertCriteria("Scale", runtimeObject.scaleRequest);
    vQB.insertCriteria("ToneGroupObject", runtimeObject.scaleRequest);
    ViewQuery viewQuery = vQB.compileViewQuery();

    viewTables.add(this.getRomanNumeralAnalytic(viewQuery));
    viewTables.add(this.getIntervalAnalytic(viewQuery));
    viewTables.add(this.getStepPatternAnalytic(viewQuery));

    return viewTables;
  }

  private List<OutputForm> renderAnalytics(List<ViewTable> modelsRendered,
      OutputFormFactory viewFactory) {
    List<OutputForm> analytics = new ArrayList<OutputForm>();

    for (ViewTable modelTable : modelsRendered) {
      analytics.add(viewFactory.renderView(modelTable));
    }

    return analytics;
  }

  private void createOutput(RuntimeObject runtimeObject, List<OutputForm> views) {
    String outputFileName = runtimeObject.outputFileName;

    if ((outputFileName == null) || (outputFileName.trim().equals(""))) {
      this.writeOutputToStdOut(views);
    }
    else {
      this.writeOutputToFile(runtimeObject, views);
    }
  }

  private ViewTable getParallelModeAnalyticFactory(ViewQuery viewQuery) {
    ViewFactory viewFactory = new ParallelModeAnalyticViewFactory();

    return viewFactory.createView(viewQuery);
  }

  private Scale parseAndRenderScale(String keyRequest, String scaleRequest) {
    ScaleHelper helper = ScaleHelper.getInstance();
    Scale scaleRendered = helper.getScale(keyRequest, scaleRequest);

    return scaleRendered;
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

  private void writeOutputToStdOut(List<OutputForm> views) {
    for (OutputForm view : views) {
      System.out.println(view.toString());
    }
  }

  private void writeOutputToFile(RuntimeObject runtimeObject, List<OutputForm> views) {
    File file = new File(runtimeObject.outputFileName);

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

  private OutputFormFactory selectCreateOutputFormFactory(OutputFormat outputFormat) {
    switch (outputFormat) {
      case CSV:
        return new CSVOutputFormFactory();
      case TXT:
        return new TabularTextOutputFormFactory();
      default:
        return new TabularTextOutputFormFactory();
    }
  }

  class RuntimeObject {
    String typeRequest = null;
    Tone keyRequest = null;
    Scale scaleRequest = null;
    OutputFormat formatRequest = null;
    String outputFileName = null;
  }
}
