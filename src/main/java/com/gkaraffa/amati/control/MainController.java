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
import com.gkaraffa.cremona.helper.Helper;
import com.gkaraffa.cremona.theoretical.scale.DiatonicScale;
import com.gkaraffa.cremona.theoretical.scale.Scale;
import com.gkaraffa.guarneri.outputform.CSVOutputFormFactory;
import com.gkaraffa.guarneri.outputform.OutputForm;
import com.gkaraffa.guarneri.outputform.OutputFormFactory;
import com.gkaraffa.guarneri.outputform.TextOutputFormFactory;
import com.gkaraffa.guarneri.view.ViewFactory;
import com.gkaraffa.guarneri.view.ViewTable;
import com.gkaraffa.guarneri.view.analytic.IntervalAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.RomanNumeralAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.ScalarAnalyticViewFactory;

public class MainController {
  @Parameter(names = {"--key", "-k"})
  private String keyRequest;

  @Parameter(names = {"--scale", "-s"})
  private String scaleRequest;

  @Parameter(names = {"--format", "-f"})
  private String formatRequest = "text";

  @Parameter(names = "--views", listConverter = ViewListConverter.class)
  List<String> viewRequests;

  @Parameter(names = {"--output", "-o"})
  private String outputFileName;


  public static void main(String[] args) {
    MainController mainController = new MainController();
    JCommander.newBuilder().addObject(mainController).build().parse(args);

    mainController.run();
  }

  public void run() {
    Scale scaleRendered = this.parseAndRenderScale();
    List<ViewTable> modelsRendered = this.parseAndRenderModels(scaleRendered);
    OutputFormFactory viewFactory = this.selectCreateViewFactory(formatRequest);
    List<OutputForm> views = this.renderViews(modelsRendered, viewFactory);

    this.createOutput(views);
  }

  private void writeOutputToStdOut(List<OutputForm> views) {
    for (OutputForm view : views) {
      System.out.println(view.toString());
    }
  }

  private void writeOutputToFile(List<OutputForm> views) {
    File file = new File(this.outputFileName.trim());
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

  private List<OutputForm> renderViews(List<ViewTable> modelsRendered, OutputFormFactory viewFactory) {
    List<OutputForm> views = new ArrayList<OutputForm>();

    for (ViewTable modelTable : modelsRendered) {
      views.add(viewFactory.renderView(modelTable));
    }

    return views;
  }

  private Scale parseAndRenderScale() {
    Helper helper = Helper.getInstance();
    Scale scaleRendered = helper.getScale(keyRequest, scaleRequest);

    return scaleRendered;
  }

  private List<ViewTable> parseAndRenderModels(Scale scaleRendered) {
    List<ViewTable> modelsRendered = new ArrayList<ViewTable>();

    for (String viewRequest : this.viewRequests) {
      try {
        switch (viewRequest.toUpperCase().trim()) {
          case "ROMAN":
            modelsRendered.add(getRomanNumeralModel(scaleRendered));
            break;
          case "INTERVAL":
            modelsRendered.add(getIntervalModel(scaleRendered));
            break;
          case "SCALAR":
            modelsRendered.add(getScalarModel(scaleRendered));
            break;
        }
      }
      catch (IllegalArgumentException iAE) {
        iAE.printStackTrace();
      }
    }

    return modelsRendered;
  }

  private ViewTable getRomanNumeralModel(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      ViewFactory modelFactory = new RomanNumeralAnalyticViewFactory();
      return modelFactory.createModel(scale);
    }
    else {
      throw new IllegalArgumentException("RomanNumeralAnalytic cannot be rendered for this scale");
    }
  }

  private ViewTable getIntervalModel(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      ViewFactory modelFactory = new IntervalAnalyticViewFactory();
      return modelFactory.createModel(scale);
    }
    else {
      throw new IllegalArgumentException("IntervalAnalytic view cannot be rendered for this scale");
    }
  }

  private ViewTable getScalarModel(Scale scale) {
    ViewFactory modelFactory = new ScalarAnalyticViewFactory();
    return modelFactory.createModel(scale);
  }


  private OutputFormFactory selectCreateViewFactory(String formatRequest) {
    OutputFormat outputFormat = OutputFormat.getOutputFormat(formatRequest);
    switch (outputFormat) {
      case CSV:
        return new CSVOutputFormFactory();
      case TXT:
        return new TextOutputFormFactory();
      default:
        return new TextOutputFormFactory();
    }
  }

  private void createOutput(List<OutputForm> views) {
    if ((outputFileName == null) || (outputFileName.trim().equals(""))) {
      writeOutputToStdOut(views);
    }
    else {
      writeOutputToFile(views);
    }
  }

  public class ViewListConverter implements IStringConverter<List<String>> {
    @Override
    public List<String> convert(String viewsString) {
      String[] viewArray = viewsString.split(",");
      List<String> viewList = new ArrayList<>();

      for (String currentView : viewArray) {
        viewList.add(currentView);
      }

      return viewList;
    }
  }
}
