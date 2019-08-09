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
import com.gkaraffa.guarneri.analysis.IntervalAnalytic;
import com.gkaraffa.guarneri.analysis.RomanNumeralAnalytic;
import com.gkaraffa.guarneri.analysis.ScalarAnalytic;
import com.gkaraffa.guarneri.analysis.TabularAnalytic;
import com.gkaraffa.guarneri.view.CSVViewFactory;
import com.gkaraffa.guarneri.view.TextViewFactory;
import com.gkaraffa.guarneri.view.View;
import com.gkaraffa.guarneri.view.ViewFactory;

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
    List<TabularAnalytic> analyticsRendered = this.parseAndRenderAnalytics(scaleRendered);
    ViewFactory viewFactory = this.selectCreateViewFactory(formatRequest);
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

  private List<View> renderViews(List<TabularAnalytic> analyticsRendered, ViewFactory viewFactory) {
    List<View> views = new ArrayList<View>();

    for (TabularAnalytic tabularAnalytic : analyticsRendered) {
      views.add(viewFactory.renderView(tabularAnalytic));
    }

    return views;
  }

  private Scale parseAndRenderScale() {
    Helper helper = Helper.getInstance();
    Scale scaleRendered = helper.getScale(keyRequest, scaleRequest);

    return scaleRendered;
  }

  private List<TabularAnalytic> parseAndRenderAnalytics(Scale scaleRendered) {
    List<TabularAnalytic> analyticsRendered = new ArrayList<TabularAnalytic>();

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

  private TabularAnalytic getRomanNumeralAnalytic(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      return RomanNumeralAnalytic.createRomanNumeralAnalytic((DiatonicScale) scale);
    }
    else {
      throw new IllegalArgumentException("RomanNumeralAnalytic cannot be rendered for this scale");
    }
  }

  private TabularAnalytic getIntervalAnalytic(Scale scale) throws IllegalArgumentException {
    if (scale instanceof DiatonicScale) {
      return IntervalAnalytic.createIntervalAnalytic(scale);
    }
    else {
      throw new IllegalArgumentException("IntervalAnalytic view cannot be rendered for this scale");
    }
  }
  
  private TabularAnalytic getScalarAnalytic(Scale scale) throws IllegalArgumentException {
    return ScalarAnalytic.createScalarAnalytic(scale);
  }

  private ViewFactory selectCreateViewFactory(String formatRequest) {
    OutputFormat outputFormat = OutputFormat.getOutputFormat(formatRequest);
    switch(outputFormat) {
      case CSV:
        return new CSVViewFactory();
      case TXT:
        return new TextViewFactory();
      default:
        return new TextViewFactory();
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
