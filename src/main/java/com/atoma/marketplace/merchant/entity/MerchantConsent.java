package com.atoma.marketplace.merchant.entity;

import com.atoma.marketplace.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "merchant_consents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "consent_key"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantConsent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, length = 80)
    private String consentKey;

    @Column(nullable = false, length = 40)
    private String policyVersion;

    @Column(nullable = false)
    private Instant acceptedAt;
}
