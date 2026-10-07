package lk.sliit.se3070.group058.disasterlink.shared.service;

/**
 * The kind of business identifier {@link IdGenerator} makes. An enum rather than a string type code
 * (L03 Primitive Obsession, Replace Type Code).
 *
 * <p>Implementation-only class of the model dictionary section 6 (P62 FX-12, R4G-11).
 */
public enum IdKind {
  HAZARD,
  WARNING,
  HAZARD_REPORT,
  DISASTER_REPORT,
  CITIZEN
}
