package com.gkaraffa.amati.control;

import java.util.HashMap;

public enum OutputFormat {
  TXT(new String[] {"TEXT", "TXT"}),
  CSV(new String[] {"CSV"}),
  XLS(new String[] {"XLS"}),
  XLSX(new String[] {"XLSX"});

  private final String[] formatTexts;
  private final static HashMap<String, OutputFormat> hashMap = new HashMap<>();

  static {
    for (OutputFormat outputFormat : OutputFormat.values()) {
      String[] currentTexts = outputFormat.getFormatTexts();
      for (String currentText : currentTexts) {
        hashMap.put(currentText.trim().toUpperCase(), outputFormat);
      }
    }
  }

  OutputFormat(String[] texts) {
    this.formatTexts = texts;
  }

  public String[] getFormatTexts() {
    return this.formatTexts;
  }

  public static final OutputFormat getOutputFormat(String inputText)
      throws IllegalArgumentException {
    String analysisText = inputText.trim().toUpperCase();

    OutputFormat outputFormat = hashMap.get(analysisText);
    if (outputFormat == null) {
      throw new IllegalArgumentException("Unknown output type");
    }

    return outputFormat;
  }
}
