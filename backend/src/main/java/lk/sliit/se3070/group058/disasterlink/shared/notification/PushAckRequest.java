package lk.sliit.se3070.group058.disasterlink.shared.notification;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * The body of the acknowledgement the app sends for the entries it has shown.
 *
 * <p>Implementation-only wire record (model dictionary section 0.3).
 *
 * @param ids the identifiers of the entries to mark as delivered
 */
public record PushAckRequest(@NotNull List<String> ids) {}
