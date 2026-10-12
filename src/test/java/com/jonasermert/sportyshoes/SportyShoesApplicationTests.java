package com.jonasermert.sportyshoes;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class SportyShoesApplicationTests {
  @Autowired MockMvc mvc;
  @Autowired Accounts accounts;
  @Autowired Products products;
  @Autowired Purchases purchases;
  @Autowired PasswordEncoder encoder;
  @Autowired ObjectMapper json;

  @BeforeEach
  void reset() {
    purchases.deleteAll();
    products.deleteAll();
    accounts.deleteAll();
    Account a = new Account();
    a.name = "Admin";
    a.email = "admin@sportyshoes.local";
    a.password = encoder.encode("ChangeMe123!");
    a.role = "ADMIN";
    accounts.save(a);
    Account u = new Account();
    u.name = "Runner";
    u.email = "runner@example.com";
    u.password = encoder.encode("Runner123!");
    u.role = "USER";
    accounts.save(u);
  }

  Product product() {
    Product p = new Product();
    p.name = "Runner";
    p.category = "Running";
    p.price = new BigDecimal("90.00");
    p.stock = 2;
    return products.save(p);
  }

  @Test
  void authorizationAndCsrf() throws Exception {
    mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/users").with(user("runner@example.com").roles("USER")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/products")
                .with(user("admin@sportyshoes.local").roles("ADMIN"))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/products")).andExpect(status().isOk());
  }

  @Test
  void registrationValidationAndSearch() throws Exception {
    String body =
        "{\"name\":\"Jonas\",\"email\":\"jonas@example.com\",\"password\":\"Password123!\"}";
    mvc.perform(post("/api/register").with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.password").doesNotExist());
    mvc.perform(post("/api/register").with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/register")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"bad\",\"password\":\"x\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/admin/users?search=JONAS")
                .with(user("admin@sportyshoes.local").roles("ADMIN")))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].password").doesNotExist());
    assertTrue(
        encoder.matches("Password123!", accounts.findByEmail("jonas@example.com").get().password));
  }

  @Test
  void loginAndPasswordChange() throws Exception {
    mvc.perform(
            post("/api/login")
                .with(csrf())
                .param("username", "admin@sportyshoes.local")
                .param("password", "wrong"))
        .andExpect(status().isUnauthorized());
    MockHttpSession session =
        (MockHttpSession)
            mvc.perform(
                    post("/api/login")
                        .with(csrf())
                        .param("username", "admin@sportyshoes.local")
                        .param("password", "ChangeMe123!"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getRequest()
                .getSession(false);
    mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isOk());
    mvc.perform(
            post("/api/password")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"NewPassword123!\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/password")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"currentPassword\":\"ChangeMe123!\",\"newPassword\":\"NewPassword123!\"}"))
        .andExpect(status().isOk());
    assertTrue(session.isInvalid());
    mvc.perform(
            post("/api/login")
                .with(csrf())
                .param("username", "admin@sportyshoes.local")
                .param("password", "NewPassword123!"))
        .andExpect(status().isNoContent());
  }

  @Test
  void productManagementAndHistoricalReports() throws Exception {
    String body = "{\"name\":\"Test Shoe\",\"category\":\"Running\",\"price\":90,\"stock\":2}";
    String result =
        mvc.perform(
                post("/api/admin/products")
                    .with(user("admin@sportyshoes.local").roles("ADMIN"))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long id = json.readTree(result).get("id").asLong();
    mvc.perform(
            post("/api/purchases")
                .with(user("runner@example.com"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"productId\":" + id + ",\"quantity\":2}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total").value(180));
    assertEquals(0, products.findById(id).get().stock);
    mvc.perform(
            post("/api/purchases")
                .with(user("runner@example.com"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"productId\":" + id + ",\"quantity\":1}"))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/admin/products/" + id)
                .with(user("admin@sportyshoes.local").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("Running", "Outdoor")))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/admin/reports?category=Running&from="
                    + LocalDate.now()
                    + "&to="
                    + LocalDate.now())
                .with(user("admin@sportyshoes.local").roles("ADMIN")))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].category").value("Running"));
    mvc.perform(
            get("/api/admin/reports?category=Outdoor")
                .with(user("admin@sportyshoes.local").roles("ADMIN")))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            get("/api/admin/reports?from=2026-12-31&to=2026-01-01")
                .with(user("admin@sportyshoes.local").roles("ADMIN")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            delete("/api/admin/products/" + id)
                .with(user("admin@sportyshoes.local").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/products")).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/purchases").with(user("runner@example.com")))
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/purchases").with(user("admin@sportyshoes.local").roles("ADMIN")))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void rejectsInvalidOrdersAndProducts() throws Exception {
    Product p = product();
    mvc.perform(
            post("/api/purchases")
                .with(user("runner@example.com"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"productId\":" + p.id + ",\"quantity\":0}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/admin/products")
                .with(user("admin@sportyshoes.local").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"Shoe\",\"category\":\"Running\",\"price\":-1,\"stock\":-1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void concurrentPurchasesCannotOversell() throws Exception {
    Product p = product();
    p.stock = 1;
    products.save(p);
    java.util.concurrent.ExecutorService pool =
        java.util.concurrent.Executors.newFixedThreadPool(2);
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.Callable<Integer> buy =
        () -> {
          start.await();
          return mvc.perform(
                  post("/api/purchases")
                      .with(user("runner@example.com"))
                      .with(csrf())
                      .contentType("application/json")
                      .content("{\"productId\":" + p.id + ",\"quantity\":1}"))
              .andReturn()
              .getResponse()
              .getStatus();
        };
    try {
      java.util.concurrent.Future<Integer> first = pool.submit(buy);
      java.util.concurrent.Future<Integer> second = pool.submit(buy);
      start.countDown();
      java.util.List<Integer> statuses =
          java.util.Arrays.asList(
              first.get(10, java.util.concurrent.TimeUnit.SECONDS),
              second.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertTrue(statuses.contains(201));
      assertTrue(statuses.contains(409));
      assertEquals(0, products.findById(p.id).get().stock);
      assertEquals(1, purchases.count());
    } finally {
      pool.shutdownNow();
    }
  }
}
