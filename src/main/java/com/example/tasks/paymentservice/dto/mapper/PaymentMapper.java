package com.example.tasks.paymentservice.dto.mapper;

import com.example.tasks.paymentservice.dto.PaymentRequestDto;
import com.example.tasks.paymentservice.dto.PaymentResponseDto;
import com.example.tasks.paymentservice.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "timestamp", ignore = true)
	@Mapping(target = "processorTransactionId", ignore = true)
	@Mapping(target = "errorCode", ignore = true)
	@Mapping(target = "errorMessage", ignore = true)
	@Mapping(source = "amount", target = "paymentAmount")
	Payment toEntity(PaymentRequestDto requestDto);

	@Mapping(source = "paymentAmount", target = "amount")
	PaymentResponseDto toDto(Payment entity);
}
