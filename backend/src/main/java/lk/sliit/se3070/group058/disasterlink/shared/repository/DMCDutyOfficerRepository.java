package lk.sliit.se3070.group058.disasterlink.shared.repository;

import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for DMC duty officers; typed {@code findById} without downcasts (MD section 6,
 * restored by FQ1).
 */
public interface DMCDutyOfficerRepository extends JpaRepository<DMCDutyOfficer, String> {}
