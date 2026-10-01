package com.atoma.marketplace.merchant.entity;

import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.common.entity.BaseEntity;
import com.atoma.marketplace.common.enums.KycStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, unique = true)
    private User owner;

    @Column(nullable = false, length = 200)
    private String businessName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 100)
    private String tinNumber;

    @Column(length = 100)
    private String businessLicenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MerchantStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KycStatus kycStatus;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String riskScore;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("5.00");

    @Column(nullable = false)
    @Builder.Default
    private boolean provisionalActive = false;
}
