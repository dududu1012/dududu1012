import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

public class MathTrainingSystemTest {

    private static final String EXERCISES = "Exercises.txt";
    private static final String ANSWERS = "Answers.txt";
    private static final String USER_ANSWERS = "UserAnswers.txt";
    private static final String GRADE = "Grade.txt";

    @Before
    @After
    public void cleanFiles() {
        deleteFile(EXERCISES);
        deleteFile(ANSWERS);
        deleteFile(USER_ANSWERS);
        deleteFile(GRADE);
    }

    private void deleteFile(String name) {
        File file = new File(name);
        if (file.exists()) file.delete();
    }

    private void writeFile(String name, List<String> content) throws IOException {
        MathTrainingSystem.writeToFile(name, content);
    }

    private List<String> readFile(String name) throws IOException {
        return MathTrainingSystem.readFromFile(name);
    }

    // 测试1：基础题目生成（数量、格式）
    @Test
    public void testGenerateBasicQuestions() throws IOException {
        MathTrainingSystem.generateQuestionsAndAnswers(3, 5);
        List<String> exercises = readFile(EXERCISES);
        List<String> answers = readFile(ANSWERS);

        assertEquals(3, exercises.size());
        assertEquals(3, answers.size());
        for (String ex : exercises) {
            assertTrue(ex.matches("\\d+\\. .+ ="));
        }
    }

    // 测试2：分数解析（整数、真分数、带分数）
    @Test
    public void testFractionParsing() {
        MathTrainingSystem.Fraction f1 = MathTrainingSystem.parseNumber("7");
        assertEquals(7, f1.numerator);
        assertEquals(1, f1.denominator);

        MathTrainingSystem.Fraction f2 = MathTrainingSystem.parseNumber("3/4");
        assertEquals(3, f2.numerator);
        assertEquals(4, f2.denominator);

        MathTrainingSystem.Fraction f3 = MathTrainingSystem.parseNumber("2'1/3");
        assertEquals(7, f3.numerator); // 2×3+1=7
        assertEquals(3, f3.denominator);
    }

    // 测试3：分数加法
    @Test
    public void testFractionAddition() {
        MathTrainingSystem.Fraction a = new MathTrainingSystem.Fraction(1, 2);
        MathTrainingSystem.Fraction b = new MathTrainingSystem.Fraction(1, 3);
        MathTrainingSystem.Fraction sum = MathTrainingSystem.add(a, b);

        assertEquals(5, sum.numerator);
        assertEquals(6, sum.denominator);
    }

    // 测试4：分数减法（结果非负）
    @Test
    public void testFractionSubtraction() {
        MathTrainingSystem.Fraction a = new MathTrainingSystem.Fraction(5, 6);
        MathTrainingSystem.Fraction b = new MathTrainingSystem.Fraction(1, 3);
        MathTrainingSystem.Fraction diff = MathTrainingSystem.subtract(a, b);

        assertEquals(1, diff.numerator);
        assertEquals(2, diff.denominator);
    }

    // 测试5：分数乘法
    @Test
    public void testFractionMultiplication() {
        MathTrainingSystem.Fraction a = new MathTrainingSystem.Fraction(2, 3);
        MathTrainingSystem.Fraction b = new MathTrainingSystem.Fraction(3, 4);
        MathTrainingSystem.Fraction product = MathTrainingSystem.multiply(a, b);

        assertEquals(1, product.numerator);
        assertEquals(2, product.denominator);
    }

    // 测试6：分数除法（除数非零）
    @Test
    public void testFractionDivision() {
        MathTrainingSystem.Fraction a = new MathTrainingSystem.Fraction(1, 2);
        MathTrainingSystem.Fraction b = new MathTrainingSystem.Fraction(3, 4);
        MathTrainingSystem.Fraction quotient = MathTrainingSystem.divide(a, b);

        assertEquals(2, quotient.numerator);
        assertEquals(3, quotient.denominator);
    }

    // 测试7：答案全对校验
    @Test
    public void testAllCorrectAnswers() throws IOException {
        List<String> stdAnswers = Arrays.asList(
                "1. 3/2",
                "2. 5",
                "3. 1'1/3"
        );
        List<String> userAnswers = Arrays.asList(
                "1. 3/2",
                "2. 5",
                "3. 4/3"
        );
        writeFile(ANSWERS, stdAnswers);
        writeFile(USER_ANSWERS, userAnswers);

        MathTrainingSystem.checkAnswers("", USER_ANSWERS);
        List<String> grade = readFile(GRADE);

        assertEquals("Correct: 3 (1, 2, 3)", grade.get(0));
        assertEquals("Wrong: 0 ()", grade.get(1));
    }

    // 测试8：答案全错校验
    @Test
    public void testAllWrongAnswers() throws IOException {
        List<String> stdAnswers = Arrays.asList("1. 2/3");
        List<String> userAnswers = Arrays.asList("1. 3/2");
        writeFile(ANSWERS, stdAnswers);
        writeFile(USER_ANSWERS, userAnswers);

        MathTrainingSystem.checkAnswers("", USER_ANSWERS);
        List<String> grade = readFile(GRADE);

        assertEquals("Correct: 0 ()", grade.get(0));
        assertEquals("Wrong: 1 (1)", grade.get(1));
    }

    // 测试9：答案数量不匹配异常
    @Test(expected = IllegalArgumentException.class)
    public void testAnswerCountMismatch() throws IOException {
        List<String> stdAnswers = Arrays.asList("1. 1", "2. 2", "3. 3");
        List<String> userAnswers = Arrays.asList("1. 1", "2. 3");
        writeFile(ANSWERS, stdAnswers);
        writeFile(USER_ANSWERS, userAnswers);

        MathTrainingSystem.checkAnswers("", USER_ANSWERS);
    }

    // 测试10：带括号表达式计算
    @Test
    public void testParenthesesExpression() {
        String expr = "(1 + 1/2) × 2";
        MathTrainingSystem.Fraction result = MathTrainingSystem.calculateExpression(expr);

        assertEquals(3, result.numerator);
        assertEquals(1, result.denominator);
    }
}