package br.com.dynamodb.mapper;

import br.com.dynamodb.dto.CustomerDTO;
import br.com.dynamodb.entity.CustomerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new CustomerMapper();
    }

    @ParameterizedTest(name = "epoch {0} deve virar \"{1}\" no fuso de Recife")
    @CsvSource({
            // epoch 1713186000 == 2024-04-15T13:00:00Z == 2024-04-15T10:00:00 em America/Recife
            "1713186000, '15/04/2024 10:00:00'",
            // epoch 0 == 1970-01-01T00:00:00Z == 1969-12-31T21:00:00 em America/Recife (-03:00)
            // 21h em relogio de 12h (padrao "hh") = 09
            "0, '31/12/1969 09:00:00'"
    })
    void toStringDate_deveFormatarEpochNoFusoDeRecife(Long epoch, String esperado) {
        String resultado = mapper.toStringDate(epoch);

        assertThat(resultado).isEqualTo(esperado);
        assertThat(resultado).isNotBlank();
    }

    @Test
    void toStringLocalDateTime_deveFormatarDataEHora() {
        String resultado = mapper.toStringLocalDateTime("2024-01-15T10:30:45");

        assertThat(resultado).isEqualTo("15/01/2024 10:30:45");
    }

    @Test
    void toStringLocalDateTime_deveTratarMeiaNoiteNoPadraoDeRelogio12h() {
        // hora-do-dia 00 -> "hh" (clock-hour-of-am-pm) formata como 12
        String resultado = mapper.toStringLocalDateTime("2024-06-01T00:00:00");

        assertThat(resultado).isEqualTo("01/06/2024 12:00:00");
    }

    @Test
    void toCustomerDTO_comUpdatedDateNulo_naoDeveFormatarUpdatedDate() {
        CustomerEntity customer = CustomerEntity.builder()
                .id("id-1")
                .companyName("Empresa A")
                .companyDocumentNumber("doc-a")
                .phoneNumber("8100000000")
                .createDate("2024-01-15T10:30:45")
                .expirationDate(1713186000L)
                .updatedDate(null)
                .active(true)
                .build();

        CustomerDTO resultado = mapper.toCustomerDTO(customer);

        assertThat(resultado.getCompanyName()).isEqualTo("Empresa A");
        assertThat(resultado.getCompanyDocumentNumber()).isEqualTo("doc-a");
        assertThat(resultado.getPhoneNumber()).isEqualTo("8100000000");
        assertThat(resultado.getActive()).isTrue();
        assertThat(resultado.getCreateDate()).isEqualTo("15/01/2024 10:30:45");
        assertThat(resultado.getExpirationDate()).isEqualTo("15/04/2024 10:00:00");
        assertThat(resultado.getUpdatedDate()).isNull();
    }

    @Test
    void toCustomerDTO_comUpdatedDatePreenchido_deveFormatarUpdatedDate() {
        CustomerEntity customer = CustomerEntity.builder()
                .id("id-2")
                .companyName("Empresa B")
                .companyDocumentNumber("doc-b")
                .phoneNumber("8100000001")
                .createDate("2024-01-15T10:30:45")
                .expirationDate(1713186000L)
                .updatedDate("2024-01-15T22:15:00")
                .active(false)
                .build();

        CustomerDTO resultado = mapper.toCustomerDTO(customer);

        assertThat(resultado.getActive()).isFalse();
        assertThat(resultado.getUpdatedDate()).isEqualTo("15/01/2024 10:15:00");
    }

    @Test
    void toCustomerDTOList_comListaVazia_deveRetornarListaVazia() {
        List<CustomerDTO> resultado = mapper.toCustomerDTOList(List.of());

        assertThat(resultado).isEmpty();
    }

    @Test
    void toCustomerDTOList_deveMapearEManterAOrdemDaLista() {
        CustomerEntity customer1 = CustomerEntity.builder()
                .id("id-6")
                .companyName("Primeira Empresa")
                .companyDocumentNumber("doc-1")
                .phoneNumber("8100000005")
                .createDate("2024-01-01T00:00:00")
                .expirationDate(1713186000L)
                .active(true)
                .build();

        CustomerEntity customer2 = CustomerEntity.builder()
                .id("id-7")
                .companyName("Segunda Empresa")
                .companyDocumentNumber("doc-2")
                .phoneNumber("8100000006")
                .createDate("2024-01-02T00:00:00")
                .expirationDate(1713186000L)
                .active(false)
                .build();

        List<CustomerDTO> resultado = mapper.toCustomerDTOList(List.of(customer1, customer2));

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getCompanyName()).isEqualTo("Primeira Empresa");
        assertThat(resultado.get(0).getCompanyDocumentNumber()).isEqualTo("doc-1");
        assertThat(resultado.get(1).getCompanyName()).isEqualTo("Segunda Empresa");
        assertThat(resultado.get(1).getCompanyDocumentNumber()).isEqualTo("doc-2");
    }
}
