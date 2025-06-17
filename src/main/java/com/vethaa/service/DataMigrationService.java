package com.vethaa.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vethaa.dao.MigrationDao;
import com.vethaa.exception.AppException;
import com.vethaa.util.AppUtil;
import com.vethaa.util.Constants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataMigrationService {

	@Autowired
	MigrationDao migrationDao;

	@Value("${util.csv.folder.read.path}")
	private String csvReadFolderPath;

	@Value("${util.csv.folder.write.path}")
	private String csvWriteFolderPath;
	
	@Value("${util.master.file.path}")
	private String masterFilePath;

	@Value("${util.entity.package}")
	private String entityPackage;

	public Map<String, String> migrateDataGeneric() throws Exception {
		Map<String, String> fileStatusMap = new LinkedHashMap<>();
		
		AppUtil.validateFileFolderPath(csvReadFolderPath);
		AppUtil.validateFileFolderPath(csvWriteFolderPath);
		AppUtil.validateFileFolderPath(masterFilePath);
		
		List<String> fileNames = getOrderedFileList(masterFilePath);

		for (String fileName : fileNames) {
			log.info("<----------> START <----------> {} <----------> START <---------->", fileName);
			try {
				Path filePath = Paths.get(csvReadFolderPath, fileName);
				if (Files.exists(filePath)) {
					String status = processFile(filePath.toFile());
					fileStatusMap.put(fileName, status);
					if (!status.equals(Constants.PROCESS_SUCCESS))
						throw new AppException(status);
				} else {
					throw new AppException(Constants.FILE_NOT_FOUND);
				}
				log.info("<-----------> END <-----------> {} <-----------> END <----------->", fileName);
			} catch (Exception e) {
				log.error(Constants.FAILURE + " - " + e.getMessage());
				log.info("<-----------> END <-----------> {} <-----------> END <----------->", fileName);
				fileStatusMap.put(fileName, Constants.FAILURE + " - " + e.getMessage());
				return fileStatusMap;
			}
		}
		return fileStatusMap;
	}

	private String processFile(File file) throws Exception {
		log.info("PROCESSING FILE -> {}", file.getName());
		Map<Field, String> fieldToCsvHeaderMap = new LinkedHashMap<>();
		Map<Field, String> newFieldAndHeaderMap = new LinkedHashMap<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		List<String> csvheaderNames = Arrays.asList(reader.readLine().split(","));
		String tableName = file.getName().split("\\.")[0];
		Class<?> entityClass = findEntityClass(entityPackage, tableName);
		log.info("Table headers(column name) parsed from csv file - {}", csvheaderNames);
		mapFieldAndCsvHeader(fieldToCsvHeaderMap, newFieldAndHeaderMap, entityClass, csvheaderNames);
		return processData(reader,fieldToCsvHeaderMap, newFieldAndHeaderMap, csvheaderNames, entityClass, file);
	}

	private String processData(BufferedReader reader, Map<Field, String> fieldToCsvHeaderMap, Map<Field, String> newFieldAndHeaderMap,
			List<String> headerNames, Class<?> entityClass, File file) throws Exception {
		log.info("Iterating csv records in file");
		Map<String, String> failedRecordErroMsgMap = new HashMap<>();
		int count = 0;
		
		String line = reader.readLine();
		if (line == null)
			return Constants.EMPTY_FILE;
		
		while (line != null) {
			Object entityObj = mapEntityFields(entityClass, line, headerNames, fieldToCsvHeaderMap, newFieldAndHeaderMap);
			saveToDb(line, entityObj, failedRecordErroMsgMap);
			AppUtil.logCount(++count);
			line = reader.readLine();
		}
		
		log.info("FILE PROCESSED SUCCESFULLY | TOTAL RECORDS : {}", count);
		log.info("FAILED RECORDS COUNT : {}", failedRecordErroMsgMap.size());

		generateCsvFailedEntries(failedRecordErroMsgMap, headerNames, file.getName());

		if (failedRecordErroMsgMap.isEmpty()) {
			return Constants.PROCESS_SUCCESS;
		} else if (failedRecordErroMsgMap.size() == count) {
			return Constants.ALL_DATA_FAILED_INSERTED;
		} else {
			return Constants.PARTIAL_DATA_INSERTED;
		}
	}

	private Object mapEntityFields(Class<?> entityClass, String line, List<String> headerNames,
			Map<Field, String> fieldToCsvHeaderMap, Map<Field, String> newFieldAndHeaderMap) throws Exception {
		Object entityObj = entityClass.getDeclaredConstructor().newInstance();
		Map<String, String> rowData = getRowData(headerNames, line);
		
		for (Entry<Field, String> entry : fieldToCsvHeaderMap.entrySet()) {
			Field field = entry.getKey();
			String csvHeader = entry.getValue();
			if (field.isAnnotationPresent(Column.class)) {
				setFieldValue(entityObj, field, rowData.get(csvHeader));
			} else if (field.isAnnotationPresent(JoinColumn.class)) {
				setForeignFieldValue(field, entityObj, rowData.get(csvHeader));
			}
		}
		
		for (Entry<Field, String> entry : newFieldAndHeaderMap.entrySet()) {
			Field field = entry.getKey();
			String csvHeader = entry.getValue();
			if (field.isAnnotationPresent(Column.class)) {
				setFieldValue(entityObj, field, getNewFieldVaue(csvHeader));
			} else if (field.isAnnotationPresent(JoinColumn.class)) {
				setForeignFieldValue(field, entityObj, getNewFieldVaue(csvHeader));
			}
		}
		return entityObj;
	}

	private void saveToDb(String line, Object entityObj, Map<String, String> failedRecordErroMsgMap) {
		try {
			migrationDao.save(entityObj);
			log.debug("Data saved");
		} catch (Exception e) {
			log.debug("Data saving failed");
			failedRecordErroMsgMap.put(line, e.toString());
		}
	}
	
	private void generateCsvFailedEntries(Map<String, String> failedRecordErroMsgMap, List<String> headerNames,
			String fileName) throws IOException {
		if (!failedRecordErroMsgMap.keySet().isEmpty()) {
			try (BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(csvWriteFolderPath + AppUtil.getBaseFileName(fileName) + ".csv"))) {
				bufferWriter.append(String.join(",", headerNames));
				bufferWriter.append("\n");

	            for (String failedRows : failedRecordErroMsgMap.keySet()) {
	            	bufferWriter.append(failedRows);
	            	bufferWriter.append("\n");
	            }
	        }
	    }

		try (BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(csvWriteFolderPath + AppUtil.getBaseFileName(fileName) + "_error_msg.txt"))) {
			for (Entry<String, String> entry : failedRecordErroMsgMap.entrySet()) {
				String line = entry.getKey() + " : " + entry.getValue();
				bufferWriter.write(line);
				bufferWriter.newLine();
			}
		}
	}

	private Map<String, String> getRowData(List<String> headerNames, String line) {
		Map<String, String> rowDataMap = new LinkedHashMap<>();
		String[] values = line.split(",");
		for (int i = 0; i < headerNames.size(); i++) {
			String header = headerNames.get(i);
			String value = values.length > i ? values[i].trim() : null;
			rowDataMap.put(header, value);
		}
		return rowDataMap;
	}

	private Map<Field, String> mapFieldAndCsvHeader(Map<Field, String> fieldToHeaderMap, Map<Field, String> newFieldToHeaderMap, Class<?> entityClass, List<String> csvHeaderNames) throws Exception {
		
		for (Field field : entityClass.getDeclaredFields()) {
			boolean fieldFound = false;
			for (String csvHeaderName: csvHeaderNames) {
				if ((field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).name().equalsIgnoreCase(csvHeaderName))
						|| (field.isAnnotationPresent(JoinColumn.class) && field.getAnnotation(JoinColumn.class).name().equalsIgnoreCase(csvHeaderName))) {
					fieldToHeaderMap.put(field, csvHeaderName.toLowerCase());
					fieldFound = true;
					break;
				}
			}
			
			if (!fieldFound) {
				if (field.isAnnotationPresent(Column.class)) {
					String columnName = field.getAnnotation(Column.class).name().toLowerCase();
					if (Constants.NEW_FIELD_NAMES.contains(columnName)) {
						newFieldToHeaderMap.put(field, columnName.toLowerCase());
						fieldFound = true;
					} 
				} else if (field.isAnnotationPresent(JoinColumn.class)) {
					String columnName = field.getAnnotation(JoinColumn.class).name().toLowerCase();
					if (Constants.NEW_FIELD_NAMES.contains(columnName)) {
						newFieldToHeaderMap.put(field, columnName.toLowerCase());
						fieldFound = true;
					}
				}
			}
			
			if (!fieldFound) {
				throw new AppException("Cannot map entity field : '" + field.getName() + "' in entity '" + entityClass.getSimpleName() + "'");
			}
		}
		return fieldToHeaderMap;

	}

	private void setForeignFieldValue(Field field, Object entityObj, String value) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (Objects.nonNull(value) && StringUtils.isNotBlank(value)) {
			Class<?> fkEntityClass = field.getType();
			Object fkEntityObj = fkEntityClass.getDeclaredConstructor().newInstance();
			for (Field fkField : fkEntityClass.getDeclaredFields()) {
				if (fkField.isAnnotationPresent(Id.class)) {
					setFieldValue(fkEntityObj, fkField, value);
					break;
				}
			}
			field.setAccessible(true);
			field.set(entityObj, fkEntityObj);
		}
	}

	private void setFieldValue(Object entityObj, Field field, String value) throws IllegalArgumentException, IllegalAccessException {
		if (Objects.nonNull(value) && StringUtils.isNotBlank(value)) {
			field.setAccessible(true);
			field.set(entityObj, AppUtil.convertDataType(value, field.getType()));
		}
	}

	private Class<?> findEntityClass(String packageName, String tableName) throws Exception {
		log.info("Searching java entities for the table name - {}", tableName);
		Reflections reflections = new Reflections(packageName);
		Class<?> entityClass = reflections.getTypesAnnotatedWith(Entity.class).stream()
				.filter(clazz -> clazz.getAnnotation(Table.class).name().equalsIgnoreCase(tableName)).findFirst()
				.orElseThrow(() -> new Exception("Entity for table name '" + tableName + "' not found"));
		log.info("Table entity found --> {}", entityClass.getName());
		return entityClass;
	}

	private static List<String> getOrderedFileList(String masterFilePath) throws IOException {
		List<String> fileNames = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(masterFilePath))) {
			String line = br.readLine();
			while (line != null) {
				fileNames.add(line.trim());
				line = br.readLine();
			}
		}
		fileNames.replaceAll(fileName -> fileName + ".csv");
		return fileNames;
	}
	
	private String getNewFieldVaue(String csvHeader) throws AppException {
		switch (csvHeader) {
		case "org_id": {
			return "1";
		}
		default:
			throw new AppException(Constants.NO_VALUE_FOR_NEW_FIELD + " " + csvHeader);
		}
	}

	public <T> void save(T entity) {
		migrationDao.save(entity);
	}
}
