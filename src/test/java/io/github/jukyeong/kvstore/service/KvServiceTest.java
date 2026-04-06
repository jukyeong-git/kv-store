package io.github.jukyeong.kvstore.service;

import io.github.jukyeong.kvstore.dto.KvRequest;
import io.github.jukyeong.kvstore.dto.KvResponse;
import io.github.jukyeong.kvstore.exception.KeyNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class KvServiceTest {

    @Autowired
    private KvService kvService;

    // Helper to create a KvRequest via reflection (no public constructor)
    private KvRequest request(String key, String value) {
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

    @Test
    @DisplayName("First save should create version 1")
    void save_firstTime_versionOne() {
        KvResponse response = kvService.save(request("testkey1", "value1"));
        assertThat(response.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Saving the same key twice should increment the version to 2")
    void save_sameKey_versionIncrement() {
        kvService.save(request("testkey2", "value1"));
        KvResponse response = kvService.save(request("testkey2", "value2"));
        assertThat(response.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("getLatest should return the most recent version")
    void getLatest_returnsLatestValue() {
        kvService.save(request("testkey3", "value1"));
        kvService.save(request("testkey3", "value2"));
        KvResponse response = kvService.getLatest("testkey3");
        assertThat(response.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("getLatest should throw KeyNotFoundException for unknown key")
    void getLatest_keyNotFound_throwsException() {
        assertThatThrownBy(() -> kvService.getLatest("nonexistent"))
                .isInstanceOf(KeyNotFoundException.class);
    }

    @Test
    @DisplayName("50 concurrent requests should result in versions 1 through 51")
    void save_concurrent50_versionsCorrect() throws InterruptedException {
        String key = "concurrentKey";

        // Save the initial entry as version 1
        kvService.save(request(key, "initial"));

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Integer> versions = new CopyOnWriteArrayList<>();

        // Submit 50 concurrent save requests
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    KvResponse res = kvService.save(request(key, "value" + idx));
                    versions.add(res.getVersion());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // All 50 concurrent requests must have been saved
        assertThat(versions).hasSize(50);

        // The latest version must be exactly 51
        KvResponse latest = kvService.getLatest(key);
        assertThat(latest.getVersion()).isEqualTo(51);
    }
}
