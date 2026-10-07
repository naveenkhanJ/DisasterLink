package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import java.util.List;

/**
 * The reply of the online check of shelter details. An invalid result is a normal HTTP 200 reply.
 *
 * <p>Implementation-only wire record.
 *
 * @param valid whether the details are valid
 * @param errors the errors, empty when valid
 */
public record ValidationResultResponse(boolean valid, List<String> errors) {}
