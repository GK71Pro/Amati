package com.gkaraffa.amati.control;

import com.beust.jcommander.Parameter;

public class Arguments {
  @Parameter(names = {"--help", "-h"})
  private boolean helpRequest = false;

  @Parameter(names = {"--type", "-t"})
  private String typeRequest;

  @Parameter(names = {"--key", "-k"})
  private String keyRequest;

  @Parameter(names = {"--scale", "-s"})
  private String scaleRequest;

  @Parameter(names = {"--format", "-f"})
  private String formatRequest = "text";

  @Parameter(names = {"--output", "-o"})
  private String outputFileName;

  public boolean getHelpRequest() {
    return helpRequest;
  }

  public String getTypeRequest() {
    return typeRequest;
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
