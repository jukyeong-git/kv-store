package io.github.jukyeong.kvstore.repository;

import io.github.jukyeong.kvstore.entity.KvStore;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KvRepository extends JpaRepository<KvStore, Long> {

    // Retrieve the latest version of a key without locking
    @Query("SELECT k FROM KvStore k WHERE k.key = :key ORDER BY k.version DESC LIMIT 1")
    Optional<KvStore> findLatestByKey(@Param("key") String key);

    // Retrieve the latest version of a key with a pessimistic write lock
    // Used as a fallback when Redis is unavailable
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM KvStore k WHERE k.key = :key ORDER BY k.version DESC LIMIT 1")
    Optional<KvStore> findLatestByKeyWithLock(@Param("key") String key);

    // Retrieve the value of a key at a specific point in time using UNIX timestamp
    @Query("""
        SELECT k FROM KvStore k
        WHERE k.key = :key
          AND k.createdAt <= :timestamp
        ORDER BY k.createdAt DESC
        LIMIT 1
        """)
    Optional<KvStore> findByKeyAndTimestamp(
            @Param("key") String key,
            @Param("timestamp") Long timestamp
    );

    // Retrieve the latest version of all keys
    @Query("""
        SELECT k FROM KvStore k
        WHERE k.version = (
            SELECT MAX(k2.version)
            FROM KvStore k2
            WHERE k2.key = k.key
        )
        ORDER BY k.key
        """)
    List<KvStore> findAllLatest();
}
