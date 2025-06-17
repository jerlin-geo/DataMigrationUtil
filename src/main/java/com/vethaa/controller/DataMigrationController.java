package com.vethaa.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vethaa.service.DataMigrationService;
import com.vethaa.util.Constants;

@RestController
public class DataMigrationController {

	@Autowired
	DataMigrationService dataMigrationService;

	@GetMapping("migrateData")
	public ResponseEntity<Map<String, String>> migrateDataAdv() {
		try {
			Map<String, String> message = dataMigrationService.migrateDataGeneric();
			return ResponseEntity.ok(message);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(Map.of(Constants.ERROR, Constants.SOMETHING_WENT_WRONG + " - " + e.toString() ));
		}

	}
	
}