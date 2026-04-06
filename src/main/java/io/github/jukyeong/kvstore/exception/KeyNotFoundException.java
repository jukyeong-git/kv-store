package io.github.jukyeong.kvstore.exception;

public class KeyNotFoundException extends RuntimeException {

    // Thrown when the requested key does not exist in the store
    public KeyNotFoundException(String key) {
        super("Key not found: " + key);
    }
}
