package lk.sliit.se3070.group058.disasterlink.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where the report photos are stored on disk. Afham's photo endpoint, {@code receiveReport} (which
 * stores each submitted photo) and the seed (which copies a placeholder under the file name of
 * every seeded report) use the same directory, so a report's photo is always found at {@code
 * <storageDir>/<HazardReport.photo>} (P155 G9).
 *
 * @param storageDir the directory, relative to the working directory or absolute; created when the
 *     first photo is stored
 */
@Validated
@ConfigurationProperties("photo")
public record PhotoStorageProperties(
    @NotBlank(message = "photo.storage-dir must name the photo directory") String storageDir) {}
