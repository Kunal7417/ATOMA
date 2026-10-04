package com.atoma.marketplace.merchant.entity;

import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.common.entity.BaseEntity;
import com.atoma.marketplace.common.enums.ApplicationWorkflowStatus;
import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.enums.KycStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.common.enums.PayoutMethod;
import com.atoma.marketplace.common.enums.RepresentativeRole;
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
import java.time.Instant;
import java.util.UUID;

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

    @Column(length = 20, unique = true)
    private String applicationNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ApplicationWorkflowStatus applicationWorkflowStatus;

    @Builder.Default
    private int currentWizardStep = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private BusinessType businessType;

    @Column(length = 100)
    private String mainCategory;

    @Column(name = "primary_category_id")
    private UUID primaryCategoryId;

    @Builder.Default
    @Column(name = "application_version", nullable = false)
    private long applicationVersion = 0L;

    @Column(length = 200)
    private String ownerFullName;

    @Column(length = 200)
    private String ownerFatherName;

    @Column(length = 100)
    private String ownerTazkiraNumber;

    private java.time.LocalDate ownerDateOfBirth;

    @Column(length = 100)
    private String province;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RepresentativeRole representativeRole;

    @Column(length = 120)
    private String ownerEmail;

    @Column(length = 100)
    private String buildingNumber;

    @Column(length = 200)
    private String street;

    @Column(length = 100)
    private String district;

    @Column(length = 200)
    private String landmark;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PayoutMethod payoutMethod;

    @Column(length = 100)
    private String bankAccountHint;

    @Column(name = "payout_bank_id", length = 80)
    private String payoutBankId;

    @Column(name = "payout_wallet_provider_id", length = 80)
    private String payoutWalletProviderId;

    @Column(name = "payout_account_name", length = 200)
    private String payoutAccountName;

    @Column(name = "checks_updated_at")
    private Instant checksUpdatedAt;

    private Instant fixByDeadline;

    private Instant reapplyAfter;

    @Column(length = 30)
    private String decisionReference;

    @Column(length = 1000)
    private String suspensionReason;

    @Column(length = 1000)
    private String rejectionReason;

    private java.time.LocalDate licenceExpiry;

    private Instant submittedAt;

    private Instant draftSavedAt;

    @Builder.Default
    private boolean phoneVerified = false;
}
