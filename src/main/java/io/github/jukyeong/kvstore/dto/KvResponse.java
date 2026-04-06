package io.github.jukyeong.kvstore.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class KvResponse {

    // The key of the stored entry
    private String key;

    // The deserialized value
    private Object value;

    // The version number of this entry
    private Integer version;

    // UNIX timestamp (UTC) when this version was created
    private Long createdAt;

    // Human-readable format
    public String getCreatedAtFormatted() {
        if (createdAt == null) return null;
        return Instant.ofEpochSecond(createdAt)
                .toString();  // 2025-07-06T18:00:00Z
    }
}
