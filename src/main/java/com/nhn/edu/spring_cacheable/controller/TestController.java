package com.nhn.edu.spring_cacheable.controller;

import com.nhn.edu.spring_cacheable.service.CachedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    private final CachedService cachedService;


    public TestController(CachedService cachedService) {
        this.cachedService = cachedService;
    }


    @GetMapping("/test")
    public String test() {
        return cachedService.getValue("1");
    }

}
