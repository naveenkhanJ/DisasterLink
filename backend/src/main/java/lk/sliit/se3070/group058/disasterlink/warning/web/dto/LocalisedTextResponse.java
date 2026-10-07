package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

/**
 * A text in the three languages on the wire (the {@code LocalisedText} of a stored warning).
 *
 * <p>Implementation-only wire record (standards section 12.1).
 *
 * @param sinhala the text in Sinhala
 * @param tamil the text in Tamil
 * @param english the text in English
 */
public record LocalisedTextResponse(String sinhala, String tamil, String english) {}
