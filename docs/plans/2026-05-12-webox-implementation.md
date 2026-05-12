# WeBox Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver a Spring Boot 3.2 + Thymeleaf + H2 web app that implements the WeBox PRD end-to-end (auth, menu, cart, checkout, orders, preferences, Qwen-powered AI recommendation).

**Architecture:** Single executable Spring Boot jar, embedded Tomcat on port 8080, Thymeleaf server-side rendering, H2 in-memory DB seeded at boot, Spring Data JPA persistence, HttpSession cart, custom `HandlerInterceptor` for auth (no Spring Security), Qwen via DashScope OpenAI-compatible endpoint.

**Tech Stack:** Java 17, Maven, Spring Boot 3.2.x (`web`, `thymeleaf`, `data-jpa`, `validation`, `test`), H2, Spring `RestClient`, Jackson, JUnit 5 + MockMvc.

**Design reference:** `docs/plans/2026-05-12-webox-design.md`

**Conventions:**
- Package root: `com.webox.webox`
- Price type: `java.math.BigDecimal`
- Time type: `java.time.Instant` / `java.time.LocalDate`
- Every task ends with `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q test` (unless noted) and a commit
- Commit messages follow `<type>: <subject>` (`feat`, `test`, `chore`, `docs`, `refactor`)
- Use @superpowers:test-driven-development for every task that has tests
- Use @superpowers:verification-before-completion before declaring any task done

---

## Task 1: Bootstrap Maven project skeleton

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/webox/webox/WeboxApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/webox/webox/WeboxApplicationTests.java`
- Create: `.gitignore` (append if exists)

**Step 1: Write `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
    <relativePath/>
  </parent>
  <groupId>com.webox</groupId>
  <artifactId>webox</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>webox</name>
  <properties>
    <java.version>17</java.version>
  </properties>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
    </plugins>
  </build>
</project>
```

**Step 2: Write `WeboxApplication.java`**

```java
package com.webox.webox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WeboxApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeboxApplication.class, args);
    }
}
```

**Step 3: Write `application.yml`**

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:h2:mem:webox;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  h2:
    console:
      enabled: true
webox:
  ai:
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${DASHSCOPE_API_KEY:}
    model: qwen-plus
    timeout-ms: 20000
```

**Step 4: Write context-load smoke test**

```java
package com.webox.webox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WeboxApplicationTests {
    @Test
    void contextLoads() {}
}
```

**Step 5: Update `.gitignore`**

Append these lines if not present: `target/`, `.idea/`, `*.iml`, `.DS_Store`, `HELP.md`.

**Step 6: Run Maven test to verify scaffolding works**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q test` (if wrapper missing, first run `mvn -q -N io.takari:maven:wrapper -Dmaven=3.9.6` or `mvn -q wrapper:wrapper`).
Expected: `BUILD SUCCESS`, `contextLoads` passes.

**Step 7: Commit**

```bash
git add pom.xml src/main/java/com/webox/webox/WeboxApplication.java src/main/resources/application.yml src/test/java/com/webox/webox/WeboxApplicationTests.java .gitignore .mvn mvnw mvnw.cmd
git commit -m "chore: bootstrap spring boot skeleton"
```

---

## Task 2: JPA entities + repositories

**Files:**
- Create: `src/main/java/com/webox/webox/entity/User.java`
- Create: `src/main/java/com/webox/webox/entity/MenuItem.java`
- Create: `src/main/java/com/webox/webox/entity/UserPreference.java`
- Create: `src/main/java/com/webox/webox/entity/Order.java`
- Create: `src/main/java/com/webox/webox/entity/OrderItem.java`
- Create: `src/main/java/com/webox/webox/repository/UserRepository.java`
- Create: `src/main/java/com/webox/webox/repository/MenuItemRepository.java`
- Create: `src/main/java/com/webox/webox/repository/UserPreferenceRepository.java`
- Create: `src/main/java/com/webox/webox/repository/OrderRepository.java`
- Create: `src/test/java/com/webox/webox/repository/MenuItemRepositoryTest.java`

**Step 1: Write entities**

Each entity mirrors design §4 (id auto; `User.email` unique; `MenuItem.code` unique; `Order` has `@OneToMany(mappedBy="orderId")` — simplest approach: store `orderId` on `OrderItem` and configure `@OneToMany(..., cascade=ALL, orphanRemoval=true)` via `@JoinColumn(name="order_id")`).

Concrete guidance:
- Use plain fields + getters/setters (no Lombok).
- `MenuItem.allergens` type `String` (comma-separated).
- `Order.items` annotated with `@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true) @JoinColumn(name="order_id")` so `OrderItem` has a simple `order_id` FK column.
- Add `equals/hashCode` only where needed (skip for simplicity).

**Step 2: Write repositories**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    Optional<MenuItem> findByCode(String code);
    List<MenuItem> findByCategory(String category);
}
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUserId(Long userId);
}
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

**Step 3: Write repository test for MenuItem round-trip**

```java
@DataJpaTest
class MenuItemRepositoryTest {
    @Autowired MenuItemRepository repo;

