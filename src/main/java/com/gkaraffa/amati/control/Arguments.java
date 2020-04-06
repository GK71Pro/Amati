package com.gkaraffa.amati.control;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

public class Arguments {
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

  public String getKeyRequest() {
    return keyRequest;
  }

  public String getScaleRequest() {
    return scaleRequest;
  }

  public String getFormatRequest() {
    return formatRequest;
  }

  public List<String> getViewRequests() {
    return viewRequests;
  }

  public String getOutputFileName() {
    return outputFileName;
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
