-- Empties every table Hibernate creates, for the rollback tests that switch off the test's own
-- transaction (implementation standards section 4): whatever such a test writes commits, so this
-- script runs after each of its methods. The ID sequences are not reset.
-- When an entity gets a new table, add it here; CleanupScriptTest fails if a table is missing.
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE app_user;
TRUNCATE TABLE audit_record;
TRUNCATE TABLE citizen;
TRUNCATE TABLE client_sync_status;
TRUNCATE TABLE community_volunteer;
TRUNCATE TABLE criteria_template;
TRUNCATE TABLE disaster_report;
TRUNCATE TABLE disaster_report_hazard;
TRUNCATE TABLE disaster_report_provisional;
TRUNCATE TABLE disaster_report_shared_with;
TRUNCATE TABLE district_officer;
TRUNCATE TABLE dmc_duty_officer;
TRUNCATE TABLE dmc_officer;
TRUNCATE TABLE dmc_officer_reporting_district;
TRUNCATE TABLE hazard;
TRUNCATE TABLE hazard_district;
TRUNCATE TABLE hazard_report;
TRUNCATE TABLE notification;
TRUNCATE TABLE occupancy_record;
TRUNCATE TABLE organization;
TRUNCATE TABLE organization_district;
TRUNCATE TABLE processed_submission;
TRUNCATE TABLE push_outbox;
TRUNCATE TABLE relief_supply;
TRUNCATE TABLE resource_distribution;
TRUNCATE TABLE shelter;
TRUNCATE TABLE shelter_warning;
TRUNCATE TABLE warning;
TRUNCATE TABLE warning_channel;
TRUNCATE TABLE warning_target_district;
TRUNCATE TABLE warning_target_river_basin;
SET REFERENTIAL_INTEGRITY TRUE;
