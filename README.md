 Spring에서 `@Cacheable` 어노테이션이 설정된 메서드의 캐싱 동작 유무를 테스트하는 방법에 대해 공유해보고자 합니다.


## @Cacheable 적용 상황

 먼저, **HeavyService**라는 컴포넌트가 있다고 하죠.
이 **HeavyService**의 **veryHeavyMethod()** 를 호출하면
굉장히 무거운 작업을 수행하기 때문에 - 데이터베이스에서 부하가 큰 쿼리를 수행하거나 또는 응답 시간이 굉장히 오래 걸리는 다른 API 호출하거나 하는 등등 -
그 결과를 캐싱해서 사용하고자 합니다.

```java
@Slf4j
@Service
public class HeavyService {
    String veryHeavyMethod(String paramId) {
        log.error("Very Heavy Method is called with paramId: {}", paramId);
        return "2 heavy 2 handle with parameter " + paramId;
    }
}
```

 그래서, 아래와 같이 **HeavyService**를 가지면서 `@Cacheable`을 적용하는 **CacheService**를 만들었습니다.

```java
@Service
public class CacheService {
    private final HeavyService heavyService;

    public CacheService(HeavyService heavyService) {
        this.heavyService = heavyService;
    }

    @Cacheable(value = "acl", key = "#paramId", cacheManager = "aclCacheManager")
    public String cachedVeryHeadyMethod(String paramId) {
        return heavyService.veryHeavyMethod(paramId);
    }
}
```

 이제, **CacheService.cachedVeryHeadyMethod()** 메서드를 호출하면
스프링에서 `@Cacheable` 어노테이션 설정에 따라 캐싱이 적용되어
최초 호출 시에만 **HeavyService.veryHeavyMethod()** 메서드가 실행되고
이후로는 캐싱된 결과가 반환이 될 것입니다.


## 실제 `@Cacheable` 메서드 결과의 캐싱은 누가 처리하나?

 그렇다면 `@Cacheable` 어노테이션이 붙은 메서드 결과를 실제 캐싱 처리하는 것은 누구일까요?
잘 아시다시피 Spring에서는 AOP Proxy를 통해 `@Cacheable`이나 `@Transactional`과 같은 어노테이션을 처리합니다.

 즉, `@Cacheable` 어노테이션이 붙은 메서드는 실제 메서드가 실행되기에 앞서
Spring에서 생성한 Proxy 객체가 요청된 메서드의 결과가 캐싱이 되었는지 여부를 판단해서
이미 캐싱된 경우라면 해당 메서드를 호출하지 않고 캐시에서 결과를 즉시 반환하고,
아직 캐싱이 되지 않은 경우라면 해당 메서드를 호출해서 그 결과를 캐싱하도록 하는 것이죠.

 정확하게는 **org.springframework.cache.interceptor.CacheInterceptor**에서 이러한 처리를 합니다.


## `@Cacheable`이 설정된 메서드의 캐싱 동작 유무는 어떻게 테스트할 수 있나?

 `@Cacheable`이 설정된 메서드는 Spring에서 Proxy 객체를 이용해서 캐싱 처리를 한다는 점에 주목하면
`@Cacheable`이 설정된 메서드의 캐싱 동작 유무에 대한 테스트 방안을 쉽게 떠올릴 수 있습니다.

 바로 `@Cacheable`이 설정된 메서드 내에서 호출되는 컴포넌트 메서드가 실제 몇 번 호출되는 지를 체크하면 되는 것이죠.

앞서 살펴봤던 예제로 말씀드리면, `@Cacheable`이 설정된 **CacheService.cachedVeryHeadyMethod()** 메서드 내에서 호출되는
**HeavyService.veryHeavyMethod()** 메서드가 실제 몇 번 호출되는 지를 확인해보면
캐싱이 정상적으로 동작하는 지 확인할 수 있습니다.

 캐싱이 정상적으로 동작하는 상황이라면
**CacheService.cachedVeryHeadyMethod()** 메서드 내에서 호출되는 **HeavyService.veryHeavyMethod()** 메서드는
최초 호출 시 한 번만 호출이 되겠죠.
왜냐하면 최초 호출 이후로는 Proxy 객체에서 캐싱된 결과를 바로 반환할테니까요.

 그런데, 만일 어떤 이유에서건 캐싱이 정상적으로 동작하지 않는 상황이라면