    @Test
    void saves_and_finds_by_code() {
        MenuItem item = new MenuItem();
        item.setCode("item_001");
        item.setName("宫保鸡丁");
        item.setDescription("test");
        item.setPrice(new BigDecimal("22.00"));
        item.setCategory("chinese");
        item.setImage("/img/placeholder-kungpao.svg");
        item.setAllergens("peanut");
        repo.save(item);

        MenuItem found = repo.findByCode("item_001").orElseThrow();
        assertThat(found.getName()).isEqualTo("宫保鸡丁");
        assertThat(found.getPrice()).isEqualByComparingTo("22.00");
    }
}
```

**Step 4: Run and expect FAIL first, then make PASS by writing entities correctly**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q -Dtest=MenuItemRepositoryTest test`
Expected after entity code is written: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/entity src/main/java/com/webox/webox/repository src/test/java/com/webox/webox/repository
git commit -m "feat: add jpa entities and repositories"
```

---

## Task 3: Password encoder (SHA-256 + salt) with TDD

**Files:**
- Create: `src/main/java/com/webox/webox/service/PasswordEncoder.java`
- Create: `src/test/java/com/webox/webox/service/PasswordEncoderTest.java`

**Step 1: Write failing test**

```java
class PasswordEncoderTest {
    PasswordEncoder encoder = new PasswordEncoder();

    @Test
    void newSalt_is_base64_16bytes() {
        String salt = encoder.newSalt();
        byte[] raw = Base64.getDecoder().decode(salt);
        assertThat(raw).hasSize(16);
    }

    @Test
    void hash_is_deterministic_given_same_salt() {
        String salt = encoder.newSalt();
        assertThat(encoder.hash("pw123456", salt)).isEqualTo(encoder.hash("pw123456", salt));
    }

    @Test
    void hash_differs_for_different_salts() {
        String s1 = encoder.newSalt();
        String s2 = encoder.newSalt();
        assertThat(encoder.hash("pw123456", s1)).isNotEqualTo(encoder.hash("pw123456", s2));
    }

    @Test
    void matches_returns_true_for_correct_password() {
        String salt = encoder.newSalt();
        String h = encoder.hash("pw123456", salt);
        assertThat(encoder.matches("pw123456", salt, h)).isTrue();
        assertThat(encoder.matches("wrong", salt, h)).isFalse();
    }
}
```

**Step 2: Run and verify FAIL**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q -Dtest=PasswordEncoderTest test`
Expected: compilation failure (class missing).

**Step 3: Implement**

