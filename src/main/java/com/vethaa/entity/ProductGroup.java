package com.vethaa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_group")
@Getter
@Setter
@ToString
public class ProductGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_Group_ID")
	private Integer productGroupId;

	@Column(name = "product_Group_Name", length = 50)
	private String productGroupName;

	@Column(name = "sort_Order", precision = 10, scale = 0)
	private Integer sortOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "production_Section", foreignKey = @ForeignKey(name = "fk_production_Section"))
	private ProductionSection productionSection;

	@Column(name = "use_For_MB")
	private Boolean useForMB;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "vendor_id")
	private Integer vendorId;

	@Column(name = "modified_date")
	private LocalDateTime modifiedDate;
}
