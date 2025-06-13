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
@Table(name = "stockcategory_master")
@Getter
@Setter
public class StockCategoryMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stockCat_Id")
	private int stockCatId;

	@Column(name = "stockCat_Name")
	private String stockCatName;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "vendor_id")
	private Integer vendorId;

	@Column(name = "modified_date")
	private LocalDateTime modifiedDate;
}
