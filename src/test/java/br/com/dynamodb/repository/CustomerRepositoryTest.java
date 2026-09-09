package br.com.dynamodb.repository;

import br.com.dynamodb.entity.CustomerEntity;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerRepositoryTest {

    @Mock
    private DynamoDbTemplate dynamoDbTemplate;

    private CustomerRepository repository;

    @BeforeEach
    void setup() {
        repository = new CustomerRepository(dynamoDbTemplate);
    }

    private static PageIterable<CustomerEntity> pageIterableOf(List<Page<CustomerEntity>> pages) {
        return PageIterable.create(pages::iterator);
    }

    private static Page<CustomerEntity> pageOf(CustomerEntity... customers) {
        return Page.builder(CustomerEntity.class)
                .items(List.of(customers))
                .build();
    }

    // ---------------------------------------------------------------
    // findByCompanyDocumentNumber
    // ---------------------------------------------------------------

    @Test
    void findByCompanyDocumentNumber_deveRetornarItensDaPrimeiraPagina() {
        CustomerEntity customer = CustomerEntity.builder()
                .id("id-1")
                .companyDocumentNumber("12345678000199")
                .build();

        when(dynamoDbTemplate.scan(any(ScanEnhancedRequest.class), eq(CustomerEntity.class)))
                .thenReturn(pageIterableOf(List.of(pageOf(customer))));

        List<CustomerEntity> resultado = repository.findByCompanyDocumentNumber("12345678000199");

        assertThat(resultado).containsExactly(customer);
    }

    @Test
    void findByCompanyDocumentNumber_deveMontarFilterExpressionComODocumentoInformado() {
        when(dynamoDbTemplate.scan(any(ScanEnhancedRequest.class), eq(CustomerEntity.class)))
                .thenReturn(pageIterableOf(List.of(pageOf())));

        ArgumentCaptor<ScanEnhancedRequest> requestCaptor = ArgumentCaptor.forClass(ScanEnhancedRequest.class);

        repository.findByCompanyDocumentNumber("doc-123");

        verify(dynamoDbTemplate).scan(requestCaptor.capture(), eq(CustomerEntity.class));
        ScanEnhancedRequest request = requestCaptor.getValue();

        assertThat(request.filterExpression().expression())
                .isEqualTo("company_document_number = :company_document_number");
        assertThat(request.filterExpression().expressionValues())
                .containsEntry(":company_document_number",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS("doc-123"));
    }

    @Test
    void findByCompanyDocumentNumber_semPaginasRetornadas_deveLancarExcecao() {
        when(dynamoDbTemplate.scan(any(ScanEnhancedRequest.class), eq(CustomerEntity.class)))
                .thenReturn(pageIterableOf(List.of()));

        assertThatThrownBy(() -> repository.findByCompanyDocumentNumber("inexistente"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---------------------------------------------------------------
    // findByCompanyName
    // ---------------------------------------------------------------

    @Test
    void findByCompanyName_deveRetornarItensDeTodasAsPaginas() {
        CustomerEntity customer1 = CustomerEntity.builder().id("id-1").companyName("Empresa A").build();
        CustomerEntity customer2 = CustomerEntity.builder().id("id-2").companyName("Empresa A").build();

        when(dynamoDbTemplate.scan(any(ScanEnhancedRequest.class), eq(CustomerEntity.class)))
                .thenReturn(pageIterableOf(List.of(pageOf(customer1), pageOf(customer2))));

        List<CustomerEntity> resultado = repository.findByCompanyName("Empresa A");

        assertThat(resultado).containsExactly(customer1, customer2);
    }

    @Test
    void findByCompanyName_semResultados_deveRetornarListaVazia() {
        when(dynamoDbTemplate.scan(any(ScanEnhancedRequest.class), eq(CustomerEntity.class)))
                .thenReturn(pageIterableOf(List.of()));

        List<CustomerEntity> resultado = repository.findByCompanyName("Inexistente");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByCompanyName_deveMontarFilterExpressionComONomeInformado() {
        when(dynamoDbTemplate.scan(any(ScanEnhancedRequest.class), eq(CustomerEntity.class)))
                .thenReturn(pageIterableOf(List.of()));

        ArgumentCaptor<ScanEnhancedRequest> requestCaptor = ArgumentCaptor.forClass(ScanEnhancedRequest.class);

        repository.findByCompanyName("Empresa X");

        verify(dynamoDbTemplate).scan(requestCaptor.capture(), eq(CustomerEntity.class));
        ScanEnhancedRequest request = requestCaptor.getValue();

        assertThat(request.filterExpression().expression())
                .isEqualTo("company_name = :company_name");
        assertThat(request.filterExpression().expressionValues())
                .containsEntry(":company_name",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS("Empresa X"));
    }

    // ---------------------------------------------------------------
    // findCompanyNameByQuery
    // ---------------------------------------------------------------

    @Test
    void findCompanyNameByQuery_deveRetornarPrimeiroItemQuandoExiste() {
        CustomerEntity customer = CustomerEntity.builder().id("id-3").companyName("Empresa Query").build();

        when(dynamoDbTemplate.query(any(QueryEnhancedRequest.class), eq(CustomerEntity.class), eq("xCompanyName")))
                .thenReturn(pageIterableOf(List.of(pageOf(customer))));

        Optional<CustomerEntity> resultado = repository.findCompanyNameByQuery("Empresa Query");

        assertThat(resultado).contains(customer);
    }

    @Test
    void findCompanyNameByQuery_semResultados_deveRetornarOptionalVazio() {
        when(dynamoDbTemplate.query(any(QueryEnhancedRequest.class), eq(CustomerEntity.class), eq("xCompanyName")))
                .thenReturn(pageIterableOf(List.of()));

        Optional<CustomerEntity> resultado = repository.findCompanyNameByQuery("Inexistente");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findCompanyNameByQuery_deveConsultarOIndiceXCompanyName() {
        when(dynamoDbTemplate.query(any(QueryEnhancedRequest.class), eq(CustomerEntity.class), eq("xCompanyName")))
                .thenReturn(pageIterableOf(List.of()));

        repository.findCompanyNameByQuery("Empresa Y");

        verify(dynamoDbTemplate).query(any(QueryEnhancedRequest.class), eq(CustomerEntity.class), eq("xCompanyName"));
    }

    // ---------------------------------------------------------------
    // findAllCustomers
    // ---------------------------------------------------------------

    @Test
    void findAllCustomers_deveRetornarItensDeTodasAsPaginas() {
        CustomerEntity customer1 = CustomerEntity.builder().id("id-1").build();
        CustomerEntity customer2 = CustomerEntity.builder().id("id-2").build();

        when(dynamoDbTemplate.scanAll(CustomerEntity.class))
                .thenReturn(pageIterableOf(List.of(pageOf(customer1), pageOf(customer2))));

        List<CustomerEntity> resultado = repository.findAllCustomers();

        assertThat(resultado).containsExactly(customer1, customer2);
    }

    @Test
    void findAllCustomers_semClientes_deveRetornarListaVazia() {
        when(dynamoDbTemplate.scanAll(CustomerEntity.class))
                .thenReturn(pageIterableOf(List.of()));

        List<CustomerEntity> resultado = repository.findAllCustomers();

        assertThat(resultado).isEmpty();
    }

    // ---------------------------------------------------------------
    // save
    // ---------------------------------------------------------------

    @Test
    void save_devePassarOCustomerParaOTemplateERetornarOSalvo() {
        CustomerEntity customer = CustomerEntity.builder().id("id-1").companyName("Empresa A").build();

        when(dynamoDbTemplate.save(customer)).thenReturn(customer);

        CustomerEntity resultado = repository.save(customer);

        assertThat(resultado).isEqualTo(customer);
        verify(dynamoDbTemplate).save(customer);
    }

    // ---------------------------------------------------------------
    // update
    // ---------------------------------------------------------------

    @Test
    void update_devePassarOCustomerParaOTemplateERetornarOAtualizado() {
        CustomerEntity customer = CustomerEntity.builder().id("id-1").companyName("Empresa A Atualizada").build();

        when(dynamoDbTemplate.update(customer)).thenReturn(customer);

        CustomerEntity resultado = repository.update(customer);

        assertThat(resultado).isEqualTo(customer);
        verify(dynamoDbTemplate).update(customer);
    }
}
