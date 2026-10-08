package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

import jakarta.validation.constraints.NotNull;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;

/**
 * The body of {@code PUT /api/citizens/{citizenId}/language}. A missing or unknown language is a
 * malformed request, a 400.
 *
 * <p>Implementation-only wire record.
 *
 * @param language the citizen's new preferred language
 */
public record LanguageRequest(@NotNull Language language) {}