```java
@Component
public class PasswordEncoder {
    private final SecureRandom random = new SecureRandom();

    public String newSalt() {
        byte[] buf = new byte[16];
        random.nextBytes(buf);
        return Base64.getEncoder().encodeToString(buf);
    }

    public String hash(String password, String saltB64) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(saltB64));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(String password, String saltB64, String expectedHashHex) {
        return MessageDigest.isEqual(
            hash(password, saltB64).getBytes(StandardCharsets.US_ASCII),
            expectedHashHex.getBytes(StandardCharsets.US_ASCII));
    }
}
```

**Step 4: Run and verify PASS**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q -Dtest=PasswordEncoderTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/service/PasswordEncoder.java src/test/java/com/webox/webox/service/PasswordEncoderTest.java
git commit -m "feat: add sha256+salt password encoder"
```

---

## Task 4: Session model + auth interceptor

**Files:**
- Create: `src/main/java/com/webox/webox/model/LoginUser.java`
- Create: `src/main/java/com/webox/webox/support/SessionKeys.java`
- Create: `src/main/java/com/webox/webox/interceptor/AuthInterceptor.java`
- Create: `src/main/java/com/webox/webox/config/WebMvcConfig.java`
- Create: `src/test/java/com/webox/webox/interceptor/AuthInterceptorTest.java`

**Step 1: Write test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthInterceptorTest {
    @Autowired MockMvc mvc;

    @Test
    void unauthenticated_menu_redirects_to_login() throws Exception {
        mvc.perform(get("/menu"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/login"));
    }

    @Test
    void static_resources_are_not_blocked() throws Exception {
        mvc.perform(get("/css/app.css")).andExpect(status().isNotFound()); // file absent yet, but not redirected
    }

    @Test
    void authenticated_user_can_hit_menu() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.LOGIN_USER, new LoginUser(1L, "a@b.com", "A"));
        mvc.perform(get("/menu").session(session)).andExpect(status().isOk());
    }
}
```

Note: last assertion requires `/menu` returning 200; at this task it may not yet — split this assertion into a later task. For now, keep only the first two assertions and delete the third.

**Step 2: Run FAIL**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q -Dtest=AuthInterceptorTest test`

**Step 3: Implement**

- `LoginUser`: immutable record `public record LoginUser(Long id, String email, String name) {}`
- `SessionKeys`: `public final class SessionKeys { public static final String LOGIN_USER = "LOGIN_USER"; public static final String CART = "CART"; private SessionKeys(){} }`
- `AuthInterceptor#preHandle`: if session attribute `LOGIN_USER` null → `response.sendRedirect(request.getContextPath()+"/login"); return false;` else return true.
- `WebMvcConfig implements WebMvcConfigurer`: register the interceptor with `excludePathPatterns("/","/login","/register","/css/**","/js/**","/img/**","/h2-console/**","/error")`.

