package com.vethaa.util;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.vethaa.exception.AppException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppUtil {

	public static String getBaseFileName(String fileNameWithExtension) {
		return fileNameWithExtension.contains(".")
				? fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf('.'))
				: fileNameWithExtension;
	}

	public static Object convertDataType(String value, Class<?> type) {
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

	public static void logCount(int count) {
		boolean logCount = count < 10 || (count <= 25 && count % 5 == 0) || (count <= 400 && count % 50 == 0)
				|| (count <= 4000 && count % 250 == 0) || (count <= 20000 && count % 1000 == 0)
				|| (count <= 75000 && count % 2500 == 0) || (count % 100000 == 0);

		if (logCount) {
			log.info("Records processed {}", count);
		}
	}

	public static void validateFileFolderPath(String path) throws AppException {
		File file = new File(path);
		if (!file.exists()) {
			throw new AppException(Constants.INCORRECT_FILE_FOLDER_PATH + " '" + path + "'");
		}
	}
}
