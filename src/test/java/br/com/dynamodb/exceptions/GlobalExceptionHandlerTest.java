package br.com.dynamodb.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    // ---------------------------------------------------------------
    // handleAllExceptions
    // ---------------------------------------------------------------

    @Test
    void handleAllExceptions_deveRetornarStatus500ComDetalhesDaExcecao() {
        when(webRequest.getDescription(false)).thenReturn("uri=/v1/customer");
        Exception ex = new RuntimeException("erro inesperado");

        ResponseEntity<ExceptionResponse> resposta = handler.handleAllExceptions(ex, webRequest);
        ExceptionResponse body = resposta.getBody();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("erro inesperado");
        assertThat(body.getDetails()).isEqualTo("uri=/v1/customer");
        assertThat(body.getTimestamp()).isNotNull();
    }

    // ---------------------------------------------------------------
    // handleNotFoundExceptions
    // ---------------------------------------------------------------

    @Test
    void handleNotFoundExceptions_deveRetornarStatus404ComDetalhesDaExcecao() {
        when(webRequest.getDescription(false)).thenReturn("uri=/v1/customer/123");
        ResourceNotFoundException ex = new ResourceNotFoundException("cliente não encontrado");

        ResponseEntity<ExceptionResponse> resposta = handler.handleNotFoundExceptions(ex, webRequest);
        ExceptionResponse body = resposta.getBody();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("cliente não encontrado");
        assertThat(body.getDetails()).isEqualTo("uri=/v1/customer/123");
        assertThat(body.getTimestamp()).isNotNull();
    }

    // ---------------------------------------------------------------
    // handleBusinessException
    // ---------------------------------------------------------------

    @Test
    void handleBusinessException_deveRetornarStatus422ComDetalhesDaExcecao() {
        when(webRequest.getDescription(false)).thenReturn("uri=/v1/customer");
        BusinessException ex = new BusinessException("dados inválidos");

        ResponseEntity<ExceptionResponse> resposta =
                handler.handleBusinessException(ex, webRequest);
        ExceptionResponse body = resposta.getBody();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("dados inválidos");
        assertThat(body.getDetails()).isEqualTo("uri=/v1/customer");
        assertThat(body.getTimestamp()).isNotNull();
    }
}
