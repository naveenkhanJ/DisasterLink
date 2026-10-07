package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores {@link ReportResults} as JSON in one text column.
 *
 * <p>Implementation-only class of the model dictionary section 0.7: {@code ReportCriteria} and
 * {@code ReportResults} are stored as JSON through an attribute converter. It is applied explicitly
 * to the fields that need it, not automatically.
 */
@Converter
public class ReportResultsConverter implements AttributeConverter<ReportResults, String> {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public String convertToDatabaseColumn(ReportResults results) {
    if (results == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(results);
    } catch (JacksonException exception) {
      throw new IllegalStateException("cannot write the report results as JSON", exception);
    }
  }

  @Override
  public ReportResults convertToEntityAttribute(String json) {
    if (json == null) {
      return null;
    }
    try {
      return MAPPER.readValue(json, ReportResults.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException("cannot read the report results from JSON", exception);
    }
  }
}
