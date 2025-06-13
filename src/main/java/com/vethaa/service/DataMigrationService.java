package com.vethaa.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
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

	public String migrateData(String filePath) throws IOException {
		
		Reader reader = new FileReader(filePath);
		CSVParser parser =CSVFormat.Builder.create()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader);
		List<String> headers = parser.getHeaderNames();

		List<Map<String, String>> rows = new ArrayList<>();
		for (CSVRecord record : parser) {
			Map<String, String> row = new LinkedHashMap<>();
			for (String header : headers) {
				row.put(header, record.get(header));
			}
			rows.add(row);
		}
		
		System.out.println(rows);
		
		return "SUCCESS";
	}
	
	public String migrateDataGeneric(String filePath) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		String packageName = "com.vethaa.entity";
		File file = new File(filePath);
		String tableName = file.getName().split("\\.")[0];
		
        Reader reader = new FileReader(file);
		CSVParser parser = CSVFormat.Builder.create()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader);
		log.info("File read successfully");
		
		log.info("Searching java entities for the table name - {}", tableName);
		Class<?> entityClass = null;
        Reflections reflections = new Reflections(packageName);
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Entity.class)) {
        	Table table = clazz.getAnnotation(Table.class);
			if (table.name().equalsIgnoreCase(tableName)) {
				entityClass = clazz;
				log.info("Table entity found --> {}", entityClass.getName());
				break;
			}
		}

		if (entityClass == null) {
			log.error("Java entity not found for table name - {}", tableName);
			return "Entity for table name '" + tableName + "' not found";
		}

		log.info("Table headers(column name) parsed from csv file - {}", parser.getHeaderNames());
		List<Object> entityList = new ArrayList<>();
		for (CSVRecord record : parser) {
			Object entityObj = entityClass.getDeclaredConstructor().newInstance();
			
			for (String csvHeader : parser.getHeaderNames()) {
				boolean fieldFound = false;
				
				for (Field field : entityClass.getDeclaredFields()) {
					
					if (field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).name().equalsIgnoreCase(csvHeader)) {
						fieldFound = true;
						
						if (record.get(csvHeader) != null && StringUtils.isNotBlank(record.get(csvHeader))) {
							field.setAccessible(true);
							field.set(entityObj, convert(record.get(csvHeader), field.getType()));
						}
						break;
						
					} else if (field.isAnnotationPresent(JoinColumn.class) && field.getAnnotation(JoinColumn.class).name().equalsIgnoreCase(csvHeader)) {
						fieldFound = true;
						
						if (record.get(csvHeader) != null && StringUtils.isNotBlank(record.get(csvHeader))) {
							Class<?> fkEntityClass = field.getType();
							Object fkEntityObj = fkEntityClass.getDeclaredConstructor().newInstance();
							for (Field f : fkEntityClass.getDeclaredFields()) {
						        if (f.isAnnotationPresent(Id.class)) {
						        	f.setAccessible(true);
									f.set(fkEntityObj, convert(record.get(csvHeader), f.getType()));
									break;
						        } else {
						        	log.error("Field not found for column header (foreign key) -> {}", csvHeader);
									return "Cant able to find foreign entity id field for csv column header (foreign key) '" + csvHeader + "' in foreign entity -> " + fkEntityClass.getName();
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
				
				if (!fieldFound) {
					log.error("Field not found for column header -> {}", csvHeader);
					return "Cant able to find entity field for csv column header '" + csvHeader + "' in entity -> " + entityClass.getName();
				}
			}
			
			entityList.add(entityObj);
			try {
        		migrationDao.save(entityObj);
			} catch (Exception e) {
				log.error(e.toString());
//				log.info(tableName);
				String outputFilePath = "C:/Users/gerli/Desktop/Vetha/csv/production_section_fail.csv";
				Writer writer = Files.newBufferedWriter(Paths.get(outputFilePath));
				CSVPrinter csvPrinter = new CSVPrinter(writer,
						CSVFormat.Builder.create().setHeader(parser.getHeaderNames().toArray(new String[0]))
								.setQuoteMode(QuoteMode.ALL).setDelimiter(',').get());
				csvPrinter.print(record.toList().toArray());
				csvPrinter.flush();
			}
		}

		
		// TODO PERSIST ONE BY ONE , SAVE FAILED DATA IN FILE
        System.out.println(entityList);
		return "SUCCESS";
	}
	
	private static Object convert(String value, Class<?> type) {
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

		if (type == LocalDate.class) {
			return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		}

		if (type == LocalDateTime.class) {
			return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		}
		return null;
	}

	public <T> void save(T entity) {
		migrationDao.save(entity);
		
	}
}
