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
import com.gkaraffa.guarneri.view.View;
import com.gkaraffa.guarneri.model.analytic.Analytic;
import com.gkaraffa.guarneri.model.analytic.IntervalAnalytic;
import com.gkaraffa.guarneri.model.analytic.RomanNumeralAnalytic;
import com.gkaraffa.guarneri.model.analytic.ScalarAnalytic;
import com.gkaraffa.guarneri.view.AnalyticViewFactory;
import com.gkaraffa.guarneri.view.CSVAnalyticViewFactory;
import com.gkaraffa.guarneri.view.TextAnalyticViewFactory;

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
    List<Analytic> analyticsRendered = this.parseAndRenderAnalytics(scaleRendered);
    AnalyticViewFactory viewFactory = this.selectCreateViewFactory(formatRequest);
    List<View> views = this.renderViews(analyticsRendered, viewFactory);

    this.createOutput(views);
  }

  private void writeOutputToStdOut(List<View> views) {
    for (View view : views) {
      System.out.println(view.toString());
    }
  }

  private void writeOutputToFile(List<View> views) {
    File file = new File(this.outputFileName.trim());
    try (FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream writer = new BufferedOutputStream(fileOutputStream)) {

      for (View view : views) {
        byte[] buffer = view.getByteArray();
        writer.write(buffer, 0, buffer.length);
      }
    }
    catch (IOException iOE) {
      iOE.printStackTrace();
    }
  }

  private List<View> renderViews(List<Analytic> analyticsRendered, AnalyticViewFactory viewFactory) {
    List<View> views = new ArrayList<View>();

    for (Analytic tabularAnalytic : analyticsRendered) {
      views.add(viewFactory.renderView(tabularAnalytic));
    }

    return views;
  }

  private Scale parseAndRenderScale() {
    Helper helper = Helper.getInstance();
    Scale scaleRendered = helper.getScale(keyRequest, scaleRequest);

    return scaleRendered;
  }

  private List<Analytic> parseAndRenderAnalytics(Scale scaleRendered) {
    List<Analytic> analyticsRendered = new ArrayList<Analytic>();

    for (String viewRequest : this.viewRequests) {
      try {
        switch (viewRequest.toUpperCase().trim()) {
          case "ROMAN":
            analyticsRendered.add(getRomanNumeralAnalytic(scaleRendered));
            break;
          case "INTERVAL":
            analyticsRendered.add(getIntervalAnalytic(scaleRendered));
            break;
          case "SCALAR":
            analyticsRendered.add(getScalarAnalytic(scaleRendered));
            break;
        }
      }
      catch (IllegalArgumentException iAE) {
        iAE.printStackTrace();
      }
    }

    return analyticsRendered;
  }

  private Analytic getRomanNumeralAnalytic(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      return RomanNumeralAnalytic.createRomanNumeralAnalytic((DiatonicScale) scale);
    }
    else {
      throw new IllegalArgumentException("RomanNumeralAnalytic cannot be rendered for this scale");
    }
  }

  private Analytic getIntervalAnalytic(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      return IntervalAnalytic.createIntervalAnalytic(scale);
    }
    else {
      throw new IllegalArgumentException("IntervalAnalytic view cannot be rendered for this scale");
    }
  }
  
  private Analytic getScalarAnalytic(Scale scale) throws IllegalArgumentException {
    return ScalarAnalytic.createScalarAnalytic(scale);
  }

  private AnalyticViewFactory selectCreateViewFactory(String formatRequest) {
    OutputFormat outputFormat = OutputFormat.getOutputFormat(formatRequest);
    switch(outputFormat) {
      case CSV:
        return new CSVAnalyticViewFactory();
      case TXT:
        return new TextAnalyticViewFactory();
      default:
        return new TextAnalyticViewFactory();
    }
  }

  private void createOutput(List<View> views) {
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
