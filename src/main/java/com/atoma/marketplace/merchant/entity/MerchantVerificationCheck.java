package com.atoma.marketplace.merchant.entity;

import com.atoma.marketplace.common.entity.BaseEntity;
import com.atoma.marketplace.common.enums.VerificationCheckStatus;
import com.atoma.marketplace.common.enums.VerificationCheckType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "merchant_verification_checks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "check_type"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantVerificationCheck extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationCheckType checkType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationCheckStatus status;

    private Instant lastCheckedAt;
}
