package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Email contact entity - maps to emailtab table.
 * Used for bulk email distribution.
 */
@Entity
@Table(name = "emailtab")
@Data
@NoArgsConstructor
public class EmailContact {

    @Id
    @Column(name = "NIC", length = 12)
    private String nic;

    @Column(name = "EmpNo", length = 5)
    private String empNo;

    @Column(name = "Email", length = 100)
    private String email;
}
