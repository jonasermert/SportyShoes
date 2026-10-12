package com.jonasermert.sportyshoes.entity;

import java.time.Instant;
import javax.persistence.*;

@MappedSuperclass
public abstract class BaseEntity {
  @Column(nullable = false, updatable = false)
  public Instant createDate;

  public Instant lastUpdateDate;

  @PrePersist
  public void created() {
    createDate = Instant.now();
    lastUpdateDate = createDate;
  }

  @PreUpdate
  public void updated() {
    lastUpdateDate = Instant.now();
  }
}
