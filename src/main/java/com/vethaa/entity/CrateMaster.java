package com.vethaa.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "crate_master")
@Getter
@Setter
public class CrateMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "crate_ID")
	private Integer crateId;

	@Column(name = "crate_Type")
	private String crateType;

	@Column(name = "isReturnable")
	private Boolean isReturnable;

	@Column(name = "qty_UOM")
	private Double qtyUom;

	@Column(name = "op_Bal")
	private Integer opBal;

	@Column(name = "created_By")
	private Integer createdBy;

	@Column(name = "created_Date")
	private LocalDate createdDate;

	@Column(name = "modified_By")
	private Integer modifiedBy;

	@Column(name = "modified_Date")
	private LocalDate modifiedDate;

	@Column(name = "active_Flag")
	private Boolean activeFlag;

}