**CacheService.cachedVeryHeadyMethod()** 메서드 내에서 호출되는 **HeavyService.veryHeavyMethod()** 메서드는
**CacheService.cachedVeryHeadyMethod()** 메서드가 호출되는 횟수만큼 계속해서 호출되게 될 것입니다.

 그럼, 이제 이 내용을 실제 테스트 코드로 확인해봅시다.

## `@Cacheable`이 설정된 메서드 테스트 케이스

 Spring의 Application Context에 Cache 설정이 적용된 상태로 테스트 케이스를 수행해야 Spring의 Proxy 객체를 이용할 수 있을 것이므로
integration test 환경에서 테스트 케이스를 수행하도록 합시다.

 integration test 환경을 구성하려면,
Spring Boot 프로젝트라면 `@SpringBootTest`를 이용하면 될 것이고,
Spring Boot를 사용하지 않는 일반 Spring 프로젝트라면 `@ContextConfiguration`을 이용할 수 있을 것입니다.

 다음 예제 코드는 Spring Boot의 `@SpringBootTest`를 이용하였습니다.

 ```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CacheServiceTest {
    private static final String PARAM_ID = "1";

    @MockBean
    private HeavyService heavyService;

    @Autowired
    private CacheService cacheService;

    @Test
    public void testAclCacheService() throws Exception {
        given(heavyService.veryHeavyMethod(anyString())).willReturn("mock");

        IntStream.range(0, 10)
                 .forEach(i -> cacheService.cachedVeryHeadyMethod(PARAM_ID));

        then(heavyService).should().veryHeavyMethod(PARAM_ID);
    }
}
```

 이 테스트 케이스는 **CacheService.cachedVeryHeadyMethod()** 메서드를 10번 반복해서 호출할 때,
메서드 내에서 실제 호출되는 **HeavyService** 객체를 mocking해서
mockito의 `verify()`를 이용해서 실제 **HeavyService.veryHeavyMethod()** 가 1번만 수행되는 지 확인하는 내용입니다.

 캐싱이 정상 동작하는 상황이라면 이 테스트 케이스는 통과를 할 것이고, 그렇지 않은 상황이라면 이 테스트 케이스는 실패할 것입니다.
정말 테스트 케이스가 실패하는 지 확인하고 싶으시다면 아래와 같이 Cache 설정이 누락되도록 수정해서 확인해보실 수 있습니다.

```java
/**
 * Redis를 이용한 간단한 Cache 설정 예제
 * 
 * 아래 코드에서 @Configuration 부분을 주석 처리하면 캐시 설정이 동작하지 않기 때문에
 * 위에서 작성한 테스트 케이스가 실패할 것입니다.
 * 
 */
@EnableCaching(proxyTargetClass = true)
@Configuration                  // TODO : <--- 여기를 주석으로 막고 위의 테스트 케이스를 실행해보세요.
public class CacheConfig {
    @Bean(name = "aclCacheManager")
    public CacheManager cacheManager(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                                                                       .entryTtl(Duration.ofSeconds(180))
                                                                       .prefixKeysWith("acl");

        return RedisCacheManager.builder(lettuceConnectionFactory)
                                .cacheDefaults(configuration)
                                .build();
    }
}
```

## 정리하며

 지금까지 Spring에서 `@Cacheable` 어노테이션이 설정된 메서드의 캐싱 동작 유무를
`@Cacheable` 어노테이션을 처리하는 Spring AOP Proxy의 특성을 이용해서
integration test를 통해 확인하는 방법에 대해 살펴보았습니다.

 integration test는 Application Context를 로딩해서 수행되어야 하기 때문에
상대적으로 빠르게 자주 수행되어야 하는 unit test와는 구분해서 실행할 필요가 있습니다.

JUnit과 Maven을 이용하신다면 JUnit의 `@Category`를 통해 integration test들을 그룹핑해서
maven test phase가 아닌 verify phase에서 실행되도록 설정하실 수 있습니다.

