import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
import java.util.Set;

public class PaperPlagiarismCheckerTest {

    // ====================== 测试 preprocessText() 函数 ======================

    @Test
    public void testPreprocessText_SpecialChars() {
        // 特殊字符过滤：保留允许的标点，过滤其他符号
        String input = "Test@#$%^&*()_+{}[]|\\<>?/";
        String expected = "test";  // 特殊符号全部被过滤
        assertEquals(expected, PaperPlagiarismChecker.preprocessText(input));
    }

    @Test
    public void testPreprocessText_EmptyInput() {
        // 空输入处理
        assertEquals("", PaperPlagiarismChecker.preprocessText(""));
        assertEquals("", PaperPlagiarismChecker.preprocessText(null));
        assertEquals("", PaperPlagiarismChecker.preprocessText("   \t\n"));  // 空白字符
    }

    // ====================== 测试 extractNgramSet() 函数 ======================
    @Test
    public void testExtractNgramSet_2GramNormal() {
        // 正常2-gram提取
        String[] tokens = {"a", "b", "c", "d"};
        Set<PaperPlagiarismChecker.NGramHolder> result = PaperPlagiarismChecker.extractNgramSet(tokens, 2);
        assertEquals(3, result.size());  // 4个token提取3个2-gram
    }

    @Test
    public void testExtractNgramSet_1Gram() {
        // 1-gram提取（每个token单独作为gram）
        String[] tokens = {"x", "y", "z"};
        Set<PaperPlagiarismChecker.NGramHolder> result = PaperPlagiarismChecker.extractNgramSet(tokens, 1);
        assertEquals(3, result.size());  // 3个token提取3个1-gram
    }

    @Test
    public void testExtractNgramSet_BoundaryCases() {
        // 边界情况1：token数量等于n
        String[] tokens1 = {"a", "b"};
        assertEquals(1, PaperPlagiarismChecker.extractNgramSet(tokens1, 2).size());

        // 边界情况2：token数量小于n
        String[] tokens2 = {"a"};
        assertEquals(0, PaperPlagiarismChecker.extractNgramSet(tokens2, 2).size());

        // 边界情况3：空token数组
        assertEquals(0, PaperPlagiarismChecker.extractNgramSet(new String[0], 1).size());
    }

    @Test
    public void testExtractNgramSet_DuplicateHandling() {
        String[] tokens = {"a", "a", "a"};
        Set<PaperPlagiarismChecker.NGramHolder> ngramSet = PaperPlagiarismChecker.extractNgramSet(tokens, 2);
        assertEquals(1, ngramSet.size());
    }