**Step 4: Run PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/model src/main/java/com/webox/webox/support src/main/java/com/webox/webox/interceptor src/main/java/com/webox/webox/config src/test/java/com/webox/webox/interceptor
git commit -m "feat: add auth interceptor and session model"
```

---

## Task 5: Register + login controllers

**Files:**
- Create: `src/main/java/com/webox/webox/dto/RegisterForm.java`
- Create: `src/main/java/com/webox/webox/dto/LoginForm.java`
- Create: `src/main/java/com/webox/webox/service/UserService.java`
- Create: `src/main/java/com/webox/webox/controller/AuthController.java`
- Create: `src/main/resources/templates/register.html`
- Create: `src/main/resources/templates/login.html`
- Create: `src/main/resources/templates/fragments/layout.html`
- Create: `src/test/java/com/webox/webox/controller/AuthControllerTest.java`

**Step 1: Write failing controller test**

Test cases:
- GET `/register` → 200 + contains form
- POST `/register` with valid data → 302 to `/menu`, session has `LOGIN_USER`
- POST `/register` with duplicate email → 200, page contains error message
- GET `/login` → 200
- POST `/login` with correct creds → 302 `/menu`
- POST `/login` with wrong password → 200 with error
- POST `/logout` → clears session, redirects to `/login`

Use `MockMvc` + `@SpringBootTest` + `TestEntityManager` or real repo (preferred: real repo with `@Transactional`).

**Step 2: Run FAIL**

**Step 3: Implement**

- `RegisterForm`: fields `email, name, password`; annotations `@Email @NotBlank`, `@NotBlank`, `@NotBlank @Size(min=6)`
- `LoginForm`: `email, password` both `@NotBlank`
- `UserService`:
  - `register(RegisterForm)` throws `BusinessException` if email exists; else create `User` with salt+hash, save, return LoginUser
  - `login(email,password)` returns `Optional<LoginUser>`
- `AuthController`:
  - GET `/register` + POST `/register` — on error, `model.addAttribute("error", e.getMessage())` and return template
  - GET `/login` + POST `/login`
  - POST `/logout` → `session.invalidate()` → `redirect:/login`
- Templates: minimal forms using Thymeleaf `th:object`/`th:field`; include `fragments/layout.html` with `<!DOCTYPE html>` + navbar placeholder.

**Step 4: Run PASS**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q -Dtest=AuthControllerTest test`

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/dto src/main/java/com/webox/webox/service/UserService.java src/main/java/com/webox/webox/controller/AuthController.java src/main/resources/templates src/test/java/com/webox/webox/controller/AuthControllerTest.java
git commit -m "feat: register/login/logout with session auth"
```

---

## Task 6: Seed menu data at startup

**Files:**
- Create: `src/main/java/com/webox/webox/config/DataInitializer.java`
- Create: `src/main/resources/static/img/placeholder-kungpao.svg` … 8 SVG files
- Create: `src/test/java/com/webox/webox/config/DataInitializerTest.java`

**Step 1: Write failing test**

```java
@SpringBootTest
class DataInitializerTest {
    @Autowired MenuItemRepository repo;

    @Test
    void seeds_eight_menu_items() {
        assertThat(repo.count()).isEqualTo(8L);
        assertThat(repo.findByCode("item_001")).isPresent();
    }
}
```

**Step 2: Run FAIL**

**Step 3: Implement**

- `DataInitializer implements ApplicationRunner`:
  - If `menuItemRepository.count() == 0`, insert the 8 PRD items with local image paths `/img/placeholder-<slug>.svg`.
- Each SVG is a simple colored rect + `<text>` of the dish name (≤ 40 lines).

**Step 4: Run PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/config/DataInitializer.java src/main/resources/static/img src/test/java/com/webox/webox/config/DataInitializerTest.java
git commit -m "feat: seed 8 menu items on startup"
```

---

## Task 7: Menu browse + detail pages

**Files:**
- Create: `src/main/java/com/webox/webox/service/MenuService.java`
- Create: `src/main/java/com/webox/webox/controller/HomeController.java`
- Create: `src/main/java/com/webox/webox/controller/MenuController.java`
- Create: `src/main/resources/templates/menu/list.html`
- Create: `src/main/resources/templates/menu/detail.html`
- Create: `src/main/resources/static/css/app.css`
- Create: `src/test/java/com/webox/webox/service/MenuServiceTest.java`
- Create: `src/test/java/com/webox/webox/controller/MenuControllerTest.java`

**Step 1: Write failing service test**

```java
@SpringBootTest
class MenuServiceTest {
    @Autowired MenuService service;
    @Autowired UserPreferenceRepository prefRepo;

    @Test
    void lists_all_when_no_filter() {
        assertThat(service.list(null, false, null)).hasSize(8);
    }

    @Test
    void filters_by_category() {
        assertThat(service.list("chinese", false, null))
            .extracting(MenuItem::getCategory).containsOnly("chinese");
    }

    @Test
    void onlyRecommend_filters_out_allergen_hits() {
        // seed preference with peanut allergen for userId=99
        UserPreference pref = new UserPreference();
        pref.setUserId(99L); pref.setAllergens("peanut"); pref.setRecommendOnly(true);
        prefRepo.save(pref);
        List<MenuItem> result = service.list(null, true, 99L);
        assertThat(result).noneMatch(m -> m.getAllergens().contains("peanut"));
    }
}
```

