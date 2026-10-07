package lk.sliit.se3070.group058.disasterlink.shared.web;

/**
 * What the top bar of an officer page shows about the signed-in user: the layout reads it from the
 * model attribute {@code currentUser}.
 *
 * @param initials the letters in the avatar, for example {@code NJ}
 * @param name the name of the user
 * @param role the role label, for example {@code DMC Officer}
 */
public record CurrentUserView(String initials, String name, String role) {}
