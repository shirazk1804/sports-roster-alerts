package sportsalerts;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "push_receipt_tickets",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = "receipt_id"
        )
    }
)
public class PushReceiptTicket {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        name = "receipt_id",
        nullable = false,
        unique = true
    )
    private String receiptId;

    @Column(
        name = "push_token",
        nullable = false,
        length = 512
    )
    private String pushToken;

    @Column(
        name = "created_at",
        nullable = false
    )
    private LocalDateTime createdAt;

    public PushReceiptTicket() {
    }

    public PushReceiptTicket(
        String receiptId,
        String pushToken
    ) {
        this.receiptId = receiptId;
        this.pushToken = pushToken;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}