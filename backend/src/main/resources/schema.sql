-- One sequence per kind of business identifier (IdGenerator; implementation standards section 11).
-- Numbering is global per kind. Each sequence starts above the highest number the seed data uses
-- for that kind, so a new identifier never collides with a seeded one: the seed (stage S4) must
-- stay below these start values. No Flyway; this script runs after Hibernate has created the tables
-- (spring.jpa.defer-datasource-initialization=true).
CREATE SEQUENCE IF NOT EXISTS hazard_id_seq START WITH 300;
CREATE SEQUENCE IF NOT EXISTS warning_id_seq START WITH 200;
CREATE SEQUENCE IF NOT EXISTS hazard_report_id_seq START WITH 1000;
CREATE SEQUENCE IF NOT EXISTS disaster_report_id_seq START WITH 200;
CREATE SEQUENCE IF NOT EXISTS citizen_id_seq START WITH 1000;
