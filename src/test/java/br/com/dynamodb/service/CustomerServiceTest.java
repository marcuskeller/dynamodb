package br.com.dynamodb.service;

import br.com.dynamodb.dto.CustomerDTO;
import br.com.dynamodb.exceptions.ResourceNotFoundException;
import br.com.dynamodb.exceptions.BusinessException;
import br.com.dynamodb.mapper.CustomerMapper;
import br.com.dynamodb.entity.CustomerEntity;
import br.com.dynamodb.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.data.TemporalUnitWithinOffset;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static br.com.dynamodb.commom.CustomerConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @InjectMocks
    CustomerService service;

    @Mock
    CustomerRepository customerRepository;

    @Spy
    CustomerMapper customerMapper = new CustomerMapper();

    private static final String CUSTOMER_IS_ALREADY = "There is already a customer with this document number";
    private static final String CUSTOMER_IS_NOT_EXISTS = "There is not customer with this document number";

    // Valores literais para substituir o uso indevido de anyString() como argumento real.
    private static final String EXISTING_COMPANY_NAME = "Empresa Portuguesa LTDA";
    private static final String UNKNOWN_COMPANY_NAME = "Empresa Inexistente LTDA";
    private static final String UNKNOWN_DOCUMENT_NUMBER = "00000000000199";


    @Test
    public void createCustomer_WithValidData_ReturnsCustomer() {

        given(customerRepository.findByCompanyDocumentNumber(anyString())).willReturn(List.of());
        given(customerRepository.save(any(CustomerEntity.class))).willReturn(CREATED_CUSTOMER_ID);

        //System under test
        CustomerDTO sut = service.saveCustomer(CUSTOMER_DTO);

        assertNotNull(sut);
        assertThat(sut.getCreateDate()).isNotEmpty();
        assertThat(sut.getCompanyName()).isEqualTo(CREATED_CUSTOMER_ID.getCompanyName());
        assertThat(sut.getCompanyDocumentNumber()).isEqualTo(CREATED_CUSTOMER_ID.getCompanyDocumentNumber());
        assertThat(sut.getPhoneNumber()).isEqualTo(CREATED_CUSTOMER_ID.getPhoneNumber());
        assertThat(sut.getActive()).isEqualTo(CREATED_CUSTOMER_ID.getActive());
        assertThat(sut.getExpirationDate()).isEqualTo(customerMapper.toStringDate(CREATED_CUSTOMER_ID.getExpirationDate()));
        assertNull(sut.getUpdatedDate());

        // Verifica o objeto realmente enviado ao DynamoDB, não só o retorno mockado.
        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(1)).save(captor.capture());
        CustomerEntity persisted = captor.getValue();
        assertThat(persisted.getCompanyName()).isEqualTo(CUSTOMER_DTO.getCompanyName());
        assertThat(persisted.getCompanyDocumentNumber()).isEqualTo(CUSTOMER_DTO.getCompanyDocumentNumber());
        assertThat(persisted.getPhoneNumber()).isEqualTo(CUSTOMER_DTO.getPhoneNumber());
        assertThat(persisted.getActive()).isTrue();
        assertThat(persisted.getUpdatedDate()).isNull();
        assertThat(persisted.getId())
                .matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        assertThat(LocalDateTime.parse(persisted.getCreateDate()))
                .isCloseTo(LocalDateTime.now(), new TemporalUnitWithinOffset(5, ChronoUnit.SECONDS));
        // expirationDate = createDate + 3 meses (mesma data-base, fuso -3)
        assertThat(persisted.getExpirationDate()).isEqualTo(
                LocalDateTime.parse(persisted.getCreateDate())
                        .plusMonths(3)
                        .toEpochSecond(ZoneOffset.ofHours(-3)));

    }

    @Test
    public void createCustomer_WithInvalidData_ThrowsException() {

        given(customerRepository.findByCompanyDocumentNumber(anyString())).willReturn(List.of(CUSTOMER_ID));

        Exception exception = assertThrows(BusinessException.class, () -> service.saveCustomer(CUSTOMER_DTO));

        assertThat(exception.getMessage()).isEqualTo(CUSTOMER_IS_ALREADY);

        // Garante que nada foi persistido quando a validação falha.
        verify(customerRepository, never()).save(any(CustomerEntity.class));

    }

    @Test
    public void getCustomer_ByExistingCompanyName_ReturnsCustomer() {

        given(customerRepository.findByCompanyName(anyString())).willReturn(List.of(CUSTOMER_ID));

        List<CustomerDTO> sut = service.findByCompanyName(EXISTING_COMPANY_NAME);

        assertNotNull(sut);
        assertThat(sut).isNotEmpty();
        assertThat(sut.getFirst().getCompanyName()).isEqualTo(CUSTOMER_ID.getCompanyName());
        assertThat(sut.getFirst().getCompanyDocumentNumber()).isEqualTo(CUSTOMER_ID.getCompanyDocumentNumber());
        assertThat(sut.getFirst().getPhoneNumber()).isEqualTo(CUSTOMER_ID.getPhoneNumber());
        assertThat(sut.getFirst().getActive()).isEqualTo(CUSTOMER_ID.getActive());
        assertThat(sut.getFirst().getExpirationDate()).isEqualTo(customerMapper.toStringDate(CUSTOMER_ID.getExpirationDate()));
        assertFalse(sut.getFirst().getUpdatedDate().isEmpty());
        assertThat(sut.getFirst().getUpdatedDate()).isEqualTo(customerMapper.toStringLocalDateTime(CUSTOMER_ID.getUpdatedDate()));

        // Verifica que o repositório foi chamado exatamente com o nome informado, uma única vez.
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(customerRepository, times(1)).findByCompanyName(nameCaptor.capture());
        assertThat(nameCaptor.getValue()).isEqualTo(EXISTING_COMPANY_NAME);

    }

    @Test
    public void getCustomer_ByQuery_ExistingCompanyName_ReturnsCustomer() {

        given(customerRepository.findCompanyNameByQuery(anyString())).willReturn(Optional.of(CUSTOMER_ID));

        CustomerDTO sut = service.findCompanyNameByQuery(EXISTING_COMPANY_NAME);

        assertNotNull(sut);
        assertThat(sut.getCompanyName()).isEqualTo(CUSTOMER_ID.getCompanyName());
        assertThat(sut.getCompanyDocumentNumber()).isEqualTo(CUSTOMER_ID.getCompanyDocumentNumber());
        assertThat(sut.getPhoneNumber()).isEqualTo(CUSTOMER_ID.getPhoneNumber());
        assertThat(sut.getActive()).isEqualTo(CUSTOMER_ID.getActive());
        assertThat(sut.getExpirationDate()).isEqualTo(customerMapper.toStringDate(CUSTOMER_ID.getExpirationDate()));
        assertFalse(sut.getUpdatedDate().isEmpty());
        assertThat(sut.getUpdatedDate()).isEqualTo(customerMapper.toStringLocalDateTime(CUSTOMER_ID.getUpdatedDate()));

        // 1 única consulta ao repositório (o Optional retornado é reaproveitado).
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(customerRepository, times(1)).findCompanyNameByQuery(nameCaptor.capture());
        assertThat(nameCaptor.getValue()).isEqualTo(EXISTING_COMPANY_NAME);

    }

    @Test
    public void getCustomer_ByQuery_UnExistingCompanyName_ReturnsEmpty() {

        given(customerRepository.findCompanyNameByQuery(anyString())).willReturn(Optional.empty());

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> service.findCompanyNameByQuery(UNKNOWN_COMPANY_NAME));

        assertThat(exception.getMessage()).contains(CUSTOMER_IS_NOT_EXISTS);

    }


    @Test
    public void getCustomer_ByUnExistingCompanyName_ReturnsEmpty() {

        given(customerRepository.findByCompanyName(anyString())).willReturn(List.of());

        List<CustomerDTO> sut = service.findByCompanyName(UNKNOWN_COMPANY_NAME);

        assertThat(sut).isEmpty();
    }

    @Test
    public void listCustomers_ReturnsAllCustomers() {

        given(customerRepository.findAllCustomers()).willReturn(CUSTOMERS);

        List<CustomerDTO> sut = service.findAllCustomers();

        assertThat(sut).isNotEmpty();
        assertThat(sut).hasSize(4);

        assertNotNull(sut.getFirst());
        assertThat(sut.getFirst().getCompanyName()).isEqualTo(AMERICANA.getCompanyName());
        assertThat(sut.getFirst().getCompanyDocumentNumber()).isEqualTo(AMERICANA.getCompanyDocumentNumber());
        assertThat(sut.getFirst().getPhoneNumber()).isEqualTo(AMERICANA.getPhoneNumber());
        assertThat(sut.getFirst().getExpirationDate()).isEqualTo(customerMapper.toStringDate(AMERICANA.getExpirationDate()));
        assertFalse(sut.getFirst().getUpdatedDate().isEmpty());
        assertThat(sut.getFirst().getUpdatedDate()).isEqualTo(customerMapper.toStringLocalDateTime(AMERICANA.getUpdatedDate()));

        assertNotNull(sut.get(1));
        assertThat(sut.get(1).getCompanyName()).isEqualTo(CHINESA.getCompanyName());
        assertThat(sut.get(1).getCompanyDocumentNumber()).isEqualTo(CHINESA.getCompanyDocumentNumber());
        assertThat(sut.get(1).getPhoneNumber()).isEqualTo(CHINESA.getPhoneNumber());
        assertThat(sut.get(1).getExpirationDate()).isEqualTo(customerMapper.toStringDate(CHINESA.getExpirationDate()));
        assertThat(sut.get(1).getUpdatedDate()).isEqualTo(customerMapper.toStringLocalDateTime(CHINESA.getUpdatedDate()));

        assertNotNull(sut.get(2));
        assertThat(sut.get(2).getCompanyName()).isEqualTo(CANADENSE.getCompanyName());
        assertThat(sut.get(2).getCompanyDocumentNumber()).isEqualTo(CANADENSE.getCompanyDocumentNumber());
        assertThat(sut.get(2).getPhoneNumber()).isEqualTo(CANADENSE.getPhoneNumber());
        assertThat(sut.get(2).getExpirationDate()).isEqualTo(customerMapper.toStringDate(CANADENSE.getExpirationDate()));
        assertNull(sut.get(2).getUpdatedDate());


        assertNotNull(sut.getLast());
        assertThat(sut.getLast().getCompanyName()).isEqualTo(BRASILEIRA.getCompanyName());
        assertThat(sut.getLast().getCompanyDocumentNumber()).isEqualTo(BRASILEIRA.getCompanyDocumentNumber());
        assertThat(sut.getLast().getPhoneNumber()).isEqualTo(BRASILEIRA.getPhoneNumber());
        assertThat(sut.getLast().getExpirationDate()).isEqualTo(customerMapper.toStringDate(BRASILEIRA.getExpirationDate()));
        assertThat(sut.getLast().getUpdatedDate()).isEqualTo(customerMapper.toStringLocalDateTime(BRASILEIRA.getUpdatedDate()));

        verify(customerRepository, times(1)).findAllCustomers();
    }

    @Test
    public void listCustomers_ReturnsNoCustomers() {
        given(customerRepository.findAllCustomers()).willReturn(Collections.emptyList());

        List<CustomerDTO> sut = service.findAllCustomers();

        assertThat(sut).isEmpty();
    }

    @Test
    public void disableCustomer_ByExistingCompanyName_ReturnsCustomer() {

        given(customerRepository.findByCompanyDocumentNumber(anyString())).willReturn(List.of(CUSTOMER_ID));
        given(customerRepository.update(any(CustomerEntity.class))).willReturn(DISABLE_CUSTOMER_ID);

        //System under test
        CustomerDTO sut = service.disableCustomer(CUSTOMER_ID.getCompanyDocumentNumber());

        assertNotNull(sut);
        assertThat(sut.getCompanyDocumentNumber()).isEqualTo(DISABLE_CUSTOMER_ID.getCompanyDocumentNumber());
        assertThat(sut.getCompanyName()).isEqualTo(DISABLE_CUSTOMER_ID.getCompanyName());
        assertThat(sut.getPhoneNumber()).isEqualTo(DISABLE_CUSTOMER_ID.getPhoneNumber());
        assertFalse(sut.getActive());
        assertFalse(sut.getUpdatedDate().isEmpty());
        assertThat(sut.getUpdatedDate()).isEqualTo(customerMapper.toStringLocalDateTime(DISABLE_CUSTOMER_ID.getUpdatedDate()));

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(1)).update(captor.capture());
        CustomerEntity sentToUpdate = captor.getValue();
        assertFalse(sentToUpdate.getActive());
        assertThat(sentToUpdate.getId()).isEqualTo(CUSTOMER_ID.getId());
        assertThat(sentToUpdate.getCompanyName()).isEqualTo(CUSTOMER_ID.getCompanyName());
        assertThat(sentToUpdate.getCompanyDocumentNumber()).isEqualTo(CUSTOMER_ID.getCompanyDocumentNumber());
        assertThat(sentToUpdate.getPhoneNumber()).isEqualTo(CUSTOMER_ID.getPhoneNumber());
        assertThat(sentToUpdate.getCreateDate()).isEqualTo(CUSTOMER_ID.getCreateDate());
        assertThat(sentToUpdate.getExpirationDate()).isEqualTo(CUSTOMER_ID.getExpirationDate());
        assertThat(LocalDateTime.parse(sentToUpdate.getUpdatedDate()))
                .isCloseTo(LocalDateTime.now(), new TemporalUnitWithinOffset(5, ChronoUnit.SECONDS));

    }

    @Test
    public void disableCustomer_ByUnExistingCompanyDocumentNumber_ReturnsEmpty() {

        given(customerRepository.findByCompanyDocumentNumber(anyString())).willReturn(List.of());

        assertThatThrownBy(() -> service.disableCustomer(CUSTOMER_DTO.getCompanyDocumentNumber()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository, never()).update(any(CustomerEntity.class));
    }

    @Test
    public void disableCustomer_ByUnExistingCompanyDocumentNumber_ReturnsExceptionMessage() {

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> service.disableCustomer(UNKNOWN_DOCUMENT_NUMBER));

        assertThat(exception.getMessage()).contains(CUSTOMER_IS_NOT_EXISTS);

        verify(customerRepository, never()).update(any(CustomerEntity.class));
    }

    @Test
    public void updateCustomer_ByExistingCompanyName_ReturnsUpdatedCustomer() {

        given(customerRepository.findByCompanyDocumentNumber(anyString())).willReturn(List.of(CUSTOMER_ID));
        given(customerRepository.update(any(CustomerEntity.class))).willReturn(AMERICANA);

        CustomerDTO sut = service.updateCustomer(CUSTOMER_DTO);

        assertNotNull(sut);
        assertThat(sut.getCompanyName()).isEqualTo(AMERICANA.getCompanyName());
        assertThat(sut.getCompanyDocumentNumber()).isEqualTo(AMERICANA.getCompanyDocumentNumber());
        assertThat(sut.getPhoneNumber()).isEqualTo(AMERICANA.getPhoneNumber());
        assertThat(sut.getActive()).isEqualTo(AMERICANA.getActive());
        assertThat(sut.getExpirationDate()).isEqualTo(customerMapper.toStringDate(AMERICANA.getExpirationDate()));
        assertThat(sut.getUpdatedDate()).isEqualTo(customerMapper.toStringLocalDateTime(AMERICANA.getUpdatedDate()));

        // Garante que o objeto realmente enviado ao update() reflete os dados do DTO recebido
        // (companyName/phoneNumber), não os do customer antigo.
        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository, times(1)).update(captor.capture());
        CustomerEntity sentToUpdate = captor.getValue();
        assertThat(sentToUpdate.getId()).isEqualTo(CUSTOMER_ID.getId());
        assertThat(sentToUpdate.getCompanyName()).isEqualTo(CUSTOMER_DTO.getCompanyName());
        assertThat(sentToUpdate.getPhoneNumber()).isEqualTo(CUSTOMER_DTO.getPhoneNumber());
        assertThat(sentToUpdate.getCompanyDocumentNumber()).isEqualTo(CUSTOMER_ID.getCompanyDocumentNumber());
        assertThat(sentToUpdate.getCreateDate()).isEqualTo(CUSTOMER_ID.getCreateDate());
        assertThat(sentToUpdate.getExpirationDate()).isEqualTo(CUSTOMER_ID.getExpirationDate());
        assertTrue(sentToUpdate.getActive());
        assertThat(LocalDateTime.parse(sentToUpdate.getUpdatedDate()))
                .isCloseTo(LocalDateTime.now(), new TemporalUnitWithinOffset(5, ChronoUnit.SECONDS));

    }

    @Test
    public void updateCustomer_ByUnExistingCompanyDocumentNumber_ReturnsExceptionMessage() {

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> service.updateCustomer(CUSTOMER_DTO));

        assertThat(exception.getMessage()).contains(CUSTOMER_IS_NOT_EXISTS);

        verify(customerRepository, never()).update(any(CustomerEntity.class));
    }

}
