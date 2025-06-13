package com.vethaa.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_master")
@Getter
@Setter
public class ProductMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_ID")
	private Integer productId;

	@Column(name = "product_Code")
	private String productCode;

	@Column(name = "hsn_Code")
	private String hsnCode;

	@Column(name = "product_Desc")
	private String productDesc;

	@ManyToOne
	@JoinColumn(name = "product_Type_ID", referencedColumnName = "product_Group_ID")
	private ProductGroup productType;

	@ManyToOne
	@JoinColumn(name = "category_ID", referencedColumnName = "category_ID")
	private CategoryMaster category;

	@ManyToOne
	@JoinColumn(name = "groupName_Id", referencedColumnName = "prodGroup_Name_Id")
	private ProdGroupNameMaster groupName;

	@ManyToOne
	@JoinColumn(name = "selling_UOM_ID", referencedColumnName = "uom_ID")
	private UomMaster sellingUom;

	@ManyToOne
	@JoinColumn(name = "qty_UOM_ID", referencedColumnName = "uom_ID")
	private UomMaster qtyUom;

	@Column(name = "qty_InUOM")
	private Integer qtyInUom;

	@ManyToOne
	@JoinColumn(name = "crate_Type", referencedColumnName = "crate_ID")
	private CrateMaster crateType;

	@Column(name = "crate_Qty")
	private Integer crateQty;

	@Column(name = "sorting_No")
	private Integer sortingNo;

	@Column(name = "tax_Percent", precision = 18, scale = 2)
	private Double taxPercent;

	@Column(name = "selling_Rate", precision = 18, scale = 2)
	private Double sellingRate;

	@Column(name = "active_Flag")
	private Boolean activeFlag;

	@Column(name = "prod_Weight", precision = 18, scale = 3)
	private Double prodWeight;

	@Column(name = "billSorting_No")
	private Integer billSortingNo;

	@Column(name = "repSorting_No")
	private Integer repSortingNo;

	@Column(name = "rate_Per")
	private String ratePer;

	@Column(name = "is_Leakable")
	private Boolean isLeakable;

	@Column(name = "created_By")
	private Integer createdBy;

	@Column(name = "created_Date")
	private LocalDate createdDate;

	@Column(name = "modified_By")
	private Integer modifiedBy;

	@Column(name = "modified_Date")
	private LocalDate modifiedDate;

	@Column(name = "advanceStock_Unit", precision = 18, scale = 2)
	private Double advanceStockUnit;

	@Column(name = "advanceStock_Crate", precision = 18, scale = 2)
	private Double advanceStockCrate;

	@Column(name = "adjustment", precision = 18, scale = 2)
	private Double adjustment;

	@Column(name = "sale_Acc")
	private Integer saleAcc;

	@Column(name = "purc_Acc")
	private Integer purcAcc;

	@Column(name = "input_CGST")
	private Integer inputCGST;

	@Column(name = "input_SGST")
	private Integer inputSGST;

	@Column(name = "input_IGST")
	private Integer inputIGST;

	@Column(name = "output_CGST")
	private Integer outputCGST;

	@Column(name = "output_SGST")
	private Integer outputSGST;

	@Column(name = "output_IGST")
	private Integer outputIGST;

	@Column(name = "fat", precision = 18, scale = 8)
	private Double fat;

	@Column(name = "snf", precision = 18, scale = 8)
	private Double snf;

	@Column(name = "is_Ghee")
	private Boolean isGhee;

	@Column(name = "lr", precision = 18, scale = 2)
	private Double lr;

	@Column(name = "vendor_id")
	private Integer vendorId;
}
