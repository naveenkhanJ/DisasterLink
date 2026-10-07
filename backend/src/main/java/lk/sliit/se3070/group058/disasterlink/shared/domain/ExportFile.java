package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * An exported report: the file name, its format and its bytes.
 *
 * <p>Implements CH-32. The content is copied in and out, and two files are equal when their content
 * is equal.
 *
 * @param fileName the name of the file
 * @param format the format of the file
 * @param content the bytes of the file
 */
public record ExportFile(String fileName, ExportFormat format, byte[] content) {

  /**
   * Creates the file, checking the components and copying the content.
   *
   * @throws NullPointerException if a component is {@code null}
   */
  public ExportFile(String fileName, ExportFormat format, byte[] content) {
    this.fileName = Objects.requireNonNull(fileName, "fileName");
    this.format = Objects.requireNonNull(format, "format");
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
        || other instanceof ExportFile that
            && fileName.equals(that.fileName)
            && format == that.format
            && Arrays.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, format, Arrays.hashCode(content));
  }

  @Override
  public String toString() {
    return "ExportFile[fileName=%s, format=%s, contentLength=%d]"
        .formatted(fileName, format, content.length);
  }
}
