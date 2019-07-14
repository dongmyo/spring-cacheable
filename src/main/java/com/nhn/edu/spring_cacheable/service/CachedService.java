package com.nhn.edu.spring_cacheable.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CachedService {
    @Autowired
    private HeavyService heavyService;


    @Cacheable(cacheNames = "cac", key = "#paramId")
    public String getValue(String paramId) {
        return heavyService.veryHeavyMethod(paramId);
    }

}
