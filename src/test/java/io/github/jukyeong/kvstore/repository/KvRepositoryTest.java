package io.github.jukyeong.kvstore.repository;

import io.github.jukyeong.kvstore.entity.KvStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("local")
class KvRepositoryTest {

    @Autowired
    private KvRepository kvRepository;

    private KvStore createEntry(String key, String value,
                                int version, long createdAt) {
        return KvStore.builder()
                .key(key)
                .value(value)
                .version(version)
                .createdAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("findLatestByKey should return highest version")
    void findLatestByKey_returnsHighestVersion() {
        kvRepository.save(createEntry("k1", "v1", 1, 1000L));
        kvRepository.save(createEntry("k1", "v2", 2, 2000L));
        kvRepository.save(createEntry("k1", "v3", 3, 3000L));

        Optional<KvStore> result = kvRepository.findLatestByKey("k1");
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("findLatestByKey should return empty for unknown key")
    void findLatestByKey_unknownKey_returnsEmpty() {
        Optional<KvStore> result = kvRepository.findLatestByKey("unknown");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByKeyAndTimestamp should return correct version")
    void findByKeyAndTimestamp_returnsCorrectVersion() {
        kvRepository.save(createEntry("k2", "v1", 1, 1000L));
        kvRepository.save(createEntry("k2", "v2", 2, 2000L));
        kvRepository.save(createEntry("k2", "v3", 3, 3000L));

        Optional<KvStore> result =
                kvRepository.findByKeyAndTimestamp("k2", 1500L);
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByKeyAndTimestamp with exact timestamp should return that version")
    void findByKeyAndTimestamp_exactTimestamp_returnsCorrectVersion() {
        kvRepository.save(createEntry("k3", "v1", 1, 1000L));
        kvRepository.save(createEntry("k3", "v2", 2, 2000L));

        Optional<KvStore> result =
                kvRepository.findByKeyAndTimestamp("k3", 2000L);
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllLatest should return only latest version per key")
    void findAllLatest_returnsLatestPerKey() {
        kvRepository.save(createEntry("k4", "v1", 1, 1000L));
        kvRepository.save(createEntry("k4", "v2", 2, 2000L));
        kvRepository.save(createEntry("k5", "v1", 1, 1000L));

        List<KvStore> result = kvRepository.findAllLatest();
        assertThat(result).hasSize(2);
        assertThat(result.stream()
                .filter(k -> k.getKey().equals("k4"))
                .findFirst().get().getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Duplicate key+version should throw exception")
    void save_duplicateKeyVersion_throwsException() {
        kvRepository.save(createEntry("k6", "v1", 1, 1000L));
        assertThatThrownBy(() ->
                kvRepository.saveAndFlush(createEntry("k6", "v2", 1, 2000L))
        ).isInstanceOf(Exception.class);
    }
}
