package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

/**
 * The server's reply to a report upload on the wire. A REJECTED report is an HTTP 200 reply with
 * this body (standards section 12.1, item 2).
 *
 * <p>Implementation-only wire record.
 *
 * @param result the outcome of the upload
 * @param report the stored report, or {@code null} if none was stored (a rejected upload) or the
 *     earlier report of a duplicate is gone
 */
public record ReportReceiptResponse(SyncResultResponse result, HazardReportResponse report) {}
