package com.example.tasks.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
		int status,
		String error,
		String message,
		String path,
		Instant timestamp
) {
	public ErrorResponseDto(int status, String error, String message, String path) {
		this(status, error, message, path, LocalDateTime.now().atZone(ZoneOffset.UTC).withNano(0).toInstant());
	}
}
