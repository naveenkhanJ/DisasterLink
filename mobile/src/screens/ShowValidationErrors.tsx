/**
 * ShowValidationErrors.tsx — `showValidationErrors(errors)`: the summary above "Review Report" and
 * any error of no known field; each field's own error is drawn under that field by the report form
 * (SC2 E1 16a to 16c, E3; WF2P-S03e and S03b).
 *
 * Implements CH-27 (FD-12).
 */
import React from 'react';
import { View } from 'react-native';
import { ErrorText } from '../components/ui';
import { hasErrors, type FieldErrors } from './texts';

/**
 * Draws the summary of the errors.
 *
 * @param props the errors by field
 * @returns the summary, or nothing when there is no error
 */
export function ShowValidationErrors({ errors }: { errors: FieldErrors }) {
  if (!hasErrors(errors)) {
    return null;
  }
  return (
    <View>
      <ErrorText>Please correct the field marked above</ErrorText>
      {errors.other.map((error) => (
        <ErrorText key={error}>{error}</ErrorText>
      ))}
    </View>
  );
}
