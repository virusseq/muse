package org.cancogenvirusseq.muse.exceptions;

import java.util.Map;

public interface MuseBaseException {
  Map<String, Object> getErrorObject();
}
