/**
 * descriptionAddress.ts — how the optional "Address or landmark" is kept in the report's description
 * (implementation-only; standards §10 "The address", P214, deviation D-UC2-4; serves CH-27). The
 * address is a phone-side helper, not a model attribute: while the citizen edits, the screen holds the
 * description text and the address text apart; `ReportDraft.description` is their composition, the
 * description followed, when an address is given, by a new line and `ADDRESS_LABEL` and the address.
 * Reopening a draft splits that last line back into the field.
 */
import { ADDRESS_LABEL } from '../screens/texts';

/** A description and an address, as the two fields of the form hold them. */
export interface DescriptionParts {
  description: string;
  address: string;
}

/**
 * Composes the stored description.
 *
 * @param description what the Description field holds
 * @param address what the "Address or landmark" field holds
 * @returns the description alone when the address is blank, else the description, a new line, and
 *   `ADDRESS_LABEL` followed by the address
 */
export function composeDescription(description: string, address: string): string {
  return address.trim() === '' ? description : `${description}\n${ADDRESS_LABEL}${address}`;
}

/**
 * Splits a stored description back into the two fields.
 *
 * @param stored the stored description, if there is one
 * @returns when the last line starts with `ADDRESS_LABEL`, that line (without the label) is the
 *   address and the rest is the description; otherwise everything is the description
 */
export function splitDescription(stored: string | undefined): DescriptionParts {
  const text = stored ?? '';
  const lastBreak = text.lastIndexOf('\n');
  const lastLine = text.slice(lastBreak + 1);
  if (!lastLine.startsWith(ADDRESS_LABEL)) {
    return { description: text, address: '' };
  }
  return {
    description: lastBreak < 0 ? '' : text.slice(0, lastBreak),
    address: lastLine.slice(ADDRESS_LABEL.length),
  };
}
