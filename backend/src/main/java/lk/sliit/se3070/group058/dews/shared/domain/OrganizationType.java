package lk.sliit.se3070.group058.dews.shared.domain;

/**
 * The kind of an organisation; the police count as a government body.
 *
 * <p>Implements CH-15 and CH-07.
 */
public enum OrganizationType {
  GOVERNMENT_BODY,
  ARMED_FORCES,
  NGO,
  PRIVATE_DONOR,
  COMMUNITY_VOLUNTEER_GROUP
}
