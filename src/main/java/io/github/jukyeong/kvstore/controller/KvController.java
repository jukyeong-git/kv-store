package io.github.jukyeong.kvstore.controller;

import io.github.jukyeong.kvstore.dto.KvRequest;
import io.github.jukyeong.kvstore.dto.KvResponse;
import io.github.jukyeong.kvstore.service.KvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "KV Store", description = "Version-controlled Key-Value Store API")
@RestController
@RequestMapping("/object")
@RequiredArgsConstructor
public class KvController {

    private final KvService kvService;

    // Store a key-value pair
    // Creates version 1 if key does not exist, otherwise increments by 1
    @Operation(
            summary = "Save a key-value pair",
            description = "Creates version 1 for a new key, or increments the version by 1 for an existing key"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<KvResponse> save(
            @Valid @RequestBody KvRequest request) {
        return ResponseEntity.ok(kvService.save(request));
    }

    // Retrieve the value of a key
    // Returns the latest version if no timestamp is provided
    // Returns the value at the given UNIX timestamp if provided
    @Operation(
            summary = "Retrieve a value by key",
            description = "Returns the latest value if no timestamp is given, " +
                    "or the value at the specified UNIX timestamp (UTC)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    @GetMapping("/{key}")
    public ResponseEntity<KvResponse> get(
            @Parameter(description = "The key to look up")
            @PathVariable String key,
            @Parameter(description = "UNIX timestamp in UTC. If provided, returns the value at that point in time")
            @RequestParam(required = false) Long timestamp) {

        KvResponse response = (timestamp != null)
                ? kvService.getByTimestamp(key, timestamp)
                : kvService.getLatest(key);

        return ResponseEntity.ok(response);
    }

    // Retrieve the latest version of all stored keys
    @Operation(
            summary = "Retrieve all records",
            description = "Returns the latest version of every key currently stored in the database"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all records")
    @GetMapping("/get_all_records")
    public ResponseEntity<List<KvResponse>> getAll() {
        return ResponseEntity.ok(kvService.getAll());
    }
}
