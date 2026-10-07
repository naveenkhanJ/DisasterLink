package lk.sliit.se3070.group058.disasterlink.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The switches of the demo data under the {@code demo} prefix that are not users (see {@link
 * DemoUserProperties}). Read by the seed runner (P155 B-12).
 *
 * @param pendingClient {@code true} seeds one Kalutara client with pending items, so that a report
 *     generated in the demo is provisional (SC4 A3; standards section 11); {@code false} (the
 *     default) seeds none
 */
@ConfigurationProperties("demo")
public record DemoSeedProperties(boolean pendingClient) {}
