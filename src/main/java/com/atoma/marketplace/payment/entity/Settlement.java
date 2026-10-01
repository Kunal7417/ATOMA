package com.atoma.marketplace.payment.entity;

import com.atoma.marketplace.common.entity.BaseEntity;
import com.atoma.marketplace.common.enums.SettlementStatus;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal platformCommission;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal merchantNetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status;

    @Column(length = 100)
    private String walletSettlementRef;
}
