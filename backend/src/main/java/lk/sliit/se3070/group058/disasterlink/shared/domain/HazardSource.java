package lk.sliit.se3070.group058.disasterlink.shared.domain;

/**
 * Where a recorded hazard comes from; every hazard has exactly one source.
 *
 * <p>Implements CH-08 and CH-25.
 */
public enum HazardSource {
  AGENCY_BULLETIN,
  VERIFIED_REPORT
}
