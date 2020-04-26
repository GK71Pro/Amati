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
import com.gkaraffa.guarneri.view.analytic.scale.IntervalAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.scale.ReharmonizationOptionsAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.scale.RomanNumeralAnalyticViewFactory;
import com.gkaraffa.guarneri.view.analytic.scale.StepPatternAnalyticFactory;

public class MainController {
  private Arguments arguments = null;
  private RuntimeObjects runtimeObjects = null;
  

  public static void main(String[] args) {
    MainController mainController = new MainController();
    Arguments arguments = new Arguments();
    JCommander.newBuilder().addObject(arguments).build().parse(args);
    mainController.run(arguments);
  }

  public void run(Arguments arguments) {
    this.arguments = arguments;
    this.runtimeObjects = this.createRuntimeObjects(arguments);
    List<ViewTable> modelsRendered = this.parseAndRenderAnalytics(this.runtimeObjects);
    OutputFormFactory viewFactory = this.selectCreateOutputFormFactory(arguments.getFormatRequest());
    List<OutputForm> views = this.renderAnalytics(modelsRendered, viewFactory);

    this.createOutput(views);
  }
  
  private RuntimeObjects createRuntimeObjects(Arguments arguments) {
    Tone requestedKey = this.parseAndRenderKey(arguments.getKeyRequest());
    Scale requestedScale = this.parseAndRenderScale(arguments.getKeyRequest(), arguments.getScaleRequest());
    List<String> requestedViews = arguments.getViewRequests();
    
    RuntimeObjects runtimeObjects = new RuntimeObjects(requestedKey, requestedScale, requestedViews);
    
    return runtimeObjects;
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

  private List<OutputForm> renderAnalytics(List<ViewTable> modelsRendered,
      OutputFormFactory viewFactory) {
    List<OutputForm> analytics = new ArrayList<OutputForm>();

    for (ViewTable modelTable : modelsRendered) {
      analytics.add(viewFactory.renderView(modelTable));
    }

    return analytics;
  }

  private Tone parseAndRenderKey(String keyRequest) {
    return Tone.stringToTone(keyRequest);
  }
  

  private Scale parseAndRenderScale(String keyRequest, String scaleRequest) {
    ScaleHelper helper = ScaleHelper.getInstance();
    Scale scaleRendered = helper.getScale(keyRequest, scaleRequest);

    return scaleRendered;
  }

  private List<ViewTable> parseAndRenderAnalytics(RuntimeObjects runtimeObjects) {
    List<ViewTable> viewsRendered = new ArrayList<ViewTable>();
    ViewQueryBuilder vQB = new ViewQueryBuilder();

    vQB.insertCriteria("Scale", runtimeObjects.getRequestedScale());
    ViewQuery viewQuery = vQB.compileViewQuery();
    
    for (String viewRequest : runtimeObjects.requestedViews) {
      try {
        switch (viewRequest.toUpperCase().trim()) {
          case "ROMAN":
            viewsRendered.add(this.getRomanNumeralAnalytic(viewQuery));
            break;
          case "INTERVAL":
            viewsRendered.add(this.getIntervalAnalytic(viewQuery));
            break;
          case "STEP":
            viewsRendered.add(this.getStepPatternAnalytic(viewQuery));
            break;
          case "REHARM":
            viewsRendered.add(this.getReharmonizationOptionsAnalytic(viewQuery));
            break;
        }
      }
      catch (IllegalArgumentException iAE) {
        iAE.printStackTrace();
      }
    }

    return viewsRendered;
  }


  private ViewTable getRomanNumeralAnalytic(ViewQuery viewQuery) throws IllegalArgumentException {
    if ((Scale)viewQuery.getCriteria("Scale") instanceof DiatonicScale) {
      ViewFactory viewFactory = new RomanNumeralAnalyticViewFactory();
      return viewFactory.createView(viewQuery);
    }
    else {
      throw new IllegalArgumentException("RomanNumeralAnalytic cannot be rendered for this scale");
    }
  }

  private ViewTable getIntervalAnalytic(ViewQuery viewQuery) throws IllegalArgumentException {
    if ((Scale)viewQuery.getCriteria("Scale")  instanceof DiatonicScale) {
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

  private ViewTable getReharmonizationOptionsAnalytic(ViewQuery viewQuery) {
    ViewFactory viewFactory = new ReharmonizationOptionsAnalyticViewFactory();

    return viewFactory.createView(viewQuery);
  }

  private OutputFormFactory selectCreateOutputFormFactory(String formatRequest) {
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
      this.writeOutputToStdOut(views);
    }
    else {
      this.writeOutputToFile(views);
    }
  }
  
  class RuntimeObjects{
    private Tone requestedKey = null;
    private Scale requestedScale = null;
    private List<String> requestedViews = null;
    
    public RuntimeObjects(Tone requestedKey, Scale requestedScale, List<String> requestedViews) {
      this.requestedKey = requestedKey;
      this.requestedScale = requestedScale;
      this.requestedViews = requestedViews;
    }
    
    public Tone getRequestedKey() {
      return requestedKey;
    }
    
    public Scale getRequestedScale() {
      return requestedScale;
    }
    
    public List<String> getRequestedViews(){
      return requestedViews;
    }
  }
  
}
