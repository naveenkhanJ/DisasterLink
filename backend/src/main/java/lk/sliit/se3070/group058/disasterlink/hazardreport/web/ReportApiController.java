package lk.sliit.se3070.group058.disasterlink.hazardreport.web;

import jakarta.validation.Valid;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.hazardreport.service.ReportSubmissionController;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.ClientMetaRequest;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.HazardReportResponse;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.LanguageRequest;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.ReportDraftRequest;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.ReportReceiptResponse;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper.ReportMapper;
import lk.sliit.se3070.group058.disasterlink.shared.domain.PhotoUpload;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The endpoints of the citizen app for use case UC2 (standards sections 12.1 and 12.3): the report
 * upload, the status of one report, the citizen's reports and the language change. Each calls the
 * {@link ReportSubmissionController} and maps the entities it returns to wire records; entities
 * never reach the wire.
 *
 * <p>Every method that maps an entity runs in a transaction, because open-in-view is off and the
 * mapper reads the report's lazy hazard link (standards section 4, "Exceptions"; section 12.1, item
 * 10): the upload is {@code @Transactional} and the control's own transaction joins it, the reads
 * are read-only transactional, as in {@code WarningApiController}.
 *
 * <p>Errors are answered by the shared {@code ApiExceptionHandler} as problem details: a missing or
 * malformed part, a failed shape check, a missing or unknown language is a 400, an unknown report
 * or citizen a 404. A report that fails {@code validateReport} is not an error: it is a 200 reply
 * with the outcome REJECTED (SC2 E1, E2, E6, E8). A missing or empty photo part is passed on to the
 * control for that reason and is not a 400.
 *
 * <p>Implements CH-25 and CH-26.
 */
@RestController
public class ReportApiController {

  private final ReportSubmissionController reportSubmissionController;

  /**
   * Creates the controller.
   *
   * @param reportSubmissionController the UC2 control of the citizen side
   */
  public ReportApiController(ReportSubmissionController reportSubmissionController) {
    this.reportSubmissionController = reportSubmissionController;
  }

  /**
   * Receives a report with its photo: {@code POST /api/reports} as {@code multipart/form-data} with
   * the JSON parts {@code draft} and {@code meta} and the file part {@code photo}. The reply is
   * HTTP 200 with the receipt, whether ACCEPTED, DUPLICATE or REJECTED.
   *
   * @param draft the report as the app sent it
   * @param meta the client's metadata
   * @param photo the photo part, or {@code null} if the upload had none
   * @return the receipt
   */
  @PostMapping(path = "/api/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Transactional
  public ReportReceiptResponse receiveReport(
      @RequestPart("draft") @Valid ReportDraftRequest draft,
      @RequestPart("meta") @Valid ClientMetaRequest meta,
      @RequestPart(value = "photo", required = false) MultipartFile photo) {
    PhotoUpload upload = photo == null ? null : ReportMapper.toPhotoUpload(photo);
    return ReportMapper.toReceiptResponse(
        reportSubmissionController.receiveReport(
            ReportMapper.toDraft(draft), upload, ReportMapper.toMeta(meta)));
  }

  /**
   * Returns one report, for the app's status screen: {@code GET /api/reports/{reportId}}.
   *
   * @param reportId the identifier of the report
   * @return the report; an unknown identifier is a 404
   */
  @GetMapping("/api/reports/{reportId}")
  @Transactional(readOnly = true)
  public HazardReportResponse getReportStatus(@PathVariable String reportId) {
    return ReportMapper.toResponse(reportSubmissionController.getReportStatus(reportId));
  }

  /**
   * Lists a citizen's reports, newest first: {@code GET /api/citizens/{citizenId}/reports}.
   *
   * @param citizenId the identifier of the citizen
   * @return the citizen's reports; an unknown citizen is a 404
   */
  @GetMapping("/api/citizens/{citizenId}/reports")
  @Transactional(readOnly = true)
  public List<HazardReportResponse> getReportsByReporter(@PathVariable String citizenId) {
    return ReportMapper.toResponses(reportSubmissionController.getReportsByReporter(citizenId));
  }

  /**
   * Changes a citizen's preferred language: {@code PUT /api/citizens/{citizenId}/language} with
   * {@code {"language": "SINHALA"}}. The reply is 204.
   *
   * @param citizenId the identifier of the citizen; an unknown citizen is a 404
   * @param request the new language; a missing or unknown one is a 400
   */
  @PutMapping("/api/citizens/{citizenId}/language")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePreferredLanguage(
      @PathVariable String citizenId, @RequestBody @Valid LanguageRequest request) {
    reportSubmissionController.changePreferredLanguage(citizenId, request.language());
  }
}
