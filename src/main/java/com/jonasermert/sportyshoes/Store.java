package com.jonasermert.sportyshoes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jonasermert.sportyshoes.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

@Entity
class Account extends BaseEntity {
  @Id @GeneratedValue public Long id;

  @Column(unique = true, nullable = false)
  public String email;

  public String name;
  @JsonIgnore public String password;
  public String role;
}

@Entity
class Product extends BaseEntity {
  @Version public Long version;
  @Id @GeneratedValue public Long id;
  public String name;
  public String category;

  @Column(length = 2000)
  public String description;

  public BigDecimal price;
  public int stock;
  public boolean active = true;
}

@Entity
class Purchase extends BaseEntity {
  @Id @GeneratedValue public Long id;
  public Long userId;
  public Long productId;
  public String customer;
  public String product;
  public String category;
  public int quantity;
  public BigDecimal total;
  public LocalDate date;
}

interface Accounts extends JpaRepository<Account, Long> {
  java.util.Optional<Account> findByEmail(String email);
}

interface Products extends JpaRepository<Product, Long> {}

interface Purchases extends JpaRepository<Purchase, Long> {}
