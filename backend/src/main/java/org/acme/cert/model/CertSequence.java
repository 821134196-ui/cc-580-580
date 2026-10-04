package org.acme.cert.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 证书编号发号器。签发事务内先 UPDATE 本表（SQLite 写锁使并发签发串行化），
 * 再读取序号，配合 cert_number 唯一索引，保证编号绝不重复。
 */
@Entity
@Table(name = "cert_sequence")
public class CertSequence {

    @Id
    @Column(name = "name", length = 32)
    public String name;

    @Column(name = "next_value", nullable = false)
    public long nextValue;
}
