package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.Objects;

/**
 * A shelter's details together with the client metadata of the write.
 *
 * <p>Implements CH-05 and CH-28.
 *
 * @param details the shelter details
 * @param meta the client metadata of the write
 */
public record ShelterSubmission(ShelterDetails details, ClientMeta meta) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ShelterSubmission(ShelterDetails details, ClientMeta meta) {
    this.details = Objects.requireNonNull(details, "details");
    this.meta = Objects.requireNonNull(meta, "meta");
  }
}
