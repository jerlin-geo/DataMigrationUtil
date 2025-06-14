package com.vethaa.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
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
	
	@Value("${util.entity.package}")
	private String entityPackage;

	public Map<String, String> migrateDataGeneric() throws Exception {
		Map<String, String> fileStatusMap = new HashMap<>();
		File folder = new File(csvReadFolderPath);
		File[] files = folder.listFiles();

		for (File file : files) {
		    if (file.isFile()) {
		    	log.info("<-------------> START <-------------> {} <-------------> START <------------->", file.getName());
		    	try {
		    		String status = processFile(file);
		    		fileStatusMap.put(file.getName(), status);
				} catch (Exception e) {
					fileStatusMap.put(file.getName(),"FAILURE - " + e.getMessage());
				}
		    	log.info("<--------------> END <--------------> {} <--------------> END <-------------->", file.getName());

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
		if (entityClass == null) {
			log.error("Java entity not found for table name - {}", tableName);
			throw new Exception("Entity for table name '" + tableName + "' not found");
		}

		log.info("Table headers(column name) parsed from csv file - {}", parser.getHeaderNames());
		log.info("Iterating csv records in file");
		Map<CSVRecord, Object> csvRecordEntityObjMap = convertFileDataToEntity(parser, entityClass);
		log.info("FILE READ SUCCESFULLY | TOTAL RECORDS : {}", csvRecordEntityObjMap.size());
		
        Map<CSVRecord, String> failedRecordErroMsgMap = saveToDb(csvRecordEntityObjMap);
		log.info("FAILED RECORDS COUNT : {}", failedRecordErroMsgMap.keySet().size());
		
		generateCsvFailedEntries(failedRecordErroMsgMap, parser.getHeaderNames(), file.getName());
		
		if (failedRecordErroMsgMap.keySet().isEmpty()) {
			return "ALL DATA INSERTED";
		} else if (failedRecordErroMsgMap.keySet().size() == csvRecordEntityObjMap.size()) {
			return "NO DATA INSERTED";
		} else {
			return "PARTIAL DATA INSERTED";
		}
	}

	private void generateCsvFailedEntries(Map<CSVRecord, String> failedRecordErroMsgMap, List<String> csvHeaders, String fileName) throws IOException {
		if (!failedRecordErroMsgMap.keySet().isEmpty()) {
			Writer writer = Files.newBufferedWriter(Paths.get(csvWriteFolderPath + FilenameUtils.getBaseName(fileName) + "_failed.csv"));
			CSVFormat csvWriteFormat = CSVFormat.Builder.create()
					.setHeader(csvHeaders.toArray(new String[0]))
					.setQuoteMode(QuoteMode.ALL)
					.setDelimiter(',')
					.get();
			CSVPrinter csvPrinter = new CSVPrinter(writer, csvWriteFormat);
			
			for (CSVRecord csvRecord : failedRecordErroMsgMap.keySet()) {
				csvPrinter.printRecord(csvRecord);
			}
			csvPrinter.flush();
			csvPrinter.close();
		}
		
	}

	private Map<CSVRecord, String> saveToDb(Map<CSVRecord, Object> csvRecordEntityObjMap) {
		log.info("Persisiting data ...");
		Map<CSVRecord, String> failedRecordErroMsgMap = new HashMap<>();
		for (Entry<CSVRecord, Object> entry : csvRecordEntityObjMap.entrySet()) {
			try {
				migrationDao.save(entry.getValue());
			} catch (Exception e) {
				failedRecordErroMsgMap.put(entry.getKey(), e.toString());
			}
		}
		log.info("Data saved");
		return failedRecordErroMsgMap;
	}

	private Map<CSVRecord, Object> convertFileDataToEntity(CSVParser parser, Class<?> entityClass) throws Exception {
		Map<CSVRecord, Object> csvRecordEntityObjMap = new HashMap<>();
		for (CSVRecord record : parser.getRecords()) {
			Object entityObj = entityClass.getDeclaredConstructor().newInstance();
			
			for (String csvHeader : parser.getHeaderNames()) {
				boolean isFieldFound = false;
				
				for (Field field : entityClass.getDeclaredFields()) {
					if (field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).name().equalsIgnoreCase(csvHeader)) {
						isFieldFound = true;
						if (record.get(csvHeader) != null && StringUtils.isNotBlank(record.get(csvHeader))) {
							setFieldValue(entityObj, field, record.get(csvHeader));
						}
						break;
						
					} else if (field.isAnnotationPresent(JoinColumn.class) && field.getAnnotation(JoinColumn.class).name().equalsIgnoreCase(csvHeader)) {
						isFieldFound = true;
						if (record.get(csvHeader) != null && StringUtils.isNotBlank(record.get(csvHeader))) {
							Class<?> fkEntityClass = field.getType();
							Object fkEntityObj = fkEntityClass.getDeclaredConstructor().newInstance();
							for (Field fkField : fkEntityClass.getDeclaredFields()) {
						        if (fkField.isAnnotationPresent(Id.class)) {
						        	setFieldValue(fkEntityObj, fkField, record.get(csvHeader));
									break;
						        } else {
						        	log.error("Field not found for column header (foreign key) -> {}", csvHeader);
									throw new Exception("Cant able to find foreign entity id field for csv column header (foreign key) '" + csvHeader + "' in foreign entity -> " + fkEntityClass.getName());
						        }
							}
							field.setAccessible(true);
							field.set(entityObj, fkEntityObj);
						}
						break;
					} else {
						//TODO org entity , created by, org id , modified by
					}
				}
				
				if (!isFieldFound) {
					log.error("Field not found for column header -> {}", csvHeader);
					throw new Exception("Cant able to find entity field for csv column header '" + csvHeader + "' in entity -> " + entityClass.getName());
				}
			}
			csvRecordEntityObjMap.put(record, entityObj);
		}
		return csvRecordEntityObjMap;
	}

	private void setFieldValue(Object entityObj, Field field, String value) throws IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		field.set(entityObj, convertDataType(value, field.getType()));
	}

	private Class<?> findEntityClass(String packageName, String tableName) {
		log.info("Searching java entities for the table name - {}", tableName);
		Reflections reflections = new Reflections(packageName);
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Entity.class)) {
        	Table table = clazz.getAnnotation(Table.class);
			if (table.name().equalsIgnoreCase(tableName)) {
				log.info("Table entity found --> {}", clazz.getName());
				return clazz;
			}
		}
		return null;
	}

	private static Object convertDataType(String value, Class<?> type) {
		if (type == String.class)
			return value;
		if (type == int.class || type == Integer.class)
			return Integer.parseInt(value);
		if (type == long.class || type == Long.class)
			return Long.parseLong(value);
		if (type == double.class || type == Double.class)
			return Double.parseDouble(value);
		if (type == boolean.class || type == Boolean.class)
			return Boolean.parseBoolean(value);
		if (type == LocalDate.class)
			return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		if (type == LocalDateTime.class)
			return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		return null;
	}

	public <T> void save(T entity) {
		migrationDao.save(entity);
		
	}
}
