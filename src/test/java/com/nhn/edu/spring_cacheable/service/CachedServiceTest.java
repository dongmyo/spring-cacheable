package com.nhn.edu.spring_cacheable.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CachedServiceTest {
    @Autowired
    private CachedService cachedService;

    @MockBean
    private HeavyService heavyService;


    @Test
    public void testGetValue() throws Exception {
        given(heavyService.veryHeavyMethod(anyString())).willReturn("mocked value");

        IntStream.range(1, 10).forEach(i -> cachedService.getValue("1"));

        then(heavyService).should().veryHeavyMethod("1");
    }

}
