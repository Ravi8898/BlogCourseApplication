package org.project.model;

import jakarta.persistence.*;
import lombok.*;
import org.project.model.audit.BaseAuditEntity;

import java.util.Date;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String assignedTo;

    private Date invoiceDate;
}