**Step 2: FAIL**

**Step 3: Implement `MenuService.list` + controllers**

- `HomeController`: GET `/` → if session `LOGIN_USER` present redirect `/menu` else `/login`
- `MenuController`:
  - GET `/menu?category=&onlyRecommend=` → renders list; passes categories, selected category, onlyRecommend flag
  - GET `/menu/{code}` → loads item by code; 404 page otherwise
- `MenuService.list`:
  - Start with `repo.findAll()` or `findByCategory`
  - If `onlyRecommend && userId != null` → load preference; filter out items whose allergens intersect preference allergens
- Template renders cards with `⚠ 含 <allergen>` label when item allergens non-empty and not in onlyRecommend mode

**Step 4: Write controller test — assert `/menu` returns 200 with 8 items when authenticated; 302 otherwise**

**Step 5: Run all tests PASS**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q test`

**Step 6: Commit**

```bash
git add src/main/java/com/webox/webox/service/MenuService.java src/main/java/com/webox/webox/controller/HomeController.java src/main/java/com/webox/webox/controller/MenuController.java src/main/resources/templates/menu src/main/resources/static/css/app.css src/test/java/com/webox/webox/service/MenuServiceTest.java src/test/java/com/webox/webox/controller/MenuControllerTest.java
git commit -m "feat: menu list and detail with category/preference filters"
```

---

## Task 8: Cart (session-backed) service + controller + page

**Files:**
- Create: `src/main/java/com/webox/webox/dto/CartView.java`
- Create: `src/main/java/com/webox/webox/service/CartService.java`
- Create: `src/main/java/com/webox/webox/controller/CartController.java`
- Create: `src/main/resources/templates/cart.html`
- Create: `src/test/java/com/webox/webox/service/CartServiceTest.java`

**Step 1: Write failing test**

Cases against `CartService` taking `HttpSession` (use `MockHttpSession`):
- `add(session, 1L, 2)` then `view(session)` total = price*2
- repeat `add(session, 1L, 3)` → quantity 5
- `update(session, 1L, 1)` → quantity 1
- `remove(session, 1L)` → empty
- `clear(session)` → empty

**Step 2: FAIL**

**Step 3: Implement**

- `CartView` contains `List<Line>` and `BigDecimal total`; `Line` has `MenuItem menuItem, int quantity, BigDecimal subtotal`.
- `CartService` reads/writes `Map<Long,Integer>` at `SessionKeys.CART`; `view` enriches via `MenuItemRepository`.
- `CartController` endpoints:
  - GET `/cart`
  - POST `/cart/add` with `menuItemId`, `quantity`, `redirect` (defaults to `/menu`)
  - POST `/cart/update`
  - POST `/cart/remove`
- Layout fragment shows cart count via a `@ModelAttribute("cartCount")` method on a `@ControllerAdvice` (or computed per request).

**Step 4: PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/dto/CartView.java src/main/java/com/webox/webox/service/CartService.java src/main/java/com/webox/webox/controller/CartController.java src/main/resources/templates/cart.html src/test/java/com/webox/webox/service/CartServiceTest.java
git commit -m "feat: session-backed cart with add/update/remove/view"
```

---

## Task 9: Checkout + orders

**Files:**
- Create: `src/main/java/com/webox/webox/dto/CheckoutForm.java`
- Create: `src/main/java/com/webox/webox/service/OrderService.java`
- Create: `src/main/java/com/webox/webox/controller/CheckoutController.java`
- Create: `src/main/java/com/webox/webox/controller/OrderController.java`
- Create: `src/main/resources/templates/checkout.html`
- Create: `src/main/resources/templates/order/success.html`
- Create: `src/main/resources/templates/order/list.html`
- Create: `src/main/resources/templates/order/detail.html`
- Create: `src/test/java/com/webox/webox/service/OrderServiceTest.java`

