package br.com.dynamodb.config;

import java.time.format.DateTimeFormatter;

public final class Constants {

    public static final int TIMEZONE = -3;

    public static final String TIMEZONE_RECIFE = "America/Recife";

    public static final String GSI_COMPANY_NAME = "xCompanyName";

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss");

    private Constants() {
        throw new UnsupportedOperationException("Classe utilitária não pode ser instanciada");
    }

}
