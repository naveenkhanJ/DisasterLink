package lk.sliit.se3070.group058.disasterlink.shared.web;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * The body of the {@code dev} toggle that makes a mock gateway fail for some recipients.
 *
 * <p>Implementation-only wire record (standards section 11).
 *
 * @param prefixes the recipient-ID prefixes to fail, for example {@code ["CIT-COL-"]}
 */
public record FailurePrefixesRequest(@NotNull List<String> prefixes) {}
