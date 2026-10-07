package lk.sliit.se3070.group058.disasterlink.shared.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Makes the business identifiers: HZ-yyyy-nnnn (hazard), WRN-yyyy-nnnn (warning), RPT-yyyy-DDD-nnnn
 * (hazard report), DR-yyyy-nnnn (disaster report) and CIT-DDD-nnnn (citizen).
 *
 * <p>Numbering is global per kind: each kind has one database sequence, created in {@code
 * schema.sql}, so the district code in a report or citizen identifier is a label and not a separate
 * sequence. The year in the identifier comes from the injected clock. The class depends on no
 * feature package.
 *
 * <p>Implementation-only class of the model dictionary section 6 (PR14, R3G-07; P62 FX-12, R4G-11).
 */
@Component
public class IdGenerator {

  private static final int NUMBER_WIDTH = 4;

  private static final String SEPARATOR = "-";

  /** How an identifier of one kind is built. */
  private record Format(String prefix, String sequence, boolean withYear, boolean withDistrict) {}

  private static final Map<IdKind, Format> FORMATS = formats();

  private static final Map<District, String> DISTRICT_CODES = districtCodes();

  private final JdbcTemplate jdbcTemplate;

  private final Clock clock;

  /**
   * Creates the generator.
   *
   * @param jdbcTemplate used to draw the next number of a sequence
   * @param clock supplies the year of the identifier
   */
  public IdGenerator(JdbcTemplate jdbcTemplate, Clock clock) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Makes the next identifier of a kind.
   *
   * @param kind the kind of identifier
   * @param district the district for a hazard report or a citizen, or {@code null} for the other
   *     kinds, where it is ignored
   * @return the new identifier, for example {@code HZ-2026-0300}
   * @throws NullPointerException if {@code kind} is {@code null}
   * @throws IllegalArgumentException if the kind needs a district and {@code district} is {@code
   *     null}
   */
  public String next(IdKind kind, District district) {
    Objects.requireNonNull(kind, "kind");
    Format format = FORMATS.get(kind);
    if (format.withDistrict() && district == null) {
      throw new IllegalArgumentException("a district is required for " + kind);
    }
    List<String> parts = new ArrayList<>();
    parts.add(format.prefix());
    if (format.withYear()) {
      parts.add(String.valueOf(LocalDate.now(clock).getYear()));
    }
    if (format.withDistrict()) {
      parts.add(DISTRICT_CODES.get(district));
    }
    parts.add(paddedNumber(format.sequence()));
    return String.join(SEPARATOR, parts);
  }

  private String paddedNumber(String sequence) {
    Long number = jdbcTemplate.queryForObject("SELECT NEXT VALUE FOR " + sequence, Long.class);
    return String.format("%0" + NUMBER_WIDTH + "d", Objects.requireNonNull(number));
  }

  private static Map<IdKind, Format> formats() {
    Map<IdKind, Format> formats = new EnumMap<>(IdKind.class);
    formats.put(IdKind.HAZARD, new Format("HZ", "hazard_id_seq", true, false));
    formats.put(IdKind.WARNING, new Format("WRN", "warning_id_seq", true, false));
    formats.put(IdKind.HAZARD_REPORT, new Format("RPT", "hazard_report_id_seq", true, true));
    formats.put(IdKind.DISASTER_REPORT, new Format("DR", "disaster_report_id_seq", true, false));
    formats.put(IdKind.CITIZEN, new Format("CIT", "citizen_id_seq", false, true));
    return formats;
  }

  /**
   * The three-letter district codes; the codes other than the four the standards name are a
   * Group_058 assumption.
   */
  private static Map<District, String> districtCodes() {
    Map<District, String> codes = new EnumMap<>(District.class);
    codes.put(District.AMPARA, "AMP");
    codes.put(District.ANURADHAPURA, "ANU");
    codes.put(District.BADULLA, "BAD");
    codes.put(District.BATTICALOA, "BAT");
    codes.put(District.COLOMBO, "COL");
    codes.put(District.GALLE, "GAL");
    codes.put(District.GAMPAHA, "GAM");
    codes.put(District.HAMBANTOTA, "HAM");
    codes.put(District.JAFFNA, "JAF");
    codes.put(District.KALUTARA, "KLT");
    codes.put(District.KANDY, "KAN");
    codes.put(District.KEGALLE, "KEG");
    codes.put(District.KILINOCHCHI, "KIL");
    codes.put(District.KURUNEGALA, "KUR");
    codes.put(District.MANNAR, "MAN");
    codes.put(District.MATALE, "MTL");
    codes.put(District.MATARA, "MTR");
    codes.put(District.MONARAGALA, "MON");
    codes.put(District.MULLAITIVU, "MUL");
    codes.put(District.NUWARA_ELIYA, "NUW");
    codes.put(District.POLONNARUWA, "POL");
    codes.put(District.PUTTALAM, "PUT");
    codes.put(District.RATNAPURA, "RAT");
    codes.put(District.TRINCOMALEE, "TRI");
    codes.put(District.VAVUNIYA, "VAV");
    return codes;
  }
}
