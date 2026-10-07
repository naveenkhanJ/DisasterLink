package lk.sliit.se3070.group058.disasterlink.shared.domain;

/**
 * The lengths of the long text columns, in one place so that no entity repeats the number (no magic
 * numbers, standards section 4; P155 B-15). Annotation values must be constants, so these are
 * {@code static final} fields.
 */
public final class ColumnLengths {

  /** A free text of a few paragraphs: descriptions, messages, reasons, details. */
  public static final int LONG_TEXT = 2000;

  /** The findings an officer writes against a report. */
  public static final int FINDINGS = 4000;

  private ColumnLengths() {
    throw new AssertionError("ColumnLengths is a utility class");
  }
}
