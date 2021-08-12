package org.cancogenvirusseq.muse.utils;

import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {
  private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
  private static final Pattern DOUBLE_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");

  public static Boolean isDouble(String s) {
    return DOUBLE_PATTERN.matcher(s).matches();
  }

  public static Boolean isInteger(String s) {
    return INTEGER_PATTERN.matcher(s).matches();
  }
}
