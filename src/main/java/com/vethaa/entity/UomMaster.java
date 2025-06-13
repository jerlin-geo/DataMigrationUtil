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
@Table(name = "uom_Master")
@Getter
@Setter
public class UomMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "uom_ID")
	private int uomId;

	@Column(name = "uom_Name")
	private String uomName;

	@Column(name = "uom_Short_Name")
	private String uomShortName;

	@Column(name = "alternate_UOM_ID")
	private Short alternateUomId;

	@Column(name = "alternate_UOM_Qty")
	private Double alternateUomQty;

	@Column(name = "tally_UOM")
	private String tallyUom;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "vendor_id")
	private Integer vendorId;

	@Column(name = "modified_date")
	private LocalDateTime modifiedDate;
}
