package io.github.chris013.practice.task02;

/**
 * Checks new passwords on sign-up and password change.
 *
 * <p>Task 02: read {@code tasks/02-password-policy-refactoring.md} first. This is legacy code: refactor it while
 * {@code PasswordCheckerCharacterizationTest} stays green, then implement {@link PasswordPolicy}.
 */
public class PasswordChecker {

    // checks the password. returns null if ok, otherwise the error
    public String check(String pw, String user, boolean isAdmin) {
        if (pw != null) {
            // check length (min 6)
            if (pw.length() >= 8) {
                if (!isAdmin || pw.length() >= 12) {
                    boolean d = false;
                    boolean u = false;
                    boolean l = false;
                    boolean s = false;
                    for (int i = 0; i < pw.length(); i++) {
                        char c = pw.charAt(i);
                        if (Character.isDigit(c)) {
                            d = true;
                        } else if (Character.isUpperCase(c)) {
                            u = true;
                        } else if (Character.isLowerCase(c)) {
                            l = true;
                        } else {
                            s = true;
                        }
                    }
                    if (d && u && l) {
                        // admins need special chars
                        if (isAdmin && !s) {
                            return "Admin passwords need a special character";
                        }
                        // username
                        if (user != null && pw.toLowerCase().contains(user.toLowerCase())) {
                            return "Password must not contain the username";
                        }
                        if (pw.length() > 64) {
                            return "Password too long";
                        }
                        return null;
                    } else {
                        return "Password needs digit, upper and lower case letter";
                    }
                } else {
                    return "Admin passwords need at least 12 characters";
                }
            } else {
                return "Password too short";
            }
        } else {
            return "Password missing";
        }
    }
}
