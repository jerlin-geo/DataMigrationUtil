package com.vethaa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "category_master")
@Getter
@Setter
public class CategoryMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "category_ID")
	private Integer categoryId;

	@Column(name = "category_Name")
	private String categoryName;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "vendor_id")
	private Integer vendorId;

	@Column(name = "modified_date")
	private LocalDateTime modifiedDate;
}
