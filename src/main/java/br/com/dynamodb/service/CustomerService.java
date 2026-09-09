package br.com.dynamodb.service;

import br.com.dynamodb.config.Constants;
import br.com.dynamodb.dto.CustomerDTO;
import br.com.dynamodb.entity.CustomerEntity;
import br.com.dynamodb.exceptions.BusinessException;
import br.com.dynamodb.exceptions.ResourceNotFoundException;
import br.com.dynamodb.mapper.CustomerMapper;
import br.com.dynamodb.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private static final String CUSTOMER_IS_ALREADY = "There is already a customer with this document number";
    private static final String CUSTOMER_IS_NOT_EXISTS = "There is not customer with this document number";

    /** Meses até o cliente expirar (TTL). Regra de negócio do Customer. */
    private static final int EXPIRATION_MONTHS = 3;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    public CustomerDTO saveCustomer(CustomerDTO customerDTO) {
        boolean alreadyExists = !customerRepository
                .findByCompanyDocumentNumber(customerDTO.getCompanyDocumentNumber())
                .isEmpty();

        if (alreadyExists) {
            throw new BusinessException(CUSTOMER_IS_ALREADY);
        }

        String createDate = LocalDateTime.now().toString();

        CustomerEntity toCreate = CustomerEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyName(customerDTO.getCompanyName())
                .companyDocumentNumber(customerDTO.getCompanyDocumentNumber())
                .phoneNumber(customerDTO.getPhoneNumber())
                .createDate(createDate)
                .expirationDate(toExpirationEpoch(createDate))
                .active(true)
                .build();

        return customerMapper.toCustomerDTO(customerRepository.save(toCreate));
    }

    public List<CustomerDTO> findAllCustomers() {
        return customerMapper.toCustomerDTOList(customerRepository.findAllCustomers());
    }

    public List<CustomerDTO> findByCompanyName(String companyName) {
        return customerMapper.toCustomerDTOList(customerRepository.findByCompanyName(companyName));
    }

    public CustomerDTO findCompanyNameByQuery(String companyName) {
        return customerRepository.findCompanyNameByQuery(companyName)
                .map(customerMapper::toCustomerDTO)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_IS_NOT_EXISTS));
    }

    public CustomerDTO updateCustomer(CustomerDTO customerDTO) {
        CustomerEntity current = findByDocumentOrThrow(customerDTO.getCompanyDocumentNumber());

        CustomerEntity toUpdate = CustomerEntity.builder()
                .id(current.getId())
                .companyName(customerDTO.getCompanyName())
                .companyDocumentNumber(current.getCompanyDocumentNumber())
                .phoneNumber(customerDTO.getPhoneNumber())
                .createDate(current.getCreateDate())
                .expirationDate(current.getExpirationDate())
                .updatedDate(LocalDateTime.now().toString())
                .active(true)
                .build();

        return customerMapper.toCustomerDTO(customerRepository.update(toUpdate));
    }

    public CustomerDTO disableCustomer(String companyDocumentNumber) {
        CustomerEntity current = findByDocumentOrThrow(companyDocumentNumber);

        CustomerEntity toDisable = CustomerEntity.builder()
                .id(current.getId())
                .companyName(current.getCompanyName())
                .companyDocumentNumber(current.getCompanyDocumentNumber())
                .phoneNumber(current.getPhoneNumber())
                .createDate(current.getCreateDate())
                .expirationDate(current.getExpirationDate())
                .updatedDate(LocalDateTime.now().toString())
                .active(false)
                .build();

        return customerMapper.toCustomerDTO(customerRepository.update(toDisable));
    }

    private CustomerEntity findByDocumentOrThrow(String companyDocumentNumber) {
        List<CustomerEntity> found = customerRepository.findByCompanyDocumentNumber(companyDocumentNumber);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException(CUSTOMER_IS_NOT_EXISTS);
        }
        return found.getFirst();
    }

    /** createDate (ISO) + EXPIRATION_MONTHS meses, em epoch seconds (fuso {@link Constants#TIMEZONE}). */
    private long toExpirationEpoch(String createDate) {
        return LocalDateTime.parse(createDate)
                .plusMonths(EXPIRATION_MONTHS)
                .toEpochSecond(ZoneOffset.ofHours(Constants.TIMEZONE));
    }
}
