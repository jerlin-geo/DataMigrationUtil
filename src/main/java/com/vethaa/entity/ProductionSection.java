package com.vethaa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "production_section")
@Data
public class ProductionSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_ID")
    private int sectionId;

    @Column(name = "section_Name")
    private String sectionName;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "vendor_id")
    private Integer vendorId;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
