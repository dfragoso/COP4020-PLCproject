package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),

                //5 matching cases
                Arguments.of("Underscore in name", "thefat_mouse@gmail.com", true),
                Arguments.of("Dot in name", "thefat.mouse@ufl.edu", true),
                Arguments.of("Only one letter", "z@gmail.com", true),
                Arguments.of("Only a number", "1@gmail.com", true),
                Arguments.of("Number first", "1hello@gmail.com", true),

                //5 not matching cases
                Arguments.of("No name", "@gmail.com", false),
                Arguments.of("No domain", "hello.com", false),
                Arguments.of("No .com", "hello@world", false),
                Arguments.of("No @", "hellogmail.com", false),
                Arguments.of("Empty String", "", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),

                //5 matching cases
                Arguments.of("length of 12", "kkkkkkkkkkkk", true),
                Arguments.of("Only Characters, size 16", "@#!$#^#^$&%&*&(&", true),
                Arguments.of("Only numbers", "12345678910111213", true),
                Arguments.of("length of 20", "jsjsjsjsjsjsjsjsjsjs", true),
                Arguments.of("Even String, size 10", "12345hello", true),

                //5 not matching cases
                Arguments.of("Length of 9", "ooooooooo", false),
                Arguments.of("Length of 0", "", false),
                Arguments.of("Length of 21", "kkkkkkkkk!kkkkkkkkkkk", false),
                Arguments.of("Odd string, length of 11", "helloworld1", false),
                Arguments.of("Size of 1", "*", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),

                //5 matching
                Arguments.of("Empty list", "[]", true),
                Arguments.of("No spaces", "[1,2,3]", true),
                Arguments.of("Mixed spaces", "[1,2, 3, 4, 5,6]", true),
                Arguments.of("Big digits", "[100, 5929939392]", true),
                Arguments.of("One big number", "[9999999999999999999999999999999999999999999999999999999999999999999999999]", true),

                //5 not matching
                Arguments.of("Trailing comma", "[1, 2, 3,]", false),
                Arguments.of("Starting comma, no space", "[,1]", false),
                Arguments.of("Starting comma, space", "[, 1,4,5]", false),
                Arguments.of("Only numbers, no spaces", "[1234]", false),
                Arguments.of("Wrong brackets", "{1, 2, 3, 4}", false)
        );
    }

            //Arguments.of("", "", true),
            //Arguments.of("", "", false)
    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {

        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Number", "1", true),
                Arguments.of("Decimal", "1.2", true),
                Arguments.of("Leading zero", "0.5", true),
                Arguments.of("trailing zero", "5.0", true),
                Arguments.of("Sign", "+1.9", true),

                Arguments.of("Leading decimal", ".2", false),
                Arguments.of("Trailing decimal", "2.", false),
                Arguments.of("Two + signs", "++0.9", false),
                Arguments.of("Two - signs", "--0.9", false),
                Arguments.of("No number", "", false)
        );

    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {

        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Single quotes", "\"\"", true),
                Arguments.of("Word ", "\"hello\"", true),
                Arguments.of("Word ", "\"Hello, World!\"", true),
                Arguments.of("Word with tab ", "\"1\\t2\"", true),
                Arguments.of("No letter ", "\"*(&^$^&&^$^\"", true),

                Arguments.of("No string", "", false),
                Arguments.of("One quote", "\"unterminated", false),
                Arguments.of("One quote end", "unterminated\"", false),
                Arguments.of("Scape", "\"invalid\\escape\"", false),
                Arguments.of("char", "888888", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
