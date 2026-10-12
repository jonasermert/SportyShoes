package com.jonasermert.sportyshoes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class Api {
  private final Accounts accounts;
  private final Products products;
  private final Purchases purchases;
  private final PasswordEncoder encoder;

  Api(Accounts a, Products p, Purchases o, PasswordEncoder e) {
    accounts = a;
    products = p;
    purchases = o;
    encoder = e;
  }

  static class Registration {
    @NotBlank public String name;
    @Email @NotBlank public String email;

    @Size(min = 8, max = 72)
    @NotNull
    public String password;
  }

  static class PasswordChange {
    @NotBlank public String currentPassword;

    @Size(min = 8, max = 72)
    @NotNull
    public String newPassword;
  }

  static class ProductInput {
    @NotBlank public String name;
    @NotBlank public String category;

    @NotNull
    @DecimalMin("0.01")
    public BigDecimal price;

    @Min(0)
    public int stock;

    public String description;
    public boolean active = true;
  }

  static class OrderInput {
    @NotNull public Long productId;

    @Min(1)
    @Max(100)
    public int quantity;
  }

  @GetMapping("/csrf")
  Object csrf(CsrfToken token) {
    return Collections.singletonMap("token", token.getToken());
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  Account register(@Valid @RequestBody Registration input) {
    String email = input.email.trim().toLowerCase(Locale.ROOT);
    if (accounts.findByEmail(email).isPresent())
      throw error(HttpStatus.CONFLICT, "Email already registered");
    Account a = new Account();
    a.name = input.name.trim();
    a.email = email;
    a.password = encoder.encode(input.password);
    a.role = "USER";
    return accounts.save(a);
  }

  @GetMapping("/me")
  Account me(Principal p) {
    return account(p);
  }

  @PostMapping("/password")
  @Transactional
  public void password(
      Principal p, @Valid @RequestBody PasswordChange input, HttpServletRequest request) {
    Account a = account(p);
    if (!encoder.matches(input.currentPassword, a.password))
      throw error(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    a.password = encoder.encode(input.newPassword);
    accounts.save(a);
    request.getSession().invalidate();
  }

  @GetMapping("/products")
  List<Product> catalogue() {
    return products.findAll().stream().filter(p -> p.active).collect(Collectors.toList());
  }

  @GetMapping("/admin/products")
  List<Product> all() {
    return products.findAll();
  }

  @PostMapping("/admin/products")
  @ResponseStatus(HttpStatus.CREATED)
  Product create(@Valid @RequestBody ProductInput in) {
    return save(new Product(), in);
  }

  @PutMapping("/admin/products/{id}")
  Product update(@PathVariable Long id, @Valid @RequestBody ProductInput in) {
    return save(
        products.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Product not found")),
        in);
  }

  @DeleteMapping("/admin/products/{id}")
  void archive(@PathVariable Long id) {
    Product p =
        products.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Product not found"));
    p.active = false;
    products.save(p);
  }

  private Product save(Product p, ProductInput in) {
    p.name = in.name.trim();
    p.category = in.category.trim();
    p.price = in.price.setScale(2, java.math.RoundingMode.HALF_UP);
    p.stock = in.stock;
    p.description = in.description;
    p.active = in.active;
    return products.save(p);
  }

  @GetMapping("/admin/users")
  List<Account> users(@RequestParam(defaultValue = "") String search) {
    String q = search.toLowerCase(Locale.ROOT);
    return accounts.findAll().stream()
        .filter(a -> a.name.toLowerCase(Locale.ROOT).contains(q) || a.email.contains(q))
        .collect(Collectors.toList());
  }

  @PostMapping("/purchases")
  @Transactional
  @ResponseStatus(HttpStatus.CREATED)
  public Purchase buy(Principal principal, @Valid @RequestBody OrderInput in) {
    Account a = account(principal);
    Product locked =
        em.find(Product.class, in.productId, javax.persistence.LockModeType.PESSIMISTIC_WRITE);
    if (locked == null) throw error(HttpStatus.NOT_FOUND, "Product not found");
    if (!locked.active || locked.stock < in.quantity)
      throw error(HttpStatus.CONFLICT, "Insufficient stock");
    locked.stock -= in.quantity;
    products.save(locked);
    Purchase o = new Purchase();
    o.userId = a.id;
    o.customer = a.email;
    o.productId = locked.id;
    o.product = locked.name;
    o.category = locked.category;
    o.quantity = in.quantity;
    o.total = locked.price.multiply(BigDecimal.valueOf(in.quantity));
    o.date = LocalDate.now();
    return purchases.save(o);
  }

  @javax.persistence.PersistenceContext private javax.persistence.EntityManager em;

  @GetMapping("/purchases")
  List<Purchase> history(Principal p) {
    Long id = account(p).id;
    return purchases.findAll().stream()
        .filter(o -> o.userId.equals(id))
        .collect(Collectors.toList());
  }

  @GetMapping("/admin/reports")
  List<Purchase> reports(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "") String category) {
    LocalDate start = parse(from), end = parse(to);
    if (start != null && end != null && start.isAfter(end))
      throw error(HttpStatus.BAD_REQUEST, "Start date must precede end date");
    return purchases.findAll().stream()
        .filter(
            o ->
                (start == null || !o.date.isBefore(start))
                    && (end == null || !o.date.isAfter(end))
                    && (category.isEmpty() || category.equals(o.category)))
        .collect(Collectors.toList());
  }

  private LocalDate parse(String s) {
    try {
      return s == null || s.isEmpty() ? null : LocalDate.parse(s);
    } catch (Exception e) {
      throw error(HttpStatus.BAD_REQUEST, "Invalid date");
    }
  }

  private Account account(Principal p) {
    return accounts
        .findByEmail(p.getName())
        .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Sign in required"));
  }

  private ResponseStatusException error(HttpStatus s, String message) {
    return new ResponseStatusException(s, message);
  }

  @ExceptionHandler({
    org.springframework.dao.DataIntegrityViolationException.class,
    org.springframework.dao.OptimisticLockingFailureException.class
  })
  @ResponseStatus(HttpStatus.CONFLICT)
  Map<String, String> conflict() {
    return Collections.singletonMap(
        "message", "The record changed or already exists. Refresh and retry.");
  }
}
