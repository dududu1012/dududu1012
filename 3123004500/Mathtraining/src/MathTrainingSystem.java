import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathTrainingSystem {
    // 正则表达式定义
    private static final Pattern FRACTION_PATTERN = Pattern.compile("(?:(\\d+)')?(\\d+)/(\\d+)");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

    // 主方法：处理命令行参数
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printHelp();
                return;
            }

            // 处理题目生成参数（同时自动生成标准答案到Answers.txt）
            if (args.length >= 4 && args[0].equals("-n") && args[2].equals("-r")) {
                int questionCount = Integer.parseInt(args[1]);
                int range = Integer.parseInt(args[3]);

                if (questionCount <= 0 || questionCount > 10000) {
                    System.err.println("题目数量必须在1-10000之间");
                    return;
                }
                if (range < 1) {
                    System.err.println("数值范围必须大于等于1");
                    return;
                }

                generateQuestionsAndAnswers(questionCount, range);
                System.out.println("题目生成完成！已保存到Exercises.txt");
                System.out.println("标准答案已自动生成！已保存到Answers.txt");
            }
            // 处理用户输入自己的答案（保存到用户指定文件）
            else if (args.length == 3 && args[0].equals("-a") && args[1].equals("input")) {
                String userAnswerFile = args[2];
                inputUserAnswers(userAnswerFile);
                System.out.println("用户答案已保存到" + userAnswerFile);
            }
            // 处理答案校验参数（比对用户答案文件与Answers.txt）
            else if (args.length >= 4 && args[0].equals("-e") && args[2].equals("-a")) {
                String exerciseFile = args[1];
                String userAnswerFile = args[3];

                checkAnswers(exerciseFile, userAnswerFile);
                System.out.println("答案校验完成！结果已保存到Grade.txt");
            }
            else {
                printHelp();
            }
        } catch (Exception e) {
            System.err.println("程序运行出错：" + e.getMessage());
            e.printStackTrace();
            printHelp();
        }
    }

    // 打印帮助信息
    private static void printHelp() {
        System.out.println("小学数学四则运算系统");
        System.out.println("用法1：生成题目和标准答案");
        System.out.println("  MathTrainingSystem -n <题目数量> -r <数值范围>");
        System.out.println("  示例：MathTrainingSystem -n 10 -r 10 （生成10道10以内的题目和标准答案）");
        System.out.println("\n用法2：用户输入自己的答案");
        System.out.println("  MathTrainingSystem -a input <用户答案文件>");
        System.out.println("  示例：MathTrainingSystem -a input MyAnswers.txt");
        System.out.println("  说明：读取Exercises.txt并让用户输入自己的答案，保存到指定文件");
        System.out.println("\n用法3：校验答案（比对用户答案与Answers.txt）");
        System.out.println("  MathTrainingSystem -e <题目文件> -a <用户答案文件>");
        System.out.println("  示例：MathTrainingSystem -e Exercises.txt -a MyAnswers.txt");
        System.out.println("  说明：将用户答案与系统生成的Answers.txt比对，结果保存到Grade.txt");
    }

    // 生成题目并自动生成标准答案（保存到Answers.txt）
    static void generateQuestionsAndAnswers(int count, int range) throws IOException {
        List<String> exercises = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        Set<String> uniqueExpressions = new HashSet<>(); // 用于去重

        Random random = new Random();
        int generated = 0;

        // 最多尝试生成10倍数量的题目，避免死循环
        int maxAttempts = count * 10;
        int attempts = 0;

        while (generated < count && attempts < maxAttempts) {
            attempts++;
            // 随机生成1-3个运算符的表达式
            int opCount = random.nextInt(3) + 1;
            String expression = generateExpression(opCount, range, random);

            if (expression == null) continue;

            // 标准化表达式用于去重
            String normalized = normalizeExpression(expression);
            if (uniqueExpressions.contains(normalized)) {
                continue;
            }

            // 计算结果，确保非负
            Fraction result = calculateExpression(expression);
            if (result == null || result.numerator < 0) {
                continue; // 计算出错或结果为负，跳过该题目
            }

            // 格式化输出题目和答案
            int questionNum = generated + 1;
            exercises.add(questionNum + ". " + expression + " =");
            answers.add(questionNum + ". " + formatFraction(result));

            uniqueExpressions.add(normalized);
            generated++;
        }

        if (generated < count) {
            System.err.println("警告：生成题目数量不足（" + generated + "/" + count + "），可能是去重或规则限制导致");
        }

        // 保存题目和标准答案（标准答案文件名为Answers.txt）
        writeToFile("Exercises.txt", exercises);
        writeToFile("Answers.txt", answers);
    }

    // 生成单个表达式
    static String generateExpression(int opCount, int range, Random random) {
        List<String> parts = new ArrayList<>();
        parts.add(generateNumber(range, random));

        for (int i = 0; i < opCount; i++) {
            String op = getRandomOperator(random);
            String num = generateNumber(range, random);

            // 处理减法和除法的特殊规则
            if (op.equals("-")) {
                // 确保结果非负：被减数 >= 减数
                while (compareNumbers(parts.get(parts.size() - 1), num) < 0) {
                    num = generateNumber(range, random);
                }
            }
            else if (op.equals("÷")) {
                // 确保除法结果为真分数：被除数 < 除数（分数除法）
                while (true) {
                    Fraction num1 = parseNumber(parts.get(parts.size() - 1));
                    Fraction num2 = parseNumber(num);
                    // 避免除以零
                    if (num2.numerator == 0) {
                        num = generateNumber(range, random);
                        continue;
                    }
                    // 检查是否为真分数结果（a/b ÷ c/d = ad/bc，应小于1）
                    if ((long)num1.numerator * num2.denominator < (long)num1.denominator * num2.numerator) {
                        break; // 分子小于分母，结果为真分数
                    }
                    num = generateNumber(range, random);
                }
            }

            parts.add(op);
            parts.add(num);
        }

        // 随机添加括号（仅当有2个以上运算符时）
        if (opCount >= 2 && random.nextBoolean()) {
            return addParentheses(String.join(" ", parts), random);
        }

        return String.join(" ", parts);
    }

    // 生成随机数字（自然数或真分数）
    private static String generateNumber(int range, Random random) {
        if (random.nextBoolean()) {
            // 生成自然数（0到range-1）
            return String.valueOf(random.nextInt(range));
        } else {
            // 生成真分数：分子 < 分母
            int denominator = random.nextInt(range - 1) + 2; // 分母2~range
            int numerator = random.nextInt(denominator - 1) + 1; // 分子1~分母-1

            // 10%概率生成带分数（整数部分0到range-1）
            if (random.nextDouble() < 0.1) {
                int integerPart = random.nextInt(range);
                if (integerPart > 0) {
                    return integerPart + "'" + numerator + "/" + denominator;
                }
            }

            return numerator + "/" + denominator;
        }
    }

    // 获取随机运算符
    private static String getRandomOperator(Random random) {
        String[] ops = {"+", "-", "×", "÷"};
        return ops[random.nextInt(ops.length)];
    }

    // 为表达式添加括号
    private static String addParentheses(String expr, Random random) {
        String[] parts = expr.split(" ");
        int opCount = (parts.length - 1) / 2;

        if (opCount < 2) return expr; // 至少需要2个运算符才能加括号

        // 选择括号位置（确保括号包含2个操作数和1个运算符）
        int startOp = random.nextInt(opCount - 1) + 1; // 从1开始避免重复
        int endOp = startOp + 1;

        // 构建带括号的表达式
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i == (startOp - 1) * 2) {
                sb.append("(");
            }
            sb.append(parts[i]);
            if (i == endOp * 2) {
                sb.append(")");
            }
            if (i != parts.length - 1) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    // 标准化表达式（用于去重）
    static String normalizeExpression(String expr) {
        // 移除括号并标准化加法/乘法顺序
        expr = expr.replaceAll("\\(|\\)", "");
        String[] parts = expr.split(" ");

        // 处理加法和乘法的交换律
        for (int i = 1; i < parts.length; i += 2) {
            if (parts[i].equals("+") || parts[i].equals("×")) {
                Fraction num1 = parseNumber(parts[i-1]);
                Fraction num2 = parseNumber(parts[i+1]);

                if (compareFractions(num1, num2) > 0) {
                    // 交换两个数的位置
                    String temp = parts[i-1];
                    parts[i-1] = parts[i+1];
                    parts[i+1] = temp;
                }
            }
        }

        return String.join(" ", parts);
    }

    // 计算表达式结果
    static Fraction calculateExpression(String expr) {
        try {
            // 处理括号（递归）
            Pattern pattern = Pattern.compile("\\(([^()]+)\\)");
            Matcher matcher = pattern.matcher(expr);
            while (matcher.find()) {
                String subExpr = matcher.group(1);
                Fraction subResult = calculateBasicExpression(subExpr);
                if (subResult == null) return null;
                expr = expr.replace("(" + subExpr + ")", formatFraction(subResult, false));
            }

            // 处理无括号的基本表达式
            return calculateBasicExpression(expr);
        } catch (Exception e) {
            System.err.println("计算表达式错误: " + expr + "，错误: " + e.getMessage());
            return null;
        }
    }

    // 计算无括号的基本表达式（先乘除后加减）
    private static Fraction calculateBasicExpression(String expr) {
        String[] parts = expr.split(" ");
        List<Object> tokens = new ArrayList<>();

        // 解析 tokens
        for (String part : parts) {
            if (part.equals("+") || part.equals("-") || part.equals("×") || part.equals("÷")) {
                tokens.add(part);
            } else {
                try {
                    tokens.add(parseNumber(part));
                } catch (Exception e) {
                    System.err.println("解析数字错误: " + part);
                    return null;
                }
            }
        }

        // 先处理乘除
        int i = 1;
        while (i < tokens.size()) {
            String op = (String) tokens.get(i);
            if (op.equals("×") || op.equals("÷")) {
                Fraction num1 = (Fraction) tokens.get(i-1);
                Fraction num2 = (Fraction) tokens.get(i+1);

                // 避免除以零
                if (op.equals("÷") && num2.numerator == 0) {
                    return null;
                }

                Fraction result = calculate(num1, num2, op);
                if (result == null) return null;

                // 移除已计算的元素并插入结果
                tokens.remove(i+1);
                tokens.remove(i);
                tokens.remove(i-1);
                tokens.add(i-1, result);
            } else {
                i += 2;
            }
        }

        // 再处理加减
        if (tokens.size() == 0) return new Fraction(0, 1);
        Fraction result = (Fraction) tokens.get(0);
        i = 1;
        while (i < tokens.size()) {
            String op = (String) tokens.get(i);
            Fraction num = (Fraction) tokens.get(i+1);
            result = calculate(result, num, op);
            if (result == null) return null;
            i += 2;
        }

        return simplifyFraction(result);
    }

    // 执行基本运算
    private static Fraction calculate(Fraction a, Fraction b, String op) {
        switch (op) {
            case "+":
                return add(a, b);
            case "-":
                return subtract(a, b);
            case "×":
                return multiply(a, b);
            case "÷":
                return divide(a, b);
            default:
                return null;
        }
    }

    // 分数加法
    static Fraction add(Fraction a, Fraction b) {
        int numerator = a.numerator * b.denominator + b.numerator * a.denominator;
        int denominator = a.denominator * b.denominator;
        return new Fraction(numerator, denominator);
    }

    // 分数减法（确保结果非负）
     static Fraction subtract(Fraction a, Fraction b) {
        if (compareFractions(a, b) < 0) return null;
        int numerator = a.numerator * b.denominator - b.numerator * a.denominator;
        int denominator = a.denominator * b.denominator;
        return simplifyFraction(new Fraction(numerator, denominator));
    }

    // 分数乘法
    static Fraction multiply(Fraction a, Fraction b) {
        int numerator = a.numerator * b.numerator;
        int denominator = a.denominator * b.denominator;
        return simplifyFraction(new Fraction(numerator, denominator));
    }

    // 分数除法（确保除数不为零）
    static Fraction divide(Fraction a, Fraction b) {
        if (b.numerator == 0) {
            return null; // 除以零，返回null表示无效
        }
        int numerator = a.numerator * b.denominator;
        int denominator = a.denominator * b.numerator;
        return simplifyFraction(new Fraction(numerator, denominator));
    }

    // 解析数字（自然数或分数）
    static Fraction parseNumber(String str) {
        Matcher fractionMatcher = FRACTION_PATTERN.matcher(str);
        if (fractionMatcher.matches()) {
            // 解析分数
            int integerPart = fractionMatcher.group(1) != null ? Integer.parseInt(fractionMatcher.group(1)) : 0;
            int numerator = Integer.parseInt(fractionMatcher.group(2));
            int denominator = Integer.parseInt(fractionMatcher.group(3));

            // 确保分母不为零
            if (denominator == 0) {
                throw new IllegalArgumentException("无效的分数（分母为零）: " + str);
            }

            return new Fraction(
                    integerPart * denominator + numerator,
                    denominator
            );
        } else if (INTEGER_PATTERN.matcher(str).matches()) {
            // 解析自然数
            return new Fraction(Integer.parseInt(str), 1);
        } else {
            throw new IllegalArgumentException("无效的数字格式：" + str);
        }
    }

    // 比较两个数字字符串的大小
    private static int compareNumbers(String aStr, String bStr) {
        Fraction a = parseNumber(aStr);
        Fraction b = parseNumber(bStr);
        return compareFractions(a, b);
    }

    // 比较两个分数的大小
    static int compareFractions(Fraction a, Fraction b) {
        long left = (long) a.numerator * b.denominator;
        long right = (long) b.numerator * a.denominator;
        return Long.compare(left, right);
    }

    // 简化分数
    static Fraction simplifyFraction(Fraction f) {
        if (f.denominator == 0) return new Fraction(0, 1);

        // 确保分母为正
        int sign = 1;
        if (f.denominator < 0) {
            sign = -1;
            f = new Fraction(f.numerator * sign, f.denominator * sign);
        }

        // 计算最大公约数
        int gcd = gcd(Math.abs(f.numerator), f.denominator);
        return new Fraction(f.numerator / gcd, f.denominator / gcd);
    }

    // 计算最大公约数
    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    // 格式化分数为字符串
    private static String formatFraction(Fraction f) {
        return formatFraction(f, true);
    }

    private static String formatFraction(Fraction f, boolean showIntegerZero) {
        f = simplifyFraction(f);

        // 处理零
        if (f.numerator == 0) {
            return showIntegerZero ? "0" : "0/1";
        }

        // 处理整数
        if (f.denominator == 1) {
            return String.valueOf(f.numerator);
        }

        // 处理带分数
        if (Math.abs(f.numerator) > f.denominator) {
            int integerPart = f.numerator / f.denominator;
            int numerator = Math.abs(f.numerator) % f.denominator;
            return integerPart + "'" + numerator + "/" + f.denominator;
        }

        // 普通分数
        return f.numerator + "/" + f.denominator;
    }

    // 写入文件
    static void writeToFile(String filename, List<String> content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : content) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // 读取文件内容
    static List<String> readFromFile(String filename) throws IOException {
        List<String> content = new ArrayList<>();
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filename);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    content.add(line);
                }
            }
        }
        return content;
    }

    // 用户输入自己的答案并保存到指定文件
    private static void inputUserAnswers(String userAnswerFile) throws IOException {
        List<String> exercises = readFromFile("Exercises.txt");
        List<String> answers = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        System.out.println("===== 开始输入您的答案 =====");
        System.out.println("请输入每道题的答案，格式说明：");
        System.out.println("- 整数直接输入数字（如：5）");
        System.out.println("- 真分数输入格式（如：3/5）");
        System.out.println("- 带分数输入格式（如：2'3/8）");
        System.out.println("===================");

        for (String exercise : exercises) {
            // 提取题目编号和内容
            String[] parts = exercise.split("\\. ", 2);
            if (parts.length < 2) {
                System.err.println("跳过无效题目格式: " + exercise);
                continue;
            }

            int questionNum = Integer.parseInt(parts[0]);
            String questionContent = parts[1];

            // 显示题目并获取答案
            System.out.print("\n第" + questionNum + "题: " + questionContent + " ");
            String userAnswer = scanner.nextLine().trim();

            // 简单验证答案格式
            if (isValidAnswerFormat(userAnswer)) {
                answers.add(questionNum + ". " + userAnswer);
            } else {
                System.out.println("答案格式无效，请重新输入（例如：5、3/5、2'3/8）");
                // 重新获取答案
                System.out.print("第" + questionNum + "题: " + questionContent + " ");
                userAnswer = scanner.nextLine().trim();
                answers.add(questionNum + ". " + userAnswer);
            }
        }

        scanner.close();
        writeToFile(userAnswerFile, answers);
    }

    // 验证答案格式是否有效
    private static boolean isValidAnswerFormat(String answer) {
        if (answer.isEmpty()) return false;
        // 匹配整数、真分数或带分数
        return INTEGER_PATTERN.matcher(answer).matches() ||
                FRACTION_PATTERN.matcher(answer).matches();
    }

    // 校验答案：比对用户答案文件与系统生成的Answers.txt（标准答案）
    static void checkAnswers(String exerciseFile, String userAnswerFile) {
        List<String> reportLines = new ArrayList<>();
        try {
            // 读取标准答案（系统生成的Answers.txt）和用户答案
            List<String> standardAnswers = readFromFile("Answers.txt");
            List<String> userAnswers = readFromFile(userAnswerFile);

            // 校验答案数量是否一致
            if (standardAnswers.size() != userAnswers.size()) {
                // 定义异常对象并抛出
                IllegalArgumentException mismatchEx = new IllegalArgumentException(
                        "标准答案数量与用户答案数量不匹配：" +
                                "标准答案" + standardAnswers.size() + "道，用户答案" + userAnswers.size() + "道"
                );
                throw mismatchEx;
            }

            // 逐题比对答案
            List<Integer> correctList = new ArrayList<>();
            List<Integer> wrongList = new ArrayList<>();

            for (int i = 0; i < standardAnswers.size(); i++) {
                String standardLine = standardAnswers.get(i);
                String userLine = userAnswers.get(i);

                // 解析标准答案的题目编号和分数
                Fraction standardFraction = parseAnswerLine(standardLine, "标准答案", i + 1);
                // 解析用户答案的题目编号和分数
                Fraction userFraction = parseAnswerLine(userLine, "用户答案", i + 1);
                // 提取题目编号（从标准答案行解析，确保一致性）
                int questionNum = extractQuestionNumber(standardLine, i + 1);

                // 比对分数（标准化后比较）
                if (compareFractions(standardFraction, userFraction) == 0) {
                    correctList.add(questionNum);
                } else {
                    wrongList.add(questionNum);
                }
            }

            // 生成评分报告（按要求格式输出）
            reportLines.add("Correct: " + correctList.size() + " (" + join(correctList) + ")");
            reportLines.add("Wrong: " + wrongList.size() + " (" + join(wrongList) + ")");

        } catch (IllegalArgumentException e) {
            // 处理业务异常：记录错误后重新抛出，确保测试用例能捕获
            reportLines.add("Error: " + e.getMessage());
            e.printStackTrace();
            throw e; // 关键：将异常传递给测试框架
        } catch (Exception e) {
            // 处理其他异常（如IO错误）：仅记录，不抛出
            reportLines.add("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                writeToFile("Grade.txt", reportLines);
            } catch (IOException e) {
                System.err.println("生成Grade.txt失败: " + e.getMessage());
            }
        }
    }

    // 解析单条答案行（格式：题目编号. 答案）
    private static Fraction parseAnswerLine(String line, String type, int index) {
        // 分割“题目编号. ”和“答案”（如“1. 3/4”分割为“1”和“3/4”）
        String[] parts = line.split("\\. ", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    type + "格式错误（第" + index + "行）：" + line + "，正确格式应为“题目编号. 答案”（如1. 3/4）"
            );
        }

        String answerStr = parts[1].trim();
        // 校验答案格式并解析为分数
        if (!isValidAnswerFormat(answerStr)) {
            throw new IllegalArgumentException(
                    type + "格式无效（第" + index + "行）：" + answerStr + "，正确格式：整数（如5）、分数（如3/5）、带分数（如2'3/8）"
            );
        }

        // 解析并标准化分数（简化、统一格式）
        return simplifyFraction(parseNumber(answerStr));
    }

    // 从答案行中提取题目编号
    private static int extractQuestionNumber(String line, int index) {
        String[] parts = line.split("\\. ", 2);
        if (parts.length < 2 || !INTEGER_PATTERN.matcher(parts[0]).matches()) {
            throw new IllegalArgumentException(
                    "无法提取题目编号（第" + index + "行）：" + line + "，正确格式应为“题目编号. 答案”（如1. 3/4）"
            );
        }
        return Integer.parseInt(parts[0]);
    }

    // 拼接列表为字符串（格式：1, 3, 5）
    private static String join(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    // 分数类
    static class Fraction {
        int numerator;   // 分子
        int denominator; // 分母

        Fraction(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }

}