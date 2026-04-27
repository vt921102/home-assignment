package com.toanlv.flashsale.flashsale.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "user_daily_purchase_limits",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_daily_purchase_limits",
                columnNames = {"user_id", "purchase_date"}
        )
)
public class UserDailyPurchaseLimit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "purchase_count", nullable = false)
    private int purchaseCount = 1;

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public UUID getId()              { return id;            }
    public UUID getUserId()          { return userId;        }
    public LocalDate getPurchaseDate(){ return purchaseDate; }
    public int getPurchaseCount()    { return purchaseCount; }
}
