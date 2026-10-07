package lk.sliit.se3070.group058.disasterlink.analytics.domain;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteriaConverter;

/**
 * A saved set of report criteria that an officer can start from.
 *
 * <p>Implements CH-31 (A27). Seed data: no flow saves a template.
 */
@Entity
public class CriteriaTemplate {

  @Id private String templateId;

  private String name;

  @Convert(converter = ReportCriteriaConverter.class)
  @Lob
  private ReportCriteria criteria;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private DMCOfficer owner;

  /**
   * Creates a template; only the seed does so.
   *
   * @throws NullPointerException if an argument is {@code null}
   */
  public CriteriaTemplate(
      String templateId, String name, ReportCriteria criteria, DMCOfficer owner) {
    this.templateId = Objects.requireNonNull(templateId, "templateId");
    this.name = Objects.requireNonNull(name, "name");
    this.criteria = Objects.requireNonNull(criteria, "criteria");
    this.owner = Objects.requireNonNull(owner, "owner");
  }

  /** Constructor for JPA only. */
  protected CriteriaTemplate() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the template.
   *
   * @return the identifier of the template
   */
  public String getTemplateId() {
    return templateId;
  }

  /**
   * Returns the name of the template.
   *
   * @return the name of the template
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the criteria the template holds.
   *
   * @return the criteria the template holds
   */
  public ReportCriteria getCriteria() {
    return criteria;
  }

  /**
   * Returns the officer who owns the template (A27).
   *
   * @return the officer who owns the template (A27)
   */
  public DMCOfficer getOwner() {
    return owner;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof CriteriaTemplate that
            && Objects.equals(getTemplateId(), that.getTemplateId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getTemplateId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getTemplateId() + "]";
  }
}
