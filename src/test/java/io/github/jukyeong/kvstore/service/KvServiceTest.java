package io.github.jukyeong.kvstore.service;

import io.github.jukyeong.kvstore.dto.KvRequest;
import io.github.jukyeong.kvstore.dto.KvResponse;
import io.github.jukyeong.kvstore.exception.KeyNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class KvServiceTest {

    @Autowired
    private KvService kvService;

    // Helper to create KvRequest via reflection
    private KvRequest request(String key, Object value) {
        try {
            KvRequest req = new KvRequest();
            var keyField = KvRequest.class.getDeclaredField("key");
            var valueField = KvRequest.class.getDeclaredField("value");
            keyField.setAccessible(true);
            valueField.setAccessible(true);
            keyField.set(req, key);
            valueField.set(req, value);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Basic Version Tests ───────────────────────────

    @Test
    @DisplayName("First save should create version 1")
    void save_firstTime_versionOne() {
        KvResponse response = kvService.save(request("testkey1", "value1"));
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Saving the same key should increment version by 1")
    void save_sameKey_versionIncrement() {
        kvService.save(request("testkey2", "value1"));
        KvResponse response = kvService.save(request("testkey2", "value2"));
        assertThat(response.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Different keys should have independent versions")
    void save_differentKeys_independentVersions() {
        KvResponse r1 = kvService.save(request("key1", "value1"));
        KvResponse r2 = kvService.save(request("key2", "value1"));
        assertThat(r1.getVersion()).isEqualTo(1);
        assertThat(r2.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Saving same key 100 times should result in version 100")
    void save_100Times_version100() {
        for (int i = 1; i <= 100; i++) {
            kvService.save(request("key100", "value" + i));
        }
        KvResponse response = kvService.getLatest("key100");
        assertThat(response.getVersion()).isEqualTo(100);
    }

    @Test
    @DisplayName("Saving same value repeatedly should still increment version")
    void save_sameValueRepeatedly_versionIncrement() {
        kvService.save(request("repeatkey", "same-value"));
        kvService.save(request("repeatkey", "same-value"));
        kvService.save(request("repeatkey", "same-value"));
        KvResponse response = kvService.getLatest("repeatkey");
        assertThat(response.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("Version numbers should be sequential with no gaps")
    void save_versions_areSequential() {
        String key = "seqkey";
        List<Integer> versions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            KvResponse r = kvService.save(request(key, "value" + i));
            versions.add(r.getVersion());
        }
        for (int i = 0; i < 10; i++) {
            assertThat(versions.get(i)).isEqualTo(i + 1);
        }
    }

    // ── GetLatest Tests ───────────────────────────────

    @Test
    @DisplayName("getLatest should return the most recent version")
    void getLatest_returnsLatestVersion() {
        kvService.save(request("testkey3", "value1"));
        kvService.save(request("testkey3", "value2"));
        kvService.save(request("testkey3", "value3"));
        KvResponse response = kvService.getLatest("testkey3");
        assertThat(response.getVersion()).isEqualTo(3);
        assertThat(response.getValue().toString()).contains("value3");
    }

    @Test
    @DisplayName("getLatest should throw KeyNotFoundException for unknown key")
    void getLatest_keyNotFound_throwsException() {
        assertThatThrownBy(() -> kvService.getLatest("nonexistent"))
                .isInstanceOf(KeyNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    // ── Timestamp Tests ───────────────────────────────

    @Test
    @DisplayName("getByTimestamp should return value at the given point in time")
    void getByTimestamp_returnsCorrectVersion() throws InterruptedException {
        kvService.save(request("tskey", "value1"));
        long timestamp = Instant.now().getEpochSecond();
        Thread.sleep(1000);
        kvService.save(request("tskey", "value2"));

        KvResponse response = kvService.getByTimestamp("tskey", timestamp);
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getValue().toString()).contains("value1");
    }

    @Test
    @DisplayName("getByTimestamp with exact createdAt should return that version")
    void getByTimestamp_exactCreatedAt_returnsCorrectVersion() {
        KvResponse saved = kvService.save(request("exactkey", "value1"));
        KvResponse response = kvService.getByTimestamp("exactkey", saved.getCreatedAt());
        assertThat(response.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("getByTimestamp with future timestamp should return latest version")
    void getByTimestamp_futureTimestamp_returnsLatest() {
        kvService.save(request("futurekey", "value1"));
        long futureTimestamp = Instant.now().getEpochSecond() + 99999;
        KvResponse response = kvService.getByTimestamp("futurekey", futureTimestamp);
        assertThat(response.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("getByTimestamp before any record should throw KeyNotFoundException")
    void getByTimestamp_beforeAnyRecord_throwsException() {
        kvService.save(request("beforekey", "value1"));
        assertThatThrownBy(() -> kvService.getByTimestamp("beforekey", 1000L))
                .isInstanceOf(KeyNotFoundException.class);
    }

    @Test
    @DisplayName("timestamp=0 should throw KeyNotFoundException")
    void getByTimestamp_zeroTimestamp_throwsException() {
        kvService.save(request("zerotskey", "value1"));
        assertThatThrownBy(() -> kvService.getByTimestamp("zerotskey", 0L))
                .isInstanceOf(KeyNotFoundException.class);
    }

    @Test
    @DisplayName("Negative timestamp should throw KeyNotFoundException")
    void getByTimestamp_negativeTimestamp_throwsException() {
        kvService.save(request("negtskey", "value1"));
        assertThatThrownBy(() -> kvService.getByTimestamp("negtskey", -1L))
                .isInstanceOf(KeyNotFoundException.class);
    }

    @Test
    @DisplayName("Max UNIX timestamp should return latest version")
    void getByTimestamp_maxTimestamp_returnsLatest() {
        kvService.save(request("maxtskey", "value1"));
        KvResponse response = kvService.getByTimestamp("maxtskey", Long.MAX_VALUE);
        assertThat(response.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("createdAt should be monotonically increasing")
    void save_createdAt_isMonotonicallyIncreasing() throws InterruptedException {
        KvResponse r1 = kvService.save(request("monokey", "value1"));
        Thread.sleep(1000);
        KvResponse r2 = kvService.save(request("monokey", "value2"));
        assertThat(r2.getCreatedAt()).isGreaterThanOrEqualTo(r1.getCreatedAt());
    }

    // ── GetAll Tests ──────────────────────────────────

    @Test
    @DisplayName("getAll with no records should return empty list")
    void getAll_noRecords_returnsEmptyList() {
        List<KvResponse> all = kvService.getAll();
        assertThat(all).isEmpty();
    }

    @Test
    @DisplayName("getAll should return only latest version per key")
    void getAll_multipleVersions_returnsOnlyLatest() {
        kvService.save(request("k1", "v1"));
        kvService.save(request("k1", "v2"));
        kvService.save(request("k1", "v3"));
        kvService.save(request("k2", "v1"));
        kvService.save(request("k2", "v2"));

        List<KvResponse> all = kvService.getAll();
        assertThat(all).hasSize(2);
        assertThat(all.stream()
                .filter(r -> r.getKey().equals("k1"))
                .findFirst().get().getVersion()).isEqualTo(3);
        assertThat(all.stream()
                .filter(r -> r.getKey().equals("k2"))
                .findFirst().get().getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("All versions should be preserved after multiple updates")
    void save_multipleUpdates_allVersionsPreserved() {
        kvService.save(request("historykey", "value1"));
        kvService.save(request("historykey", "value2"));
        kvService.save(request("historykey", "value3"));
        KvResponse latest = kvService.getLatest("historykey");
        assertThat(latest.getVersion()).isEqualTo(3);
    }

    // ── Value Type Tests ──────────────────────────────

    @Test
    @DisplayName("JSON object value should be stored correctly")
    void save_jsonObjectValue_storedCorrectly() {
        kvService.save(request("jsonkey", Map.of("name", "ryan", "age", 30)));
        KvResponse response = kvService.getLatest("jsonkey");
        assertThat(response.getValue().toString()).contains("ryan");
    }

    @Test
    @DisplayName("Number value should be stored correctly")
    void save_numberValue_storedCorrectly() {
        kvService.save(request("numkey", 12345));
        KvResponse response = kvService.getLatest("numkey");
        assertThat(response.getValue().toString()).contains("12345");
    }

    @Test
    @DisplayName("Float value should be stored correctly")
    void save_floatValue_storedCorrectly() {
        kvService.save(request("floatkey", 3.14159));
        KvResponse response = kvService.getLatest("floatkey");
        assertThat(response.getValue().toString()).contains("3.14");
    }

    @Test
    @DisplayName("Boolean value should be stored correctly")
    void save_booleanValue_storedCorrectly() {
        kvService.save(request("boolkey", true));
        KvResponse response = kvService.getLatest("boolkey");
        assertThat(response.getValue().toString()).contains("true");
    }

    @Test
    @DisplayName("Array value should be stored correctly")
    void save_arrayValue_storedCorrectly() {
        kvService.save(request("arraykey", List.of("a", "b", "c")));
        KvResponse response = kvService.getLatest("arraykey");
        assertThat(response.getValue().toString()).contains("a");
    }

    @Test
    @DisplayName("Array of objects should be stored correctly")
    void save_arrayOfObjects_storedCorrectly() {
        List<Map<String, Object>> arrayOfObjects = List.of(
                Map.of("id", 1, "name", "item1"),
                Map.of("id", 2, "name", "item2")
        );
        kvService.save(request("arrayobjkey", arrayOfObjects));
        KvResponse response = kvService.getLatest("arrayobjkey");
        assertThat(response.getValue().toString()).contains("item1");
    }

    @Test
    @DisplayName("Deeply nested JSON should be stored correctly")
    void save_deeplyNestedJson_storedCorrectly() {
        Map<String, Object> deep = Map.of(
                "level1", Map.of(
                        "level2", Map.of(
                                "level3", Map.of("level4", "deepValue")
                        )
                )
        );
        kvService.save(request("deepkey", deep));
        KvResponse response = kvService.getLatest("deepkey");
        assertThat(response.getValue().toString()).contains("deepValue");
    }

    @Test
    @DisplayName("Null value in JSON object should be stored correctly")
    void save_nullValueInJson_storedCorrectly() {
        Map<String, Object> valueWithNull = new HashMap<>();
        valueWithNull.put("name", "ryan");
        valueWithNull.put("address", null);
        kvService.save(request("nullvalkey", valueWithNull));
        KvResponse response = kvService.getLatest("nullvalkey");
        assertThat(response.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Empty string value should be stored correctly")
    void save_emptyStringValue_storedCorrectly() {
        kvService.save(request("emptykey", ""));
        KvResponse response = kvService.getLatest("emptykey");
        assertThat(response.getValue().toString()).isEmpty();
    }

    @Test
    @DisplayName("Unicode value should be stored correctly")
    void save_unicodeValue_storedCorrectly() {
        kvService.save(request("unicodekey", "안녕하세요 🎉"));
        KvResponse response = kvService.getLatest("unicodekey");
        assertThat(response.getValue().toString()).contains("안녕하세요");
    }

    // ── Key Edge Cases ────────────────────────────────

    @Test
    @DisplayName("Key with maximum length (255 chars) should be stored correctly")
    void save_maxLengthKey_storedCorrectly() {
        String maxKey = "a".repeat(255);
        kvService.save(request(maxKey, "value1"));
        KvResponse response = kvService.getLatest(maxKey);
        assertThat(response.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Key with special characters should be stored correctly")
    void save_keyWithSpecialChars_storedCorrectly() {
        kvService.save(request("special-key_123", "value1"));
        KvResponse response = kvService.getLatest("special-key_123");
        assertThat(response.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Unicode key should be stored correctly")
    void save_unicodeKey_storedCorrectly() {
        kvService.save(request("키값", "value1"));
        KvResponse response = kvService.getLatest("키값");
        assertThat(response.getVersion()).isEqualTo(1);
    }

    // ── Response Structure Tests ──────────────────────

    @Test
    @DisplayName("Response should contain createdAt timestamp")
    void save_response_containsCreatedAt() {
        KvResponse response = kvService.save(request("tskey3", "value1"));
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getCreatedAt()).isGreaterThan(0);
    }

    @Test
    @DisplayName("createdAtFormatted should be in ISO 8601 format")
    void save_response_createdAtFormattedIsISO8601() {
        KvResponse response = kvService.save(request("isokey", "value1"));
        assertThat(response.getCreatedAtFormatted())
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    }

    @Test
    @DisplayName("Response key should match request key exactly")
    void save_response_keyMatchesRequest() {
        KvResponse response = kvService.save(request("exactkey2", "value1"));
        assertThat(response.getKey()).isEqualTo("exactkey2");
    }

    // ── Security Tests ────────────────────────────────

    @Test
    @DisplayName("SQL injection attempt in key should be handled safely")
    void save_sqlInjectionKey_handledSafely() {
        String maliciousKey = "'; DROP TABLE kv_store; --";
        assertThatCode(() -> {
            kvService.save(request(maliciousKey, "value1"));
            kvService.getLatest(maliciousKey);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("XSS attempt in value should be stored as-is")
    void save_xssValue_storedAsIs() {
        String xssValue = "<script>alert('xss')</script>";
        kvService.save(request("xsskey", xssValue));
        KvResponse response = kvService.getLatest("xsskey");
        assertThat(response.getValue().toString()).contains("script");
    }

    @Test
    @DisplayName("Very large JSON value should be handled gracefully")
    void save_largeJsonValue_handledGracefully() {
        Map<String, String> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, "value".repeat(20));
        }
        assertThatCode(() -> {
            kvService.save(request("largekey", largeMap));
            kvService.getLatest("largekey");
        }).doesNotThrowAnyException();
    }

    // ── Concurrency Tests ─────────────────────────────

    @Test
    @DisplayName("50 concurrent requests should result in exactly versions 1 through 51")
    void save_concurrent50_versionsCorrect() throws InterruptedException {
        String key = "concurrentKey";
        kvService.save(request(key, "initial"));

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Integer> versions = new CopyOnWriteArrayList<>();
        List<Exception> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    KvResponse res = kvService.save(request(key, "value" + idx));
                    versions.add(res.getVersion());
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors).isEmpty();
        assertThat(versions).hasSize(50);
        KvResponse latest = kvService.getLatest(key);
        assertThat(latest.getVersion()).isEqualTo(51);
    }

    @Test
    @DisplayName("Multiple keys updated concurrently should not interfere")
    void save_multipleKeysConcurrently_noInterference()
            throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String key = "concKey" + i;
            executor.submit(() -> {
                try {
                    kvService.save(request(key, "value1"));
                    kvService.save(request(key, "value2"));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        for (int i = 0; i < threadCount; i++) {
            KvResponse response = kvService.getLatest("concKey" + i);
            assertThat(response.getVersion()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Concurrent reads and writes should not cause data corruption")
    void save_concurrentReadsAndWrites_noCorruption()
            throws InterruptedException {
        String key = "rwkey";
        kvService.save(request(key, "initial"));

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    if (idx % 2 == 0) {
                        kvService.save(request(key, "value" + idx));
                    } else {
                        kvService.getLatest(key);
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(errors).isEmpty();
    }

    // ── Worst Case Tests ──────────────────────────────

    @Test
    @DisplayName("Redis unavailable should fall back to DB lock successfully")
    void save_redisUnavailable_fallsBackToDbLock() {
        assertThatCode(() -> {
            kvService.saveWithDbLock("fallbackkey", "value1");
        }).doesNotThrowAnyException();

        KvResponse response = kvService.getLatest("fallbackkey");
        assertThat(response.getVersion()).isEqualTo(1);
    }
}