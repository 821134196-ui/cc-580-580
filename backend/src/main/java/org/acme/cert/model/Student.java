package org.acme.cert.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "student_no", nullable = false, unique = true, length = 32)
    public String studentNo;

    @Column(name = "full_name", nullable = false, length = 64)
    public String fullName;

    @Column(name = "email", nullable = false, length = 128)
    public String email;

    /** 证件号，更正姓名时用于核对身份 */
    @Column(name = "id_card_no", nullable = false, length = 32)
    public String idCardNo;
}