**Step 1: Write failing test**

- `placeOrder` builds Order with totals computed from menu prices (ignore any client-supplied total)
- `placeOrder` empties the session cart
- `listByUser` returns orders descending by createdAt

**Step 2: FAIL**

**Step 3: Implement**

- `CheckoutForm`: `deliveryDate (LocalDate, @Future-or-today)`, `mealPeriod @NotBlank`, `deliveryAddress @NotBlank`
- `OrderService.placeOrder(LoginUser, CheckoutForm, HttpSession)`:
  - read cart → fail if empty (`BusinessException`)
  - load MenuItems by ids; if any missing → fail
  - compute `totalAmount = Σ price * qty`
  - build `Order` with status `CREATED`, `createdAt=Instant.now()`
  - save with cascade
  - clear cart
  - return saved Order
- `CheckoutController`:
  - GET `/checkout` → render cart summary + form
  - POST `/checkout` → call service → redirect to `/orders/success?id=<id>`
- `OrderController`:
  - GET `/orders/success?id=` → show order
  - GET `/orders` → list user's orders
  - GET `/orders/{id}` → detail; 404 if not owned by current user

**Step 4: PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/dto/CheckoutForm.java src/main/java/com/webox/webox/service/OrderService.java src/main/java/com/webox/webox/controller/CheckoutController.java src/main/java/com/webox/webox/controller/OrderController.java src/main/resources/templates/checkout.html src/main/resources/templates/order src/test/java/com/webox/webox/service/OrderServiceTest.java
git commit -m "feat: checkout and order list/detail"
```

---

## Task 10: Preferences page (加分项 A)

**Files:**
- Create: `src/main/java/com/webox/webox/dto/PreferenceForm.java`
- Create: `src/main/java/com/webox/webox/service/PreferenceService.java`
- Create: `src/main/java/com/webox/webox/controller/PreferenceController.java`
- Create: `src/main/resources/templates/preferences.html`
- Create: `src/test/java/com/webox/webox/service/PreferenceServiceTest.java`

**Step 1: Write failing test**

- Saving preferences for a new user creates a row
- Saving again upserts the existing row (count stays 1)
- `MenuService.list` with `onlyRecommend=true` respects the new preference

**Step 2: FAIL**

**Step 3: Implement**

- `PreferenceForm`: `List<String> allergens`, `boolean recommendOnly`
- `PreferenceService.save(userId, form)`: `findByUserId` → update or create
- `PreferenceController`:
  - GET `/preferences` → load existing pref + render checkboxes for allergen constants
  - POST `/preferences` → save → flash `"已保存"` → redirect `/preferences`
- Allergen constants list: `peanut, shellfish, dairy, egg, gluten, fish, soy`

**Step 4: PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/dto/PreferenceForm.java src/main/java/com/webox/webox/service/PreferenceService.java src/main/java/com/webox/webox/controller/PreferenceController.java src/main/resources/templates/preferences.html src/test/java/com/webox/webox/service/PreferenceServiceTest.java
git commit -m "feat: user dietary preferences page"
```

---

## Task 11: Qwen client + prompt builder (加分项 B)

**Files:**
- Create: `src/main/java/com/webox/webox/config/RestClientConfig.java`
- Create: `src/main/java/com/webox/webox/service/ai/QwenProperties.java`
- Create: `src/main/java/com/webox/webox/service/ai/QwenClient.java`
- Create: `src/main/java/com/webox/webox/service/ai/PromptBuilder.java`
- Create: `src/test/java/com/webox/webox/service/ai/PromptBuilderTest.java`
- Create: `src/test/java/com/webox/webox/service/ai/QwenClientTest.java`

**Step 1: Write failing tests**