    // ====================== 测试 murmurHash3() 函数 ======================
    @Test
    public void testMurmurHash3_Consistency() {
        // 哈希一致性：相同输入必产相同输出
        String text = "consistency test 哈希一致性";
        long hash1 = PaperPlagiarismChecker.murmurHash3(text);
        long hash2 = PaperPlagiarismChecker.murmurHash3(text);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testMurmurHash3_Distribution() {
        // 哈希分散性：相似输入应产生不同输出
        String text1 = "similar text";
        String text2 = "similar text!";  // 仅多一个标点
        assertNotEquals(PaperPlagiarismChecker.murmurHash3(text1), PaperPlagiarismChecker.murmurHash3(text2));
    }

    @Test
    public void testMurmurHash3_EdgeInputs() {
        // 边缘输入哈希测试
        long hashEmpty = PaperPlagiarismChecker.murmurHash3("");
        long hashLong = PaperPlagiarismChecker.murmurHash3("a".repeat(1000));  // 长字符串
        long hashChinese = PaperPlagiarismChecker.murmurHash3("中文哈希测试");

        // 只需验证不抛出异常且哈希值有效
        assertTrue(hashEmpty >= 0);
        assertTrue(hashLong >= 0);
        assertTrue(hashChinese >= 0);
    }

    // ====================== 测试 calculateSimHash() 函数 ======================
    @Test
    public void testCalculateSimHash_EmptySet() {
        // 空n-gram集合应返回0
        Set<PaperPlagiarismChecker.NGramHolder> emptySet = new HashSet<>();
        assertEquals(0, PaperPlagiarismChecker.calculateSimHash(emptySet));
    }

    @Test
    public void testCalculateSimHash_SingleNgram() {
        // 单个n-gram的SimHash计算
        String[] tokens = {"single", "gram"};
        Set<PaperPlagiarismChecker.NGramHolder> ngramSet = new HashSet<>();
        ngramSet.add(new PaperPlagiarismChecker.NGramHolder(tokens, 0, 2));  // 1个2-gram

        long simHash = PaperPlagiarismChecker.calculateSimHash(ngramSet);
        assertNotEquals(0, simHash);  // 非空集合不应返回0
    }

    @Test
    public void testCalculateSimHash_SimilarTexts() {
        // 相似文本应产生相似的SimHash（海明距离小）
        String[] tokens1 = {"this", "is", "a", "test"};
        String[] tokens2 = {"this", "is", "a", "test", "case"};  // 仅多一个词

        Set<PaperPlagiarismChecker.NGramHolder> set1 = PaperPlagiarismChecker.extractNgramSet(tokens1, 2);
        Set<PaperPlagiarismChecker.NGramHolder> set2 = PaperPlagiarismChecker.extractNgramSet(tokens2, 2);

        long hash1 = PaperPlagiarismChecker.calculateSimHash(set1);
        long hash2 = PaperPlagiarismChecker.calculateSimHash(set2);

        int distance = PaperPlagiarismChecker.calculateHammingDistance(hash1, hash2);
        assertTrue("相似文本SimHash差异过大", distance < 10);  // 预期差异较小
    }

    // ====================== 测试 calculateHammingDistance() 函数 ======================
    @Test
    public void testCalculateHammingDistance_IdenticalHashes() {
        // 相同哈希的海明距离应为0
        long hash = 0x12345678L;
        assertEquals(0, PaperPlagiarismChecker.calculateHammingDistance(hash, hash));
    }

    @Test
    public void testCalculateHammingDistance_KnownDifferences() {
        // 已知差异位的海明距离计算
        long hash1 = 0b0000L;  // 二进制4位全0
        long hash2 = 0b1111L;  // 二进制4位全1
        assertEquals(4, PaperPlagiarismChecker.calculateHammingDistance(hash1, hash2));

        long hash3 = 0b1010L;
        long hash4 = 0b1001L;
        assertEquals(2, PaperPlagiarismChecker.calculateHammingDistance(hash3, hash4));  // 第2和第4位不同
    }

    @Test
    public void testCalculateHammingDistance_EdgeValues() {
        long hash1 = 0L;
        long hash2 = -1L; // -1 的二进制是 64 个 1
        int distance = PaperPlagiarismChecker.calculateHammingDistance(hash1, hash2);
        assertEquals(64, distance);
    }

    // ====================== 测试 calculateSimHashSimilarity() 函数 ======================
    @Test
    public void testCalculateSimHashSimilarity_KnownValues() {
        // 已知海明距离对应的相似度
        assertEquals(1.0, PaperPlagiarismChecker.calculateSimHashSimilarity(0), 0.001);    // 0位不同
        assertEquals(0.75, PaperPlagiarismChecker.calculateSimHashSimilarity(16), 0.001);  // 16位不同
        assertEquals(0.5, PaperPlagiarismChecker.calculateSimHashSimilarity(32), 0.001);   // 32位不同
        assertEquals(0.0, PaperPlagiarismChecker.calculateSimHashSimilarity(64), 0.001);   // 64位不同
    }

    @Test
    public void testCalculateSimHashSimilarity_BoundaryChecks() {
        // 边界值校验（海明距离不能超过64或为负）
        assertEquals(0.0, PaperPlagiarismChecker.calculateSimHashSimilarity(65), 0.001);  // 超过64位按64算
        assertEquals(1.0, PaperPlagiarismChecker.calculateSimHashSimilarity(-1), 0.001);  // 负数按0算
    }
}
    