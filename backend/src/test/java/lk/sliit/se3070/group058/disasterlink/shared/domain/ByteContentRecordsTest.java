package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the two records that hold a byte array: {@link PhotoUpload} and {@link ExportFile}. */
class ByteContentRecordsTest {

  @Test
  @DisplayName("PhotoUpload copies its content in")
  void photoUploadCopiesContentIn() {
    byte[] bytes = {1, 2, 3};
    PhotoUpload photo = new PhotoUpload("a.jpg", "image/jpeg", bytes);

    bytes[0] = 9;

    assertThat(photo.content()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("PhotoUpload copies its content out")
  void photoUploadCopiesContentOut() {
    PhotoUpload photo = new PhotoUpload("a.jpg", "image/jpeg", new byte[] {1, 2, 3});

    photo.content()[0] = 9;

    assertThat(photo.content()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("PhotoUploads with equal content are equal")
  void photoUploadsCompareByContent() {
    PhotoUpload first = new PhotoUpload("a.jpg", "image/jpeg", new byte[] {1, 2});
    PhotoUpload same = new PhotoUpload("a.jpg", "image/jpeg", new byte[] {1, 2});

    assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isEqualTo(first);
    assertThat(first)
        .isNotEqualTo(new PhotoUpload("a.jpg", "image/jpeg", new byte[] {1, 3}))
        .isNotEqualTo(new PhotoUpload("b.jpg", "image/jpeg", new byte[] {1, 2}))
        .isNotEqualTo(new PhotoUpload("a.jpg", "image/png", new byte[] {1, 2}))
        .isNotEqualTo("not a photo");
  }

  @Test
  @DisplayName("PhotoUpload.toString does not print the bytes")
  void photoUploadToStringHidesBytes() {
    PhotoUpload photo = new PhotoUpload("a.jpg", "image/jpeg", new byte[] {42, 43});

    assertThat(photo)
        .hasToString("PhotoUpload[fileName=a.jpg, contentType=image/jpeg, contentLength=2]");
  }

  @Test
  @DisplayName("PhotoUpload requires every component")
  void photoUploadRequiresEveryComponent() {
    assertThatNullPointerException()
        .isThrownBy(() -> new PhotoUpload(null, "image/jpeg", new byte[0]));
    assertThatNullPointerException().isThrownBy(() -> new PhotoUpload("a.jpg", null, new byte[0]));
    assertThatNullPointerException().isThrownBy(() -> new PhotoUpload("a.jpg", "image/jpeg", null));
  }

  @Test
  @DisplayName("ExportFile copies its content in and out")
  void exportFileCopiesContent() {
    byte[] bytes = {1, 2, 3};
    ExportFile file = new ExportFile("r.csv", ExportFormat.CSV, bytes);

    bytes[0] = 9;
    file.content()[1] = 9;

    assertThat(file.content()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("ExportFiles with equal content are equal")
  void exportFilesCompareByContent() {
    ExportFile first = new ExportFile("r.csv", ExportFormat.CSV, new byte[] {1, 2});
    ExportFile same = new ExportFile("r.csv", ExportFormat.CSV, new byte[] {1, 2});

    assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isEqualTo(first);
    assertThat(first)
        .isNotEqualTo(new ExportFile("r.csv", ExportFormat.CSV, new byte[] {2, 2}))
        .isNotEqualTo(new ExportFile("s.csv", ExportFormat.CSV, new byte[] {1, 2}))
        .isNotEqualTo(new ExportFile("r.csv", ExportFormat.PDF, new byte[] {1, 2}))
        .isNotEqualTo("not a file");
  }

  @Test
  @DisplayName("ExportFile.toString does not print the bytes")
  void exportFileToStringHidesBytes() {
    ExportFile file = new ExportFile("r.csv", ExportFormat.CSV, new byte[] {42});

    assertThat(file).hasToString("ExportFile[fileName=r.csv, format=CSV, contentLength=1]");
  }

  @Test
  @DisplayName("ExportFile requires every component")
  void exportFileRequiresEveryComponent() {
    assertThatNullPointerException()
        .isThrownBy(() -> new ExportFile(null, ExportFormat.CSV, new byte[0]));
    assertThatNullPointerException().isThrownBy(() -> new ExportFile("r.csv", null, new byte[0]));
    assertThatNullPointerException()
        .isThrownBy(() -> new ExportFile("r.csv", ExportFormat.CSV, null));
  }
}
