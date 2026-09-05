package br.com.dynamodb.model;

import br.com.dynamodb.config.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Customer implements Serializable {

    @Serial
    private static final long serialVersionUID = -4282005207341771716L;

    private String id;
    private String companyName;
    private String companyDocumentNumber;
    private String phoneNumber;
    private String createDate;
    private String updatedDate;
    private Long expirationDate;
    private Boolean active;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = Constants.GSI_COMPANY_NAME)
    @DynamoDbAttribute("company_name")
    @JsonProperty("company_name")
    public String getCompanyName() {
        return companyName;
    }

    @DynamoDbAttribute("company_document_number")
    @JsonProperty("company_document_number")
    public String getCompanyDocumentNumber() {
        return companyDocumentNumber;
    }

    @DynamoDbAttribute("phone_number")
    @JsonProperty("phone_number")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @DynamoDbAttribute("create_date")
    @JsonProperty("create_date")
    public String getCreateDate() {
        return createDate;
    }

    @DynamoDbAttribute("updated_date")
    @JsonProperty("updated_date")
    public String getUpdatedDate() {
        return updatedDate;
    }

    @DynamoDbAttribute("expiration_date")
    @JsonProperty("expiration_date")
    public Long getExpirationDate() {
        return expirationDate;
    }

    @DynamoDbAttribute("active")
    @JsonProperty("active")
    public Boolean getActive() {
        return active;
    }

}