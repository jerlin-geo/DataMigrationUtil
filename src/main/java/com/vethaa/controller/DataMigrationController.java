package com.vethaa.controller;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vethaa.dao.MigrationDao;
import com.vethaa.entity.ProductGroup;
import com.vethaa.entity.ProductionSection;
import com.vethaa.service.DataMigrationService;

@RestController
public class DataMigrationController {

	@Autowired
	MigrationDao migrationDao;
	
	@Autowired
	DataMigrationService dataMigrationService;

	@GetMapping("demo")
	public String test() {
		try {
			ProductionSection productionSection = new ProductionSection();
//			productionSection.setSectionId(1);
			productionSection.setSectionName(null);
			productionSection.setVendorId(2);
			productionSection.setCreatedBy(1);
			productionSection.setModifiedDate(LocalDateTime.now());
			dataMigrationService.save(productionSection);
			return "SUCCESS";
		} catch (Exception e) {
			e.printStackTrace();
			return "FAILURE";
		}
	}
	
	@GetMapping("demo1")
	public String test1() {
		try {
			ProductGroup productGroup = new ProductGroup();
			productGroup.setCreatedBy(1);
			productGroup.setModifiedDate(null);
			productGroup.setProductGroupId(null);
			productGroup.setProductGroupName("Group Name Test");
//			ProductionSection productionSection = new ProductionSection();
//			productionSection.setSectionId(1);
//			productGroup.setProductionSection(productionSection);
			productGroup.setSortOrder(2);
			productGroup.setVendorId(2);
			dataMigrationService.save(productGroup);
			return "SUCCESS";
		} catch (Exception e) {
			e.printStackTrace();
			return "FAILURE";
		}
	}

	@GetMapping("democsv")
	public String migrateData() throws IOException {
		try {
			String filePath = "C:/Users/gerli/Desktop/Vetha/csv/prod_section.csv";
			dataMigrationService.migrateData(filePath);
			return "SUCCESS";

		} catch (Exception e) {
			e.printStackTrace();
			return "FAILURE | " + e.toString();
		}

	}

	@GetMapping("democsvadv")
	public String migrateDataAdv() throws IOException {
		try {
			String filePath = "C:/Users/gerli/Desktop/Vetha/csv/production_section.csv";
//			String filePath = "C:/Users/gerli/Desktop/Vetha/csv/product_group.csv";
			String message = dataMigrationService.migrateDataGeneric(filePath);
			return message;

		} catch (Exception e) {
			e.printStackTrace();
			return "FAILURE";
		}

	}
	
}