package com.training.cert.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 学员在签发前确认的姓名。未确认不得签发。
 */
@Entity
@Table(name = "name_confirmations")
public class NameConfirmation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "enrollment_id", unique = true)
    public Enrollment enrollment;

    @Column(nullable = false)
    public String confirmedName;

    @Column(nullable = false)
    public Instant confirmedAt;
}
