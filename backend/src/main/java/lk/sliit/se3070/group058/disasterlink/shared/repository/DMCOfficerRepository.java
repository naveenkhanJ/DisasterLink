package lk.sliit.se3070.group058.disasterlink.shared.repository;

import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for DMC officers; typed {@code findById} without downcasts (MD section 6). */
public interface DMCOfficerRepository extends JpaRepository<DMCOfficer, String> {}
