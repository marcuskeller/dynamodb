package br.com.dynamodb.service;

import br.com.dynamodb.dto.CustomerDTO;
import br.com.dynamodb.exceptions.BusinessException;
import br.com.dynamodb.exceptions.ResourceNotFoundException;
import br.com.dynamodb.mapper.Mapper;
import br.com.dynamodb.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    private final Mapper mapper;

    public CustomerService(CustomerRepository repository, Mapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    private static final String CUSTOMER_IS_ALREADY = "There is already a customer with this document number";
    private static final String CUSTOMER_IS_NOT_EXISTS = "There is not customer with this document number";

    public CustomerDTO saveCustomer(CustomerDTO customerDTO) {
        var recoveryListCustomer =
                repository.findByCompanyDocumentNumber(customerDTO.getCompanyDocumentNumber());

        if (!recoveryListCustomer.isEmpty()) {
            throw new BusinessException(CUSTOMER_IS_ALREADY);
        }

        return mapper
                .toCustomerDTO(
                        repository.save(
                                mapper.toCreateCustomer(customerDTO)
                        ));
    }

    public List<CustomerDTO> findAllCustomers() {
        return mapper.toCustomerDTOList(repository.findAllCustomers());
    }

    public List<CustomerDTO> findByCompanyName(String companyName) {
        return mapper.toCustomerDTOList(repository.findByCompanyName(companyName));
    }

    public CustomerDTO findCompanyNameByQuery(String companyName) {
        var recoveredCustomer = repository.findCompanyNameByQuery(companyName);

        if (recoveredCustomer.isEmpty()) {
            throw new ResourceNotFoundException(CUSTOMER_IS_NOT_EXISTS);
        }

        return mapper
                .toCustomerDTO(mapper
                        .optionalToCustomer(repository
                                .findCompanyNameByQuery(companyName)));
    }

    public CustomerDTO updateCustomer(CustomerDTO customerDTO) {
        var recoveryListCustomer =
                repository.findByCompanyDocumentNumber(customerDTO.getCompanyDocumentNumber());

        if (recoveryListCustomer.isEmpty()) {
            throw new ResourceNotFoundException(CUSTOMER_IS_NOT_EXISTS);
        }

        var recoveryCustomer = recoveryListCustomer.stream().toList().getFirst();

        return mapper.toCustomerDTO(
                repository.update(
                        mapper.optionalToUpdateCustomer(recoveryCustomer, customerDTO)));
    }

    public CustomerDTO disableCustomer(String companyDocumentNumber) {
        var recoveryListCustomer =
                repository.findByCompanyDocumentNumber(companyDocumentNumber);

        if (recoveryListCustomer.isEmpty()) {
            throw new ResourceNotFoundException(CUSTOMER_IS_NOT_EXISTS);
        }

        var recoveryCustomer = recoveryListCustomer.stream().toList().getFirst();

        return mapper.toCustomerDTO(
                repository.update(
                        mapper.optionalToDisableCustomer(recoveryCustomer)
                ));
    }

}
