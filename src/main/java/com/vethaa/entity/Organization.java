package com.vethaa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "organization")
@Getter
@Setter
@ToString
public class Organization {

	@Id
	@Column(name = "organization_id")
	private Integer orgId;
	
	@Column(name = "org_Name")
	private String orgName;
}