- `PromptBuilderTest`:
  - `build(query, userAllergens, menu)` returns 2 messages: `system` non-empty, `user` containing query and each menu item's `code`
  - when `userAllergens` empty → user message still valid, no "过敏原" section
- `QwenClientTest` using `MockRestServiceServer`:
  - server expects POST to base-url + `/chat/completions` with correct Auth header
  - returns canned JSON with `choices[0].message.content = '{"recommendations":[{"code":"item_001","reason":"..."}]}'`
  - client parses and returns `List<Recommendation>`
  - when Key empty → throws `AiUnavailableException` (no network call)

**Step 2: FAIL**

**Step 3: Implement**

- `QwenProperties` bound with `@ConfigurationProperties("webox.ai")` — fields `baseUrl`, `apiKey`, `model`, `timeoutMs`
- `RestClientConfig` exposes a `RestClient` bean using `JdkClientHttpRequestFactory` with configured timeouts
- `PromptBuilder.build(query, allergens, List<MenuItem>)` returns `List<Map<String,String>>` with `{role, content}` entries
- `QwenClient.recommend(query, allergens, menu)`:
  - if `apiKey.isBlank()` → throw `AiUnavailableException("API Key not configured")`
  - POST body: `{model, messages, response_format:{type:"json_object"}, temperature:0.3}`
  - Read `choices[0].message.content`, deserialize into `RecommendationEnvelope{recommendations: List<Recommendation>}`
  - return list
- Register `@EnableConfigurationProperties(QwenProperties.class)` on config

**Step 4: PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/config/RestClientConfig.java src/main/java/com/webox/webox/service/ai src/test/java/com/webox/webox/service/ai
git commit -m "feat: qwen dashscope client and prompt builder"
```

---

## Task 12: AI recommend service + API + UI wiring

**Files:**
- Create: `src/main/java/com/webox/webox/dto/AiRecommendRequest.java`
- Create: `src/main/java/com/webox/webox/dto/AiRecommendResponse.java`
- Create: `src/main/java/com/webox/webox/service/ai/AiRecommendService.java`
- Create: `src/main/java/com/webox/webox/controller/AiRecommendController.java`
- Create: `src/main/resources/static/js/ai.js`
- Modify: `src/main/resources/templates/menu/list.html` (add AI panel)
- Create: `src/test/java/com/webox/webox/controller/AiRecommendControllerTest.java`

**Step 1: Write failing controller test**

- POST `/api/ai/recommend` (unauthenticated) → 302 to `/login`
- Authenticated with stubbed `QwenClient` returning 2 codes → response includes 2 items enriched with name/price
- Authenticated with `AiUnavailableException` thrown → 200 with `{items:[], error:"..."}`
- Responses with codes not in menu are filtered out

**Step 2: FAIL**

**Step 3: Implement**

- `AiRecommendRequest { String query; }`
- `AiRecommendResponse { List<Item> items; String error; }` (items: `code,name,price,reason`)
- `AiRecommendService.recommend(userId, query)`:
  - load menu and user preference allergens
  - call `QwenClient.recommend`
  - enrich `recommendations` by looking up `MenuItem` per code; drop unknown codes
  - cap to 5; if fewer than 3 and fallback desired, sort remaining menu by (category match? → removed; allergen-free first) and top up
  - on `AiUnavailableException` or `JsonProcessingException` or `RestClientException`: log warning (no key material), return empty + error message
- `AiRecommendController`:
  - `@PostMapping("/api/ai/recommend")` consumes JSON, returns JSON
  - `@RequestBody AiRecommendRequest`, `@Valid` with `@NotBlank query`
  - Derive `userId` from `HttpSession` `LOGIN_USER`
- `ai.js`:
  - Intercepts form submit; fetch POST JSON; render result list with "加入购物车" buttons that submit `/cart/add` via hidden form
- `menu/list.html`: adds a collapsible AI panel with input, submit button, result container

**Step 4: PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/dto/AiRecommendRequest.java src/main/java/com/webox/webox/dto/AiRecommendResponse.java src/main/java/com/webox/webox/service/ai/AiRecommendService.java src/main/java/com/webox/webox/controller/AiRecommendController.java src/main/resources/static/js/ai.js src/main/resources/templates/menu/list.html src/test/java/com/webox/webox/controller/AiRecommendControllerTest.java
git commit -m "feat: ai recommend api and menu page integration"
```

