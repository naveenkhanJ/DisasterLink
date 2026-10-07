-- One sequence per kind of business identifier (IdGenerator; implementation standards section 11).
-- Numbering is global per kind. Each sequence starts above the highest number the seed data uses
-- for that kind, so a new identifier never collides with a seeded one: the seed (stage S4) must
-- stay below these start values. No Flyway; this script runs after Hibernate has created the tables
-- (spring.jpa.defer-datasource-initialization=true).
-- disaster_report_id_seq starts at 184 because no disaster report is seeded and the report that UC4 generates in
-- the demo must be DR-2026-0184, the identifier the SC4 scenario and the wireframes show.
-- Highest seeded: HZ-2026-0203 (LiveStorySeed).
CREATE SEQUENCE IF NOT EXISTS hazard_id_seq START WITH 300;
-- Highest seeded: WRN-2026-0101 (LiveStorySeed).
CREATE SEQUENCE IF NOT EXISTS warning_id_seq START WITH 200;
-- Highest seeded: RPT-2026-COL-0905 (LiveStorySeed).
CREATE SEQUENCE IF NOT EXISTS hazard_report_id_seq START WITH 1000;
-- Highest seeded: none (see the comment above).
CREATE SEQUENCE IF NOT EXISTS disaster_report_id_seq START WITH 184;
-- Highest seeded: CIT-xxx-0060 in each of RAT, KLT, COL and GAM (PeopleSeed).
CREATE SEQUENCE IF NOT EXISTS citizen_id_seq START WITH 1000;
