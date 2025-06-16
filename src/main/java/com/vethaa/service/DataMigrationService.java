package com.vethaa.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vethaa.dao.MigrationDao;
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
		
		List<String> fileNames = getOrderedFileList(masterFilePath);

		for (String fileName : fileNames) {
			log.info("<----------> START <----------> {} <----------> START <---------->", fileName);
			try {
				Path filePath = Paths.get(csvReadFolderPath, fileName);
				if (Files.exists(filePath)) {
					String status = processFile(filePath.toFile());
					fileStatusMap.put(fileName, status);
					if (status != Constants.PROCESS_SUCCESS)
						return fileStatusMap;
				} else {
					fileStatusMap.put(fileName, Constants.FILE_NOT_FOUND);
					return fileStatusMap;
				}
				log.info("<-----------> END <-----------> {} <-----------> END <----------->", fileName);
			} catch (Exception e) {
				fileStatusMap.put(fileName, Constants.FAILURE + " - " + e.getMessage());
				return fileStatusMap;
			}
		}

		return fileStatusMap;
	}

	private String processFile(File file) throws Exception {
		log.info("READING DATA FROM FILE -> {}", file.getName());
		Reader reader = new FileReader(file);
		CSVParser parser = CSVFormat.Builder.create()
				.setHeader()
				.setSkipHeaderRecord(true)
				.get()
				.parse(reader);

		String tableName = file.getName().split("\\.")[0];
		Class<?> entityClass = findEntityClass(entityPackage, tableName);

		log.info("Table headers(column name) parsed from csv file - {}", parser.getHeaderNames());
		log.info("Iterating csv records in file");
		return processData(parser, entityClass, file);
	}

	private void generateCsvFailedEntries(Map<CSVRecord, String> failedRecordErroMsgMap, List<String> csvHeaders,
			String fileName) throws IOException {
		if (!failedRecordErroMsgMap.keySet().isEmpty()) {
			Writer writer = Files.newBufferedWriter(Paths.get(csvWriteFolderPath + FilenameUtils.getBaseName(fileName) + ".csv"));
			CSVFormat csvWriteFormat = CSVFormat.Builder.create()
					.setHeader(csvHeaders.toArray(new String[0]))
					.setDelimiter(',')
					.get();
			CSVPrinter csvPrinter = new CSVPrinter(writer, csvWriteFormat);
			for (CSVRecord csvRecord : failedRecordErroMsgMap.keySet()) {
				csvPrinter.printRecord(csvRecord);
			}
			csvPrinter.close();
			
		    try (BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(csvWriteFolderPath + FilenameUtils.getBaseName(fileName) + "_error_msg.txt"))) {
		        for (Entry<CSVRecord, String> entry : failedRecordErroMsgMap.entrySet()) {
		            String line = entry.getKey() + " : " + entry.getValue();
		            bufferWriter.write(line);
		            bufferWriter.newLine();
		        }
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}

	private String processData(CSVParser parser, Class<?> entityClass, File file) throws Exception {
		Map<CSVRecord, String> failedRecordErroMsgMap = new HashMap<>();
		int count = 0;
		for (CSVRecord record : parser.getRecords()) {
			Object entityObj = mapEntityFields(entityClass, record, parser.getHeaderNames());
			saveToDb(record, entityObj, failedRecordErroMsgMap);
			logCount(++count);
		}

		log.info("FILE PROCESSED SUCCESFULLY | TOTAL RECORDS : {}", count);
		log.info("FAILED RECORDS COUNT : {}", failedRecordErroMsgMap.size());

		generateCsvFailedEntries(failedRecordErroMsgMap, parser.getHeaderNames(), file.getName());

		if (failedRecordErroMsgMap.isEmpty()) {
			return Constants.PROCESS_SUCCESS;
		} else if (failedRecordErroMsgMap.size() == count) {
			return Constants.NO_DATA_INSERTED;
		} else {
			return Constants.PARTIAL_DATA_INSERTED;
		}
	}

	private Map<CSVRecord, String> saveToDb(CSVRecord record, Object entityObj, Map<CSVRecord, String> failedRecordErroMsgMap) {
		log.info("Persisiting data ...");
		try {
			migrationDao.save(entityObj);
		} catch (Exception e) {
			failedRecordErroMsgMap.put(record, e.toString());
		}
		log.info("Data saved");
		return failedRecordErroMsgMap;
	}

	private Object mapEntityFields(Class<?> entityClass, CSVRecord record, List<String> headerNames) throws Exception {
		Object entityObj = entityClass.getDeclaredConstructor().newInstance();
		for (String csvHeader : headerNames) {
			boolean isFieldFound = false;

			for (Field field : entityClass.getDeclaredFields()) {

				if (field.isAnnotationPresent(Column.class)) {
					if (field.getAnnotation(Column.class).name().equalsIgnoreCase(csvHeader)) {
						isFieldFound = true;
						setFieldValue(entityObj, field, record.get(csvHeader));
						break;
					} else if (field.getAnnotation(Column.class).name().equalsIgnoreCase("orgInfo")) {
						isFieldFound = true;
						setFieldValue(entityObj, field, "Vethaa");
						break;
					}

				} else if (field.isAnnotationPresent(JoinColumn.class)) {
					if (field.getAnnotation(JoinColumn.class).name().equalsIgnoreCase(csvHeader)) {
						isFieldFound = true;
						setForeignFieldValue(field, entityObj, record.get(csvHeader), csvHeader);
						break;
					} else if (field.getAnnotation(JoinColumn.class).name().equalsIgnoreCase("org_entity_Id")) {
						isFieldFound = true;
						setForeignFieldValue(field, entityObj, "2", "org_entity_Id");
						break;
					}
				}
			}

			if (!isFieldFound) {
				log.error("Field not found for column header -> {}", csvHeader);
				throw new Exception("Cant able to identify entity field for csv column header name '" + csvHeader + "' in entity -> " + entityClass.getName());
			}
		}
		return entityObj;
	}

	private void setForeignFieldValue(Field field, Object entityObj, String value, String csvHeader) throws Exception {
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

	private void setFieldValue(Object entityObj, Field field, String value)
			throws IllegalArgumentException, IllegalAccessException {
		if (Objects.nonNull(value) && StringUtils.isNotBlank(value)) {
			field.setAccessible(true);
			field.set(entityObj, convertDataType(value, field.getType()));
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

	private static Object convertDataType(String value, Class<?> type) {
		if (type == String.class)
			return value;
		if (type == Integer.class)
			return Integer.valueOf(value);
		if (type == Long.class)
			return Long.valueOf(value);
		if (type == Double.class)
			return Double.valueOf(value);
		if (type == Boolean.class)
			return Boolean.valueOf(value);
		if (type == LocalDate.class)
			return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		if (type == LocalDateTime.class)
			return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		return null;
	}

	private void logCount(int count) {
		boolean shouldLog = false;
		if (count < 10) {
			shouldLog = true;
		} else if (count <= 25 && count % 5 == 0) {
			shouldLog = true;
		} else if (count <= 400 && count % 50 == 0) {
			shouldLog = true;
		} else if (count <= 4000 && count % 250 == 0) {
			shouldLog = true;
		} else if (count <= 20000 && count % 1000 == 0) {
			shouldLog = true;
		} else if (count <= 75000 && count % 2500 == 0) {
			shouldLog = true;
		} else if (count % 100000 == 0) {
			shouldLog = true;
		}

		if (shouldLog) {
			log.info("Records processed {} ", count);
		}
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

	public <T> void save(T entity) {
		migrationDao.save(entity);

	}
}