---

## Task 13: Global error handling + 404/500 pages

**Files:**
- Create: `src/main/java/com/webox/webox/support/BusinessException.java`
- Create: `src/main/java/com/webox/webox/support/AiUnavailableException.java` (if not already)
- Create: `src/main/java/com/webox/webox/controller/GlobalExceptionHandler.java`
- Create: `src/main/resources/templates/error/404.html`
- Create: `src/main/resources/templates/error/500.html`
- Create: `src/main/resources/templates/error/business.html`
- Create: `src/test/java/com/webox/webox/controller/GlobalExceptionHandlerTest.java`

**Step 1: Failing test**

- Access non-existent route → renders 404 template (expect `status().isNotFound()` and model attribute or body contains "页面不存在")
- Endpoint throwing `BusinessException` → 200 with business template and message
- Endpoint throwing `RuntimeException` → 500 with 500 template

Add a test-only controller under `src/test/.../ThrowingController` mounted for the test.

**Step 2: FAIL**

**Step 3: Implement `@ControllerAdvice`**

- `@ExceptionHandler(BusinessException.class)` → business template + `model("message", ex.getMessage())`
- `@ExceptionHandler(Exception.class)` → 500 template
- Ensure Spring's default `NoHandlerFoundException` routed to `error/404.html`; add `spring.mvc.throw-exception-if-no-handler-found: true` and `spring.web.resources.add-mappings: true` in `application.yml`.

**Step 4: PASS**

**Step 5: Commit**

```bash
git add src/main/java/com/webox/webox/support src/main/java/com/webox/webox/controller/GlobalExceptionHandler.java src/main/resources/templates/error src/main/resources/application.yml src/test/java/com/webox/webox/controller/GlobalExceptionHandlerTest.java
git commit -m "feat: global exception handling and error pages"
```

---

## Task 14: End-to-end smoke integration test

**Files:**
- Create: `src/test/java/com/webox/webox/SmokeE2ETest.java`

**Step 1: Write test**

`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`:
1. register user `e2e@webox.com`
2. login → capture `JSESSIONID`
3. GET `/menu` → 200 contains "宫保鸡丁"
4. POST `/cart/add` (`menuItemId=1, quantity=2`) → 302
5. GET `/cart` → contains quantity 2
6. POST `/checkout` (date=tomorrow, period=lunch, address="Office")
7. GET `/orders` → contains one order
8. GET `/orders/{id}` → totalAmount matches `price*2`

**Step 2: Run PASS**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q -Dtest=SmokeE2ETest test`

**Step 3: Commit**

```bash
git add src/test/java/com/webox/webox/SmokeE2ETest.java
git commit -m "test: end-to-end smoke for register→order flow"
```

---

## Task 15: README + final verification

**Files:**
- Modify: `README.md`

**Step 1: Write README sections**

- Project intro (from PRD)
- Build: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn spring-boot:run`
- Access: `http://localhost:8080`
- H2 Console: `/h2-console` (jdbc url `jdbc:h2:mem:webox`)
- AI config: `export DASHSCOPE_API_KEY=...` then rerun; without key AI panel shows "AI 暂不可用，请稍后再试"
- Test: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn test`

**Step 2: Run full suite**

Run: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q test`
Expected: all green.

**Step 3: Manual smoke**

Run app locally, click through the checklist from design §8.5.

**Step 4: Commit**

```bash
git add README.md
git commit -m "docs: update README with run/test instructions"
```

---

## Done

At this point all PRD items — core + A + B — are implemented, tested, and documented. No further tasks planned.
