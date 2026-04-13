package io.github.jukyeong.kvstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KvStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(KvStoreApplication.class, args);
    }

}
