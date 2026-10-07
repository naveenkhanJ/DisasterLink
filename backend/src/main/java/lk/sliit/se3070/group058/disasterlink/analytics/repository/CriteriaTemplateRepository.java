package lk.sliit.se3070.group058.disasterlink.analytics.repository;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for criteria templates (model dictionary section 6, CriteriaTemplateRepository). */
public interface CriteriaTemplateRepository extends JpaRepository<CriteriaTemplate, String> {

  /**
   * Finds the templates an officer owns, by name (MD section 6, {@code findByOwner}; A27).
   *
   * @param officerId the identifier of the officer
   * @return the officer's templates
   */
  @Query("select t from CriteriaTemplate t where t.owner.userId = :officerId order by t.name")
  List<CriteriaTemplate> findByOwner(@Param("officerId") String officerId);
}
