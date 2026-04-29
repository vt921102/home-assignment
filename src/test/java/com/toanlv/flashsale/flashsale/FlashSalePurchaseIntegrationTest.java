package com.toanlv.flashsale.flashsale;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.testcontainers.RedisContainer;
import com.toanlv.flashsale.auth.domain.IdentifierType;
import com.toanlv.flashsale.auth.domain.User;
import com.toanlv.flashsale.auth.repository.UserRepository;
import com.toanlv.flashsale.flashsale.domain.FlashSaleSession;
import com.toanlv.flashsale.flashsale.domain.FlashSaleSessionItem;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionItemRepository;
import com.toanlv.flashsale.flashsale.repository.FlashSaleSessionRepository;
import com.toanlv.flashsale.flashsale.repository.UserDailyPurchaseLimitRepository;
import com.toanlv.flashsale.flashsale.service.IPurchaseService;
import com.toanlv.flashsale.inventory.domain.Inventory;
import com.toanlv.flashsale.inventory.repository.InventoryRepository;
import com.toanlv.flashsale.order.repository.BalanceTransactionRepository;
import com.toanlv.flashsale.order.repository.OrderRepository;
import com.toanlv.flashsale.product.domain.Product;
import com.toanlv.flashsale.product.domain.ProductCategory;
import com.toanlv.flashsale.product.repository.CategoryRepository;
import com.toanlv.flashsale.product.repository.ProductRepository;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class FlashSalePurchaseIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("flashsale_test")
          .withUsername("test")
          .withPassword("test");

  @Container static RedisContainer redis = new RedisContainer("redis:7-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired IPurchaseService purchaseService;
  @Autowired UserRepository userRepository;
  @Autowired FlashSaleSessionRepository sessionRepository;
  @Autowired FlashSaleSessionItemRepository itemRepository;
  @Autowired UserDailyPurchaseLimitRepository limitRepository;
  @Autowired OrderRepository orderRepository;
  @Autowired BalanceTransactionRepository balanceTransactionRepository;
  @Autowired ProductRepository productRepository;
  @Autowired CategoryRepository categoryRepository;
  @Autowired InventoryRepository inventoryRepository;

  private FlashSaleSessionItem sessionItem;

  @BeforeEach
  void setUp() {
    // Clean state — order matches FK constraints
    balanceTransactionRepository.deleteAll();
    orderRepository.deleteAll();
    limitRepository.deleteAll();
    itemRepository.deleteAll();
    sessionRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    userRepository.deleteAll();

    // Product
    var category = categoryRepository.save(ProductCategory.create("Electronics", null));
    var product =
        productRepository.save(
            Product.create(
                "TEST-SKU-001",
                "Test Product",
                "Description",
                BigDecimal.valueOf(100_000),
                null,
                category));
    inventoryRepository.save(Inventory.initWithStock(product.getId(), 100));

    // Session — use current time-based window for test
    var now = LocalTime.now().minusMinutes(30);
    var until = LocalTime.now().plusHours(2);
    var session =
        sessionRepository.save(FlashSaleSession.create("Test Sale", LocalDate.now(), now, until));

    sessionItem =
        itemRepository.save(
            FlashSaleSessionItem.create(session, product, BigDecimal.valueOf(99_000), 10, 1));
  }

  // ----------------------------------------------------------------
  // Correctness: 100 concurrent users, stock = 10 → exactly 10 succeed
  // ----------------------------------------------------------------

  @Test
  void concurrentPurchases_neverExceedStock() throws InterruptedException {
    int concurrency = 50;
    var users = createUsers(concurrency, BigDecimal.valueOf(1_000_000));
    var executor = Executors.newVirtualThreadPerTaskExecutor();
    var latch = new CountDownLatch(concurrency);
    var success = new AtomicInteger(0);
    var failed = new AtomicInteger(0);

    for (var user : users) {
      executor.submit(
          () -> {
            try {
              purchaseService.purchase(
                  user.getId(), sessionItem.getId(), UUID.randomUUID().toString());
              success.incrementAndGet();
            } catch (Exception e) {
              failed.incrementAndGet();
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();

    // Stock was 10 — exactly 10 should succeed
    assertThat(success.get()).isEqualTo(10);
    assertThat(failed.get()).isEqualTo(40);

    var updatedItem = itemRepository.findById(sessionItem.getId()).orElseThrow();
    assertThat(updatedItem.getSoldQuantity()).isEqualTo(10);

    // Verify exactly 10 orders created
    var orders = orderRepository.findAll();
    assertThat(orders).hasSize(10);
  }

  // ----------------------------------------------------------------
  // Idempotency: same key twice → 1 order
  // ----------------------------------------------------------------

  @Test
  void purchase_withSameIdempotencyKey_createsOnlyOneOrder() throws InterruptedException {
    var user = createUsers(1, BigDecimal.valueOf(1_000_000)).get(0);
    var idemKey = UUID.randomUUID().toString();
    var executor = Executors.newVirtualThreadPerTaskExecutor();
    var latch = new CountDownLatch(5);
    var success = new AtomicInteger(0);

    for (int i = 0; i < 5; i++) {
      executor.submit(
          () -> {
            try {
              purchaseService.purchase(user.getId(), sessionItem.getId(), idemKey);
              success.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
              latch.countDown();
            }
          });
    }
    latch.await();

    var orders = orderRepository.findAll();
    assertThat(orders).hasSize(1);
  }

  // ----------------------------------------------------------------
  // Daily limit: same user twice → second fails
  // ----------------------------------------------------------------

  @Test
  void purchase_sameUserTwiceInSameDay_secondFails() {
    var user = createUsers(1, BigDecimal.valueOf(5_000_000)).get(0);

    // First purchase — should succeed
    purchaseService.purchase(user.getId(), sessionItem.getId(), UUID.randomUUID().toString());

    // Second purchase — should fail (daily limit)
    var secondItemId = sessionItem.getId();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                purchaseService.purchase(user.getId(), secondItemId, UUID.randomUUID().toString()))
        .isInstanceOf(Exception.class);

    assertThat(orderRepository.findAll()).hasSize(1);
  }

  // ----------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------

  private List<User> createUsers(int count, BigDecimal balance) {
    var users = new ArrayList<User>();
    for (int i = 0; i < count; i++) {
      var user =
          User.create(
              "user" + i + "_" + UUID.randomUUID() + "@test.com",
              IdentifierType.EMAIL,
              "$2a$12$hashedpwd");
      user.activate();

      // Set balance via reflection workaround — using a fresh user
      // with balance set through the repository's native update
      var saved = userRepository.save(user);
      userRepository.deductBalance(saved.getId(), BigDecimal.ZERO.subtract(balance));

      // Re-fetch to verify
      users.add(userRepository.findById(saved.getId()).orElseThrow());
    }
    return users;
  }
}
