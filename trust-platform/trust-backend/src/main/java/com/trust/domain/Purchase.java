package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/** سجل عملية شراء من مورد - القسم 7.4 و 7.10 من خطة MVP */
@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private String supplierName;

    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false)
    private double costPrice;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    public double getTotalCost() {
        return quantity * costPrice;
    }
}
