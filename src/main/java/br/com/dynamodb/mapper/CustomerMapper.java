package br.com.dynamodb.mapper;

import br.com.dynamodb.dto.CustomerDTO;
import br.com.dynamodb.entity.CustomerEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static br.com.dynamodb.config.Constants.FORMATTER;
import static br.com.dynamodb.config.Constants.TIMEZONE_RECIFE;

@Component
public class CustomerMapper {

    public String toStringDate(Long epocDate) {
        return Instant
                .ofEpochSecond(epocDate)
                .atZone(ZoneId.of(TIMEZONE_RECIFE))
                .toLocalDateTime().format(FORMATTER);
    }

    public String toStringLocalDateTime(String stringDate) {
        return LocalDateTime
                .parse(stringDate)
                .format(FORMATTER);
    }

    public CustomerDTO toCustomerDTO(CustomerEntity customer) {
        return CustomerDTO.builder()
                .companyName(customer.getCompanyName())
                .companyDocumentNumber(customer.getCompanyDocumentNumber())
                .phoneNumber(customer.getPhoneNumber())
                .createDate(toStringLocalDateTime(customer.getCreateDate()))
                .expirationDate(toStringDate(customer.getExpirationDate()))
                .updatedDate(customer.getUpdatedDate() != null ? toStringLocalDateTime(customer.getUpdatedDate()) : null)
                .active(customer.getActive())
                .build();
    }

    public List<CustomerDTO> toCustomerDTOList(List<CustomerEntity> customers) {
        List<CustomerDTO> customersDTO = new ArrayList<>();
        customers.forEach(customer -> customersDTO.add(toCustomerDTO(customer)));
        return customersDTO;
    }
}
