package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * The image of a hazard report, sent with the report. The server stores the file and keeps its name
 * in the report.
 *
 * <p>Source: FD-10 and FE-03 (model dictionary section 2). The content is copied in and out, and
 * two uploads are equal when their content is equal.
 *
 * @param fileName the name of the file
 * @param contentType the media type of the file, for example {@code image/jpeg}
 * @param content the bytes of the file
 */
public record PhotoUpload(String fileName, String contentType, byte[] content) {

  /**
   * Creates the upload, checking the components and copying the content.
   *
   * @throws NullPointerException if a component is {@code null}
   */
  public PhotoUpload(String fileName, String contentType, byte[] content) {
    this.fileName = Objects.requireNonNull(fileName, "fileName");
    this.contentType = Objects.requireNonNull(contentType, "contentType");
    this.content = Objects.requireNonNull(content, "content").clone();
  }

  /**
   * Returns a copy of the content.
   *
   * @return a copy of the bytes of the file
   */
  @Override
  public byte[] content() {
    return content.clone();
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof PhotoUpload that
            && fileName.equals(that.fileName)
            && contentType.equals(that.contentType)
            && Arrays.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, contentType, Arrays.hashCode(content));
  }

  @Override
  public String toString() {
    return "PhotoUpload[fileName=%s, contentType=%s, contentLength=%d]"
        .formatted(fileName, contentType, content.length);
  }
}
