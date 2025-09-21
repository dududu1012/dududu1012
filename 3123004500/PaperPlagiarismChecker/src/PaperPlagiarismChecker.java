import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class PaperPlagiarismChecker {
    private static final Pattern NON_CORE_CHAR = Pattern.compile("[^a-zA-Z0-9\u4e00-\u9fa5]");
    private static final int SIM_HASH_BITS = 64;
    private static final int MIN_N_GRAM = 1;
    private static final int MAX_N_GRAM = 2;
    private static final String CHECK_LOG_PATH = "查重记录.log";

    public static void main(String[] args) {
        try {
            // 命令行参数校验
            if (args.length != 3) {
                throw new IllegalArgumentException("参数错误！正确格式：\njava -jar main.jar [原文文件] [抄袭版论文] [结果文件]");
            }

            String origFilePath = args[0];
            String copyFilePath = args[1];
            String resultFilePath = args[2];

            // 文件验证与处理
            validateFile(origFilePath);
            validateFile(copyFilePath);
            String originalText = readFile(origFilePath);
            String copiedText = readFile(copyFilePath);

            // 核心查重逻辑
            String processedOrig = preprocessText(originalText);
            String processedCopy = preprocessText(copiedText);
            String[] origTokens = filterEmptyTokens(processedOrig.split(" "));
            String[] copyTokens = filterEmptyTokens(processedCopy.split(" "));
            int n = selectNGramSize(origTokens.length, copyTokens.length);
            Set<NGramHolder> origNgramSet = extractNgramSet(origTokens, n);
            Set<NGramHolder> copyNgramSet = extractNgramSet(copyTokens, n);

            long origSimHash = calculateSimHash(origNgramSet);
            long copySimHash = calculateSimHash(copyNgramSet);
            int hammingDist = calculateHammingDistance(origSimHash, copySimHash);
            double similarity = calculateSimHashSimilarity(hammingDist);
            String rateStr = String.format("%.2f%%", similarity * 100);

            // 以追加模式输出记录形式结果
            appendResultAsRecord(origFilePath, copyFilePath, rateStr, resultFilePath);

            // 详细日志记录
            logDetailedCheckResult(origFilePath, copyFilePath, n, origTokens.length,
                    copyTokens.length, origSimHash, copySimHash,
                    hammingDist, similarity, rateStr);

        } catch (Exception e) {
            System.err.println("查重失败：" + e.getMessage());
            logError(e.getMessage());
            System.exit(1);
        }
    }

    // 以追加模式写入结果记录（每条记录占一行，不覆盖历史内容）
    private static void appendResultAsRecord(String origPath, String copyPath,
                                             String rateStr, String resultPath) throws IOException {
        // 记录格式：时间|原文路径|抄袭文本路径|重复率（CSV风格，便于解析）
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String record = String.join("|",
                timestamp,
                origPath,
                copyPath,
                rateStr
        );

        // 追加写入结果文件（保留历史记录）
        File resultFile = new File(resultPath);
        if (resultFile.getParentFile() != null && !resultFile.getParentFile().exists()) {
            resultFile.getParentFile().mkdirs();
        }

        // 若文件不存在，先写入表头
        boolean isNewFile = !resultFile.exists();
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(resultPath),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {

            if (isNewFile) {
                writer.write("查重时间|原文路径|抄袭文本路径|重复率");
                writer.newLine(); // 表头后换行
            }
            writer.write(record);
            writer.newLine(); // 每条记录后换行
        }

        // 控制台输出
        System.out.println("查重完成，新增记录：");
        System.out.println(record);
        System.out.println("结果已追加至：" + resultFile.getAbsolutePath());
    }

    // 详细日志记录（内部追踪用）
    private static void logDetailedCheckResult(String origPath, String copyPath, int nGram,
                                               int origTokenCount, int copyTokenCount,
                                               long origHash, long copyHash, int hammingDist,
                                               double similarity, String rateStr) throws IOException {
        String logContent = "========================================\n" +
                "查重时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n" +
                "原文路径：" + origPath + "\n" +
                "抄袭文本路径：" + copyPath + "\n" +
                "n-gram粒度：" + nGram + "-gram\n" +
                "原文token数量：" + origTokenCount + "\n" +
                "抄袭文本token数量：" + copyTokenCount + "\n" +
                "SimHash值（原文）：0x" + Long.toHexString(origHash).toUpperCase() + "\n" +
                "SimHash值（抄袭文本）：0x" + Long.toHexString(copyHash).toUpperCase() + "\n" +
                "海明距离：" + hammingDist + "\n" +
                "相似度：" + String.format("%.4f", similarity) + "\n" +
                "最终重复率：" + rateStr + "\n" +
                "========================================\n\n";

        File logFile = new File(CHECK_LOG_PATH);
        if (logFile.getParentFile() != null && !logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
        Files.writeString(Paths.get(CHECK_LOG_PATH), logContent, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    // 工具类与核心算法实现（保持不变）
    static class NGramHolder {
        private final String[] tokens;
        private final int startIdx;
        private final int endIdx;

        public NGramHolder(String[] tokens, int startIdx, int endIdx) {
            this.tokens = tokens;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = startIdx; i < endIdx; i++) {
                if (i > startIdx) sb.append(" ");
                sb.append(tokens[i]);
            }
            return sb.toString();
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (int i = startIdx; i < endIdx; i++) {
                result = 31 * result + (tokens[i] == null ? 0 : tokens[i].hashCode());
                if (i > startIdx) {
                    result = 31 * result + " ".hashCode();
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NGramHolder that = (NGramHolder) o;
            if (endIdx - startIdx != that.endIdx - that.startIdx) return false;
            for (int i = startIdx, j = that.startIdx; i < endIdx; i++, j++) {
                if (!Objects.equals(tokens[i], that.tokens[j])) return false;
            }
            return true;
        }
    }

    private static void validateFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在：" + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new FileNotFoundException("不是有效文件：" + file.getAbsolutePath());
        }
    }

    private static String readFile(String filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    static String preprocessText(String text) {
        if (text == null || text.isEmpty()) return "";
        // 第一步：将所有非核心字符替换为空格
        String cleaned = NON_CORE_CHAR.matcher(text).replaceAll(" ");
        // 第二步：合并连续空格为单个空格，去除首尾空格，转小写
        cleaned = cleaned.replaceAll("\\s+", " ").trim().toLowerCase();
        return cleaned;
    }

    private static String[] filterEmptyTokens(String[] tokens) {
        List<String> validTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                validTokens.add(token);
            }
        }
        return validTokens.toArray(new String[0]);
    }

    private static int selectNGramSize(int origTokenCount, int copyTokenCount) {
        int minTokenCount = Math.min(origTokenCount, copyTokenCount);
        return minTokenCount < 2 ? MIN_N_GRAM : MAX_N_GRAM;
    }

    static Set<NGramHolder> extractNgramSet(String[] tokens, int n) {
        Set<NGramHolder> ngramSet = new HashSet<>();
        if (tokens.length < n) return ngramSet;

        for (int i = 0; i <= tokens.length - n; i++) {
            ngramSet.add(new NGramHolder(tokens, i, i + n));
        }
        return ngramSet;
    }

    static long calculateSimHash(Set<NGramHolder> ngramSet) {
        if (ngramSet.isEmpty()) return 0;

        int[] simHashVector = new int[SIM_HASH_BITS];
        ngramSet.parallelStream().forEach(ngram -> {
            long ngramHash = murmurHash3(ngram.toString());
            for (int i = 0; i < SIM_HASH_BITS; i++) {
                long bitMask = 1L << i;
                if ((ngramHash & bitMask) != 0) {
                    simHashVector[i]++;
                } else {
                    simHashVector[i]--;
                }
            }
        });

        long simHash = 0;
        for (int i = 0; i < SIM_HASH_BITS; i++) {
            if (simHashVector[i] > 0) {
                simHash |= (1L << i);
            }
        }
        return simHash;
    }

    static long murmurHash3(String text) {
        char[] data = text.toCharArray();
        int length = data.length;
        int seed = 0x9747b28c;
        int m = 0x5bd1e995;
        int r = 24;

        int hash = seed ^ length;
        int index = 0;

        while (length >= 4) {
            int k1 = (data[index] & 0xff) | ((data[index] >>> 8) & 0xff) << 8;
            k1 |= ((data[index + 1] & 0xff) << 16) | (((data[index + 1] >>> 8) & 0xff) << 24);
            int k2 = (data[index + 2] & 0xff) | ((data[index + 2] >>> 8) & 0xff) << 8;
            k2 |= ((data[index + 3] & 0xff) << 16) | (((data[index + 3] >>> 8) & 0xff) << 24);

            k1 *= m;
            k1 ^= k1 >>> r;
            k1 *= m;
            hash *= m;
            hash ^= k1;

            k2 *= m;
            k2 ^= k2 >>> r;
            k2 *= m;
            hash *= m;
            hash ^= k2;

            index += 4;
            length -= 4;
        }

        switch (length) {
            case 3:
                hash ^= (data[index + 2] & 0xff) << 16;
                hash ^= ((data[index + 2] >>> 8) & 0xff) << 24;
            case 2:
                hash ^= (data[index + 1] & 0xff) << 8;
                hash ^= ((data[index + 1] >>> 8) & 0xff) << 16;
            case 1:
                hash ^= data[index] & 0xff;
                hash ^= ((data[index] >>> 8) & 0xff) << 8;
                hash *= m;
        }

        hash ^= hash >>> 13;
        hash *= m;
        hash ^= hash >>> 15;

        return hash & 0xffffffffL;
    }

    static int calculateHammingDistance(long hashA, long hashB) {
        long xorResult = hashA ^ hashB;
        return Long.bitCount(xorResult);
    }

    static double calculateSimHashSimilarity(int hammingDist) {
        int clampedDist = Math.min(Math.max(hammingDist, 0), SIM_HASH_BITS);
        return 1.0 - (double) clampedDist / SIM_HASH_BITS;
    }

    private static void logError(String errorMsg) {
        try {
            String log = "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] 错误：" + errorMsg + "\n";
            Files.writeString(Paths.get(CHECK_LOG_PATH), log, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("错误日志记录失败：" + e.getMessage());
        }
    }
}
