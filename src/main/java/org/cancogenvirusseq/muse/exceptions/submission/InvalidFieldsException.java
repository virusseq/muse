package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.model.tsv_parser.InvalidField;

@Value
@EqualsAndHashCode(callSuper = true)
public class InvalidFieldsException extends Throwable implements MuseBaseException {
    List<InvalidField> invalidFields;

    @Override
    public String getMessage() {
        return "Found records with invalid fields";
    }

    @Override
    public Map<String, Object> getErrorInfo() {
        return Map.of("invalidFields", invalidFields);
    }
}
