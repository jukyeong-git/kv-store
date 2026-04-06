package io.github.jukyeong.kvstore.dto;

import lombok.Builder;
import lombok.Getter;

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
}
