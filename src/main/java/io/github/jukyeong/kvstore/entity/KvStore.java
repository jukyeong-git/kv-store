package io.github.jukyeong.kvstore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "kv_store",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_key_version",
                        columnNames = {"kv_key", "version"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KvStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The key of the key-value pair
    @Column(name = "kv_key", nullable = false, length = 255)
    private String key;

    // The value stored as a JSON string
    @Column(name = "kv_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    // Version number starting from 1, incremented on each update
    @Column(name = "version", nullable = false)
    private Integer version;

    // UNIX timestamp in UTC when this version was created
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
}