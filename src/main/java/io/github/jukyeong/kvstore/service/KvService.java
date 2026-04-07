package io.github.jukyeong.kvstore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jukyeong.kvstore.dto.KvRequest;
import io.github.jukyeong.kvstore.dto.KvResponse;
import io.github.jukyeong.kvstore.entity.KvStore;
import io.github.jukyeong.kvstore.exception.KeyNotFoundException;
import io.github.jukyeong.kvstore.repository.KvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.naming.ServiceUnavailableException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KvService {

    private final KvRepository kvRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis lock key prefix
    private static final String LOCK_PREFIX = "lock:";

    // Lock expiration time in seconds
    private static final long LOCK_TIMEOUT = 10L;
    private static final int MAX_RETRIES = 50;
    private static final long RETRY_INTERVAL = 100L;

    // Save a key-value pair, attempting Redis lock first
    // Falls back to DB-level pessimistic lock if Redis is unavailable
    public KvResponse save(KvRequest request) {
        String key = request.getKey();
        String value = serializeValue(request.getValue());

        try {
            return saveWithRedisLock(key, value);
        } catch (RedisConnectionFailureException | QueryTimeoutException | RedisSystemException e) {
            // Fail-Fast strategy to protect the database
            log.error("CRITICAL: Redis is unavailable. Halting save operation to prevent Database overload. Key: {}", key);

            // Throw Spring's built-in ResponseStatusException with 503 status
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The server is temporarily unavailable. Please try again later.");
        }
    }

    // Acquire a distributed Redis lock before writing to the DB
    // Ensures only one thread increments the version at a time
    private KvResponse saveWithRedisLock(String key, String value) {
        String lockKey = LOCK_PREFIX + key;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TIMEOUT, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(locked)) {
                try {
                    return saveInternal(key, value, false);
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }

            // Log retry attempt
            log.debug("Lock acquisition failed for key: {}, attempt: {}/{}",
                    key, attempt, MAX_RETRIES);

            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for lock");
            }
        }

        throw new RuntimeException(
                "Failed to acquire lock after " + MAX_RETRIES + " attempts for key: " + key
        );
    }

    // Fallback: use DB-level pessimistic lock when Redis is down
    @Transactional
    public KvResponse saveWithDbLock(String key, String value) {
        return saveInternal(key, value, true);
    }

    // Core save logic: determine next version and persist the entry
    @Transactional
    public KvResponse saveInternal(String key, String value, boolean withLock) {
        Optional<KvStore> latest = withLock
                ? kvRepository.findLatestByKeyWithLock(key)
                : kvRepository.findLatestByKey(key);

        // Start at version 1 if key does not exist, otherwise increment by 1
        int nextVersion = latest.map(k -> k.getVersion() + 1).orElse(1);

        KvStore entry = KvStore.builder()
                .key(key)
                .value(value)
                .version(nextVersion)
                .createdAt(Instant.now().getEpochSecond())
                .build();

        KvStore saved = kvRepository.save(entry);
        return toResponse(saved);
    }

    // Retrieve the latest version of the given key
    @Transactional(readOnly = true)
    public KvResponse getLatest(String key) {
        return kvRepository.findLatestByKey(key)
                .map(this::toResponse)
                .orElseThrow(() -> new KeyNotFoundException(key));
    }

    // Retrieve the value of the given key at a specific UNIX timestamp
    @Transactional(readOnly = true)
    public KvResponse getByTimestamp(String key, Long timestamp) {
        return kvRepository.findByKeyAndTimestamp(key, timestamp)
                .map(this::toResponse)
                .orElseThrow(() -> new KeyNotFoundException(key));
    }

    // Retrieve the latest version of all stored keys
    @Transactional(readOnly = true)
    public List<KvResponse> getAll() {
        return kvRepository.findAllLatest()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Serialize the value object to a JSON string for storage
    private String serializeValue(Object value) {
        try {
            if (value instanceof String) {
                return (String) value;  // Store plain string without JSON serialization
            }
            return objectMapper.writeValueAsString(value);  // Serialize JSON object to string
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize value", e);
        }
    }

    // Deserialize the stored JSON string back to an object
    // Returns the raw string if deserialization fails
    private Object deserializeValue(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException e) {
            return value;
        }
    }

    // Convert a KvStore entity to a KvResponse DTO
    private KvResponse toResponse(KvStore entity) {
        return KvResponse.builder()
                .key(entity.getKey())
                .value(deserializeValue(entity.getValue()))
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
