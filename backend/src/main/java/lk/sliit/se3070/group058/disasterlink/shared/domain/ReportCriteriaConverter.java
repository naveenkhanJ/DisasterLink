package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores {@link ReportCriteria} as JSON in one text column.
 *
 * <p>Implementation-only class of the model dictionary section 0.7: {@code ReportCriteria} and
 * {@code ReportResults} are stored as JSON through an attribute converter. It is applied explicitly
 * to the fields that need it, not automatically.
 */
@Converter
public class ReportCriteriaConverter implements AttributeConverter<ReportCriteria, String> {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public String convertToDatabaseColumn(ReportCriteria criteria) {
    if (criteria == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(criteria);
    } catch (JacksonException exception) {
      throw new IllegalStateException("cannot write the report criteria as JSON", exception);
    }
  }

  @Override
  public ReportCriteria convertToEntityAttribute(String json) {
    if (json == null) {
      return null;
    }
    try {
      return MAPPER.readValue(json, ReportCriteria.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException("cannot read the report criteria from JSON", exception);
    }
  }
}
