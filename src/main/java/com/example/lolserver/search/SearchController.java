package com.example.lolserver.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class SearchController {

    @GetMapping("/")
    Flux<String> search() {
        return Flux.just("Hello", "World");
    }
}
