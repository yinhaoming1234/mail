import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
    public static void main(String[] args) {
        // Current pattern
        Pattern pattern = Pattern.compile(
                "MAIL\\s+FROM:\\s*<([^>]*)>",
                Pattern.CASE_INSENSITIVE);

        // Test cases
        String[] testCases = {
                "MAIL FROM:<test@localhost>",
                "MAIL FROM: <test@localhost>",
                "MAIL FROM :<test@localhost>",
                "MAIL FROM : <test@localhost>"
        };

        for (String testCase : testCases) {
            Matcher matcher = pattern.matcher(testCase);
            System.out.println("Test: '" + testCase + "'");
            System.out.println("Matches: " + matcher.find());
            if (matcher.find()) {
                matcher.reset();
                matcher.find();
                System.out.println("Group 1: " + matcher.group(1));
            }
            System.out.println();
        }
    }
}
