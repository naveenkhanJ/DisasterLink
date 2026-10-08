package lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.ReportReceipt;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.ClientMetaRequest;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.GeoPointRequest;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.GeoPointResponse;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.HazardReportResponse;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.ReportDraftRequest;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.ReportReceiptResponse;
import lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto.SyncResultResponse;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.PhotoUpload;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import lk.sliit.se3070.group058.disasterlink.shared.web.WireTime;
import org.springframework.web.multipart.MultipartFile;

/**
 * Maps between the UC2 wire records and the dictionary's types. Times on the wire carry their
 * offset (model dictionary section 0.3); stored and design times are {@code LocalDateTime} in
 * Asia/Colombo, converted through {@link WireTime}.
 *
 * <p>Implementation-only class (standards section 4, "No entities on the wire"). Implements CH-26.
 */
public final class ReportMapper {

  /** The file name given to a photo part that arrives without one. */
  static final String DEFAULT_FILE_NAME = "photo";

  /** The content type given to a photo part that arrives without one. */
  static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  private ReportMapper() {
    throw new AssertionError("ReportMapper is a utility class");
  }

  /**
   * Maps the wire form of the draft, converting {@code capturedAt} to Asia/Colombo.
   *
   * @param request the request record
   * @return the draft; the components the citizen enters stay {@code null} when missing
   */
  public static ReportDraft toDraft(ReportDraftRequest request) {
    return new ReportDraft(
        request.draftId(),
        request.hazardType(),
        request.description(),
        toGeoPoint(request.location()),
        request.locationFromGps(),
        request.district(),
        WireTime.fromWire(request.capturedAt()));
  }

  /**
   * Maps the wire form of the client meta, converting {@code capturedAt} to Asia/Colombo.
   *
   * @param request the request record
   * @return the client meta
   */
  public static ClientMeta toMeta(ClientMetaRequest request) {
    return new ClientMeta(
        request.clientId(),
        request.userId(),
        request.submissionId(),
        WireTime.fromWire(request.capturedAt()),
        request.pendingCount());
  }

  /**
   * Maps the photo part of an upload. A part without a file name is called {@code photo}, one
   * without a content type {@code application/octet-stream}; an empty part gives empty content, so
   * that {@code validateReport} can refuse it.
   *
   * @param file the part, not {@code null}
   * @return the photo
   * @throws UncheckedIOException if the part cannot be read
   */
  public static PhotoUpload toPhotoUpload(MultipartFile file) {
    String originalName = file.getOriginalFilename();
    String fileName =
        originalName == null || originalName.isBlank() ? DEFAULT_FILE_NAME : originalName;
    String contentType =
        file.getContentType() == null ? DEFAULT_CONTENT_TYPE : file.getContentType();
    try {
      return new PhotoUpload(fileName, contentType, file.getBytes());
    } catch (IOException e) {
      throw new UncheckedIOException("could not read the photo part of the upload", e);
    }
  }

  /**
   * Maps a receipt to its wire record.
   *
   * @param receipt the control's reply
   * @return the record; its report is {@code null} when the receipt has none
   */
  public static ReportReceiptResponse toReceiptResponse(ReportReceipt receipt) {
    SyncResult result = receipt.result();
    return new ReportReceiptResponse(
        new SyncResultResponse(
            result.submissionId(), result.outcome(), result.reason(), result.message()),
        receipt.report() == null ? null : toResponse(receipt.report()));
  }

  /**
   * Maps a report to its wire record. The lazy hazard link is read here, so the caller must keep a
   * transaction open.
   *
   * @param report the report
   * @return the record
   */
  public static HazardReportResponse toResponse(HazardReport report) {
    return new HazardReportResponse(
        report.getReportId(),
        report.getHazardType(),
        report.getDescription(),
        toGeoPointResponse(report.getLocation()),
        report.isLocationFromGps(),
        report.getDistrict(),
        WireTime.toWire(report.getCapturedAt()),
        WireTime.toWire(report.getReportTime()),
        report.getStatus(),
        report.getVerificationTime().map(WireTime::toWire).orElse(null),
        report.getRejectionReason().orElse(null),
        report.getHazard().map(Hazard::getHazardId).orElse(null));
  }

  /**
   * Maps the reports to their wire records.
   *
   * @param reports the reports
   * @return the records, in order
   */
  public static List<HazardReportResponse> toResponses(List<HazardReport> reports) {
    return reports.stream().map(ReportMapper::toResponse).toList();
  }

  private static GeoPoint toGeoPoint(GeoPointRequest request) {
    return request == null
        ? null
        : new GeoPoint(request.latitude(), request.longitude(), request.accuracyMetres());
  }

  private static GeoPointResponse toGeoPointResponse(GeoPoint point) {
    return new GeoPointResponse(point.latitude(), point.longitude(), point.accuracyMetres());
  }
}
