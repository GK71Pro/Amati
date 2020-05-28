package com.gkaraffa.amati.control;

import com.beust.jcommander.Parameter;

public class Arguments {
  @Parameter(names = {"--type", "-t"})
  private String requestType;
  
  @Parameter(names = {"--key", "-k"})
  private String keyRequest;

  @Parameter(names = {"--scale", "-s"})
  private String scaleRequest;

  @Parameter(names = {"--format", "-f"})
  private String formatRequest = "text";

  @Parameter(names = {"--output", "-o"})
  private String outputFileName;

  public String getRequestType() {
    return requestType;
  }
  
  public String getKeyRequest() {
    return keyRequest;
  }

  public String getScaleRequest() {
    return scaleRequest;
  }

  public String getFormatRequest() {
    return formatRequest;
  }

  public String getOutputFileName() {
    return outputFileName;
  }

}
