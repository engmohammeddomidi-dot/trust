package com.trust.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "health_score_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false)
    private LocalDate scoreDate;

    private double salesScore;
    private double profitScore;
    private double pricingScore;
    private double purchasesScore;
    private double inventoryScore;
    private double liquidityScore;
    private double totalScore;
}
