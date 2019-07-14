package com.nhn.edu.spring_cacheable.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeavyService {
    public String veryHeavyMethod(String paramId) {
        log.error("CALLED with paramId: {}", paramId);
        return "very very heavy with " + paramId;
    }

}
