package br.com.dynamodb.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class BusinessExceptionTest {

    @Test
    void devePropagarAMensagemParaARuntimeException() {
        BusinessException exception = new BusinessException("dados inválidos");

        assertThat(exception.getMessage()).isEqualTo("dados inválidos");
    }

    @Test
    void deveSerUmaRuntimeException() {
        BusinessException exception = new BusinessException("qualquer mensagem");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deveTerAAnotacaoResponseStatusComHttpStatusUnprocessableContent() {
        ResponseStatus responseStatus = BusinessException.class.getAnnotation(ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
