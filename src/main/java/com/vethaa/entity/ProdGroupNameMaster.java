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
@Table(name = "prodgroup_name_master")
@Getter
@Setter
public class ProdGroupNameMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "prodGroup_Name_Id")
	private Integer prodGroupNameId;

	@Column(name = "prodGroup_Name")
	private String prodGroupName;

	@Column(name = "sort_No")
	private Integer sortNo;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "vendor_id")
	private Integer vendorId;

	@Column(name = "modified_date")
	private LocalDateTime modifiedDate;
}
