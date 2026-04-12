package io.github.jukyeong.kvstore.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Hidden
@RestController
public class RootController {

    @GetMapping("/")
    public void redirect(HttpServletResponse response)
            throws IOException {
        response.sendRedirect("https://jukyeong-git.github.io");
    }
}
