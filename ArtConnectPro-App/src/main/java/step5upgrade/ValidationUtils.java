package step5upgrade;

public class ValidationUtils {

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean validEmail(String email) {
        return email != null && email.contains("@");
    }
}