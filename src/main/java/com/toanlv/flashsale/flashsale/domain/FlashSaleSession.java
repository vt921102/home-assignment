package com.toanlv.flashsale.flashsale.domain;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "flash_sale_sessions",
        indexes = @Index(
                name = "idx_flash_sale_sessions_active_date",
                columnList = "sale_date, is_active"
        )
)
public class FlashSaleSession {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<FlashSaleSessionItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ----------------------------------------------------------------
    // Factory
    // ----------------------------------------------------------------

    public static FlashSaleSession create(
            String name,
            LocalDate saleDate,
            LocalTime startTime,
            LocalTime endTime) {
        var session = new FlashSaleSession();
        session.name      = name;
        session.saleDate  = saleDate;
        session.startTime = startTime;
        session.endTime   = endTime;
        session.active    = true;
        return session;
    }

    // ----------------------------------------------------------------
    // Business methods
    // ----------------------------------------------------------------

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public boolean isWithinWindow(LocalDate date, LocalTime time) {
        return this.active
                && this.saleDate.equals(date)
                && !time.isBefore(this.startTime)
                && time.isBefore(this.endTime);
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public UUID getId()                          { return id;        }
    public String getName()                      { return name;      }
    public LocalDate getSaleDate()               { return saleDate;  }
    public LocalTime getStartTime()              { return startTime; }
    public LocalTime getEndTime()                { return endTime;   }
    public boolean isActive()                    { return active;    }
    public List<FlashSaleSessionItem> getItems() {
        return Collections.unmodifiableList(items);
    }
    public Instant getCreatedAt()                { return createdAt; }
}
