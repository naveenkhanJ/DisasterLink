package lk.sliit.se3070.group058.disasterlink.hazardreport.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import lk.sliit.se3070.group058.disasterlink.shared.domain.PhotoUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

/** Tests of the parts of {@link ReportMapper} that the endpoint tests cannot reach. */
class ReportMapperTest {

  @Test
  @DisplayName("SC2 step 18: a photo part that cannot be read is an unchecked I/O error")
  void unreadablePhotoIsAnUncheckedIoError() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("flood.jpg");
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getBytes()).thenThrow(new IOException("disk gone"));

    assertThatThrownBy(() -> ReportMapper.toPhotoUpload(file))
        .isInstanceOf(UncheckedIOException.class)
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("SC2 step 18: a blank file name and a missing content type get the defaults")
  void defaultsForABlankNameAndMissingType() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("  ");
    when(file.getContentType()).thenReturn(null);
    when(file.getBytes()).thenReturn(new byte[] {1, 2});

    PhotoUpload upload = ReportMapper.toPhotoUpload(file);

    assertThat(upload.fileName()).isEqualTo(ReportMapper.DEFAULT_FILE_NAME);
    assertThat(upload.contentType()).isEqualTo(ReportMapper.DEFAULT_CONTENT_TYPE);
    assertThat(upload.content()).containsExactly(1, 2);
  }

  @Test
  @DisplayName("SC2 step 18: a missing file name gets the default too")
  void defaultForAMissingName() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(null);
    when(file.getContentType()).thenReturn("image/png");
    when(file.getBytes()).thenReturn(new byte[0]);

    PhotoUpload upload = ReportMapper.toPhotoUpload(file);

    assertThat(upload.fileName()).isEqualTo("photo");
    assertThat(upload.contentType()).isEqualTo("image/png");
    assertThat(upload.content()).isEmpty();
  }
}
