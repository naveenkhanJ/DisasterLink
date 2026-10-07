package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Optional;

/**
 * Someone who can receive a notification: a citizen, an organisation or a district officer.
 *
 * <p>Implements CH-18 and CH-06. The identifier and the kind are what a notification stores for its
 * recipient (model dictionary section 0.7, V2C-05). Citizens receive SMS, push and audible alerts,
 * push only with a device token; organisations and district officers receive SMS only, so no
 * notification is sent on a channel the recipient cannot receive.
 */
public interface Recipient {

  /**
   * Returns the identifier of the recipient.
   *
   * @return the identifier, unique within its kind
   */
  String getRecipientId();

  /**
   * Returns the kind of the recipient.
   *
   * @return the kind of the recipient
   */
  RecipientKind getRecipientKind();

  /**
   * Returns the telephone number the recipient is reached on.
   *
   * @return the contact number
   */
  String getContactNumber();

  /**
   * Returns the language the recipient wants notices in.
   *
   * @return the preferred language
   */
  Language getPreferredLanguage();

  /**
   * Returns the push token of the recipient's device.
   *
   * @return the device token, or empty if the recipient has no app
   */
  Optional<String> getDeviceToken();

  /**
   * Tells whether the recipient can receive a notification on a channel.
   *
   * @param channel the channel to send on
   * @return {@code true} if the recipient can receive it there
   */
  boolean canReceive(NotificationChannel channel);
}
