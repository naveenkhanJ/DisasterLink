package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that every value-type record accepts a valid instance, rejects a null for each required
 * component and accepts a null for each component the dictionary marks {@code [0..1]} or that is an
 * input value record's field (MD section 0.13). The structural fields stay required.
 */
class RequiredComponentsTest {

  /** The {@code [0..1]} components of the records, by record and component name. */
  private static final Map<Class<?>, Set<String>> NULLABLE =
      Map.ofEntries(
          Map.entry(GeoPoint.class, Set.of("accuracyMetres")),
          Map.entry(ReportDecisionNotice.class, Set.of("reason")),
          Map.entry(
              ShelterDetails.class,
              Set.of("name", "address", "district", "contactNumber", "operatorId")),
          Map.entry(SyncResult.class, Set.of("reason", "message")),
          Map.entry(
              ReportCriteria.class,
              Set.of("districts", "hazardType", "periodStart", "periodEnd", "metrics", "hazardId")),
          // Input value records (MD section 0.13, P155 X1): the control validates these fields.
          Map.entry(LocalisedText.class, Set.of("sinhala", "tamil", "english")),
          Map.entry(
              WarningDetails.class,
              Set.of(
                  "message",
                  "instructions",
                  "targetDistricts",
                  "targetRiverBasins",
                  "channels",
                  "expiryTime")),
          Map.entry(HazardUpdate.class, Set.of("description", "severity", "districts")),
          Map.entry(
              EscalationDetails.class, Set.of("newLevel", "message", "instructions", "expiryTime")),
          Map.entry(
              NewHazardDetails.class,
              Set.of("severity", "districts", "locationName", "description")),
          Map.entry(ReportDraft.class, Set.of("hazardType", "description", "location", "district")),
          Map.entry(CriteriaCheck.class, Set.of("adjusted")),
          Map.entry(DistributionLine.class, Set.of("ownerId")));

  static Stream<Arguments> records() {
    return Stream.of(
            GeoPoint.class,
            LocalisedText.class,
            WarningDetails.class,
            WarningPreview.class,
            BroadcastSummary.class,
            HazardUpdate.class,
            EscalationDetails.class,
            NewHazardDetails.class,
            ClientMeta.class,
            ReportDraft.class,
            PhotoUpload.class,
            ReportDecisionNotice.class,
            OccupancyUpdate.class,
            ShelterDetails.class,
            ShelterSubmission.class,
            SyncResult.class,
            ReportCriteria.class,
            CriteriaCheck.class,
            ReportResults.class,
            AlertTimelineEntry.class,
            OccupancyPoint.class,
            DistributionLine.class,
            ReportHeader.class,
            ScopedReport.class,
            ExportFile.class,
            ValidationResult.class)
        .map(type -> Arguments.of(type.getSimpleName(), type));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("records")
  @DisplayName("A record rejects a null in a required component and accepts one in a [0..1] one")
  void nullHandlingFollowsTheDictionary(String name, Class<?> type) throws Exception {
    RecordComponent[] components = type.getRecordComponents();
    Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes(components));
    Set<String> nullable = NULLABLE.getOrDefault(type, Set.of());

    assertThat(constructor.newInstance(validArguments(components))).isNotNull();
    for (int index = 0; index < components.length; index++) {
      RecordComponent component = components[index];
      if (component.getType().isPrimitive()) {
        continue;
      }
      Object[] arguments = validArguments(components);
      arguments[index] = null;
      if (nullable.contains(component.getName())) {
        assertThat(constructor.newInstance(arguments)).isNotNull();
      } else {
        assertThat(constructorFailure(constructor, arguments))
            .as("%s.%s", name, component.getName())
            .isInstanceOf(NullPointerException.class);
      }
    }
  }

  private static Throwable constructorFailure(Constructor<?> constructor, Object[] arguments)
      throws ReflectiveOperationException {
    try {
      constructor.newInstance(arguments);
    } catch (InvocationTargetException exception) {
      return exception.getCause();
    }
    return new AssertionError("no exception");
  }

  private static Class<?>[] parameterTypes(RecordComponent[] components) {
    return java.util.Arrays.stream(components)
        .map(RecordComponent::getType)
        .toArray(Class<?>[]::new);
  }

  private static Object[] validArguments(RecordComponent[] components)
      throws ReflectiveOperationException {
    Object[] arguments = new Object[components.length];
    for (int index = 0; index < components.length; index++) {
      arguments[index] = sampleValue(components[index].getGenericType());
    }
    return arguments;
  }

  /** Builds a valid, non-null sample value for a type; records are built from their components. */
  private static Object sampleValue(Type type) throws ReflectiveOperationException {
    if (type instanceof ParameterizedType parameterized) {
      Class<?> raw = (Class<?>) parameterized.getRawType();
      if (raw == List.class) {
        return List.of();
      }
      if (raw == Set.class) {
        return Set.of();
      }
      return Map.of();
    }
    Class<?> cls = (Class<?>) type;
    if (cls == int.class) {
      return 0;
    }
    if (cls == double.class) {
      return 0.0;
    }
    if (cls == boolean.class) {
      return false;
    }
    if (cls == String.class) {
      return "x";
    }
    if (cls == Double.class) {
      return 1.0;
    }
    if (cls == byte[].class) {
      return new byte[] {1};
    }
    if (cls == LocalDateTime.class) {
      return LocalDateTime.of(2026, 5, 21, 8, 15);
    }
    if (cls == LocalDate.class) {
      return LocalDate.of(2026, 5, 21);
    }
    if (cls.isEnum()) {
      return cls.getEnumConstants()[0];
    }
    RecordComponent[] components = cls.getRecordComponents();
    Constructor<?> constructor = cls.getDeclaredConstructor(parameterTypes(components));
    return constructor.newInstance(validArguments(components));
  }
}
