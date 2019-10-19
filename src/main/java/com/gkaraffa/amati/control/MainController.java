package com.gkaraffa.amati.control;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.IStringConverter;
import com.gkaraffa.cremona.helper.ScaleHelper;
import com.gkaraffa.cremona.theoretical.scale.DiatonicScale;
import com.gkaraffa.cremona.theoretical.scale.Scale;
import com.gkaraffa.guarneri.outputform.CSVOutputFormFactory;
import com.gkaraffa.guarneri.outputform.OutputForm;
import com.gkaraffa.guarneri.outputform.OutputFormFactory;
import com.gkaraffa.guarneri.outputform.TabularTextOutputFormFactory;
import com.gkaraffa.guarneri.view.ViewFactory;
import com.gkaraffa.guarneri.view.ViewQuery;
import com.gkaraffa.guarneri.view.ViewTable;
import com.gkaraffa.guarneri.view.analytic.IntervalAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.RomanNumeralAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.ScalarAnalyticViewFactory;

public class MainController {
  private Arguments arguments = null;

  public static void main(String[] args) {
    MainController mainController = new MainController();
    Arguments arguments = new Arguments();
    JCommander.newBuilder().addObject(arguments).build().parse(args);
    mainController.run(arguments);
  }

  public void run(Arguments arguments) {
    this.arguments = arguments;
    Scale scaleRendered = this.parseAndRenderScale();
    List<ViewTable> modelsRendered = this.parseAndRenderAnalytics(scaleRendered);
    OutputFormFactory viewFactory = this.selectCreateViewFactory(arguments.getFormatRequest());
    List<OutputForm> views = this.renderAnalytics(modelsRendered, viewFactory);

    this.createOutput(views);
  }

  private void writeOutputToStdOut(List<OutputForm> views) {
    for (OutputForm view : views) {
      System.out.println(view.toString());
    }
  }

  private void writeOutputToFile(List<OutputForm> views) {
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

  private List<OutputForm> renderAnalytics(List<ViewTable> modelsRendered, OutputFormFactory viewFactory) {
    List<OutputForm> analytics = new ArrayList<OutputForm>();

    for (ViewTable modelTable : modelsRendered) {
      analytics.add(viewFactory.renderView(modelTable));
    }

    return analytics;
  }

  private Scale parseAndRenderScale() {
    ScaleHelper helper = ScaleHelper.getInstance();
    Scale scaleRendered = helper.getScale(arguments.getKeyRequest(), arguments.getScaleRequest());

    return scaleRendered;
  }

  private List<ViewTable> parseAndRenderAnalytics(Scale scaleRendered) {
    List<ViewTable> viewsRendered = new ArrayList<ViewTable>();
    List<String> viewRequests = arguments.getViewRequests();

    for (String viewRequest : viewRequests) {
      try {
        switch (viewRequest.toUpperCase().trim()) {
          case "ROMAN":
            viewsRendered.add(getRomanNumeralModel(scaleRendered));
            break;
          case "INTERVAL":
            viewsRendered.add(getIntervalModel(scaleRendered));
            break;
          case "SCALAR":
            viewsRendered.add(getScalarModel(scaleRendered));
            break;
        }
      }
      catch (IllegalArgumentException iAE) {
        iAE.printStackTrace();
      }
    }

    return viewsRendered;
  }

  private ViewTable getRomanNumeralModel(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      ViewFactory viewFactory = new RomanNumeralAnalyticViewFactory();
      return viewFactory.createView(new ViewQuery(scale));
    }
    else {
      throw new IllegalArgumentException("RomanNumeralAnalytic cannot be rendered for this scale");
    }
  }

  private ViewTable getIntervalModel(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      ViewFactory viewFactory = new IntervalAnalyticViewFactory();
      return viewFactory.createView(new ViewQuery(scale));
    }
    else {
      throw new IllegalArgumentException("IntervalAnalytic view cannot be rendered for this scale");
    }
  }

  private ViewTable getScalarModel(Scale scale) {
    ViewFactory viewFactory = new ScalarAnalyticViewFactory();
    return viewFactory.createView(new ViewQuery(scale));
  }


  private OutputFormFactory selectCreateViewFactory(String formatRequest) {
    OutputFormat outputFormat = OutputFormat.getOutputFormat(formatRequest);
    switch (outputFormat) {
      case CSV:
        return new CSVOutputFormFactory();
      case TXT:
        return new TabularTextOutputFormFactory();
      default:
        return new TabularTextOutputFormFactory();
    }
  }

  private void createOutput(List<OutputForm> views) {
    String outputFileName = arguments.getOutputFileName();
    if ((outputFileName == null) || (outputFileName.trim().equals(""))) {
      writeOutputToStdOut(views);
    }
    else {
      writeOutputToFile(views);
    }
  }
}
