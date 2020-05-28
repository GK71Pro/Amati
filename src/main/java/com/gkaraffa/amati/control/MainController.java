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
  /*
  private Arguments arguments = null;
  private RuntimeObjects runtimeObjects = null;
  private Branch branch = null;
  */


  public static void main(String[] args) {
    MainController mainController = new MainController();
    Arguments arguments = new Arguments();
    JCommander.newBuilder().addObject(arguments).build().parse(args);
    mainController.run(arguments);
  }

  public void run(Arguments arguments) {
    Branch branch = this.determineBranch(arguments);
    OutputFormFactory viewFactory = this.selectCreateOutputFormFactory(arguments);
    List<ViewTable> viewTables = null;

    switch (branch) {
      case KEY:
        viewTables = this.parseAndRenderKeyAnalytics(arguments);
        break;

      case SCALE:
        viewTables = this.parseAndRenderScaleAnalytics(arguments);
        break;

      default:
        throw new IllegalArgumentException();
    }

    List<OutputForm> views = this.renderAnalytics(viewTables, viewFactory);
    this.createOutput(arguments, views);
  }

  private List<ViewTable> parseAndRenderKeyAnalytics(Arguments arguments) {
    List<ViewTable> viewTables = new ArrayList<ViewTable>();
    Tone requestedKey = Tone.stringToTone(arguments.getKeyRequest().trim().toUpperCase());
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    vQB.insertCriteria("Key", requestedKey);
    ViewQuery viewQuery = vQB.compileViewQuery();
    viewTables.add(this.getParallelModeAnalyticFactory(viewQuery));

    return viewTables;
  }


  private List<ViewTable> parseAndRenderScaleAnalytics(Arguments arguments) {
    List<ViewTable> viewTables = new ArrayList<ViewTable>();
    Scale requestedScale =
        this.parseAndRenderScale(arguments.getKeyRequest(), arguments.getScaleRequest());
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    vQB.insertCriteria("Scale", requestedScale);
    ViewQuery viewQuery = vQB.compileViewQuery();

    viewTables.add(this.getRomanNumeralAnalytic(viewQuery));
    viewTables.add(this.getIntervalAnalytic(viewQuery));
    viewTables.add(this.getStepPatternAnalytic(viewQuery));

    return viewTables;
  }

  private Branch determineBranch(Arguments arguments) {
    Branch branch = null;
    String requestType = arguments.getRequestType().trim().toUpperCase();

    switch (requestType) {
      case "KEY":
        branch = Branch.KEY;
        break;
      case "SCALE":
        branch = Branch.SCALE;
        break;
      default:
        throw new IllegalArgumentException();
    }

    return branch;
  }



  private void writeOutputToStdOut(List<OutputForm> views) {
    for (OutputForm view : views) {
      System.out.println(view.toString());
    }
  }

  private void writeOutputToFile(Arguments arguments, List<OutputForm> views) {
    File file = new File(arguments.getOutputFileName().trim());

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

  private List<OutputForm> renderAnalytics(List<ViewTable> modelsRendered,
      OutputFormFactory viewFactory) {
    List<OutputForm> analytics = new ArrayList<OutputForm>();

    for (ViewTable modelTable : modelsRendered) {
      analytics.add(viewFactory.renderView(modelTable));
    }

    return analytics;
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

  private ViewTable getParallelModeAnalyticFactory(ViewQuery viewQuery) {
    ViewFactory viewFactory = new ParallelModeAnalyticViewFactory();

    return viewFactory.createView(viewQuery);
  }

  private OutputFormFactory selectCreateOutputFormFactory(Arguments arguments) {
    OutputFormat outputFormat =
        OutputFormat.getOutputFormat(arguments.getFormatRequest().trim().toUpperCase());

    switch (outputFormat) {
      case CSV:
        return new CSVOutputFormFactory();
      case TXT:
        return new TabularTextOutputFormFactory();
      default:
        return new TabularTextOutputFormFactory();
    }
  }

  private void createOutput(Arguments arguments, List<OutputForm> views) {
    String outputFileName = arguments.getOutputFileName();

    if ((outputFileName == null) || (outputFileName.trim().equals(""))) {
      this.writeOutputToStdOut(views);
    }
    else {
      this.writeOutputToFile(arguments, views);
    }
  }

  enum Branch {
    KEY("KEY"),
    SCALE("SCALE");

    Branch(String text) {
      this.text = text;
    }

    private final String text;

    public final String getText() {
      return text;
    }

    @Override
    public final String toString() {
      return text;
    }
  }

}
