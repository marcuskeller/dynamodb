package br.com.dynamodb.repository;

import br.com.dynamodb.config.Constants;
import br.com.dynamodb.entity.CustomerEntity;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CustomerRepository {

    private final DynamoDbTemplate dynamoDbTemplate;

    public CustomerRepository(DynamoDbTemplate dynamoDbTemplate) {
        this.dynamoDbTemplate = dynamoDbTemplate;
    }

    public List<CustomerEntity> findByCompanyDocumentNumber(String companyDocumentNumber) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":company_document_number", AttributeValue.fromS(companyDocumentNumber));

        Expression filterExpression = Expression.builder()
                .expression("company_document_number = :company_document_number")
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression).build();
        PageIterable<CustomerEntity> customers = dynamoDbTemplate.scan(scanEnhancedRequest,
                CustomerEntity.class);

        return customers
                .stream()
                .toList()
                .getFirst()
                .items();
    }

    public List<CustomerEntity> findByCompanyName(String companyName) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":company_name", AttributeValue.fromS(companyName));

        Expression filterExpression = Expression.builder()
                .expression("company_name = :company_name")
                .expressionValues(expressionValues)
                .build();

        ScanEnhancedRequest scanEnhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression).build();
        PageIterable<CustomerEntity> customers = dynamoDbTemplate.scan(scanEnhancedRequest,
                CustomerEntity.class);

        return customers
                .items()
                .stream()
                .toList();
    }

    public Optional<CustomerEntity> findCompanyNameByQuery(String companyName) {

        var key = Key.builder().partitionValue(companyName).build();
        var queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key)).build();

        PageIterable<CustomerEntity> customers = dynamoDbTemplate.query(queryEnhancedRequest,
                CustomerEntity.class, Constants.GSI_COMPANY_NAME);

        return customers
                .items()
                .stream()
                .findFirst();
    }

    public List<CustomerEntity> findAllCustomers() {
        var customers = dynamoDbTemplate.scanAll(CustomerEntity.class);

        return customers
                .items()
                .stream()
                .toList();

    }

    public CustomerEntity save(CustomerEntity customer) {
        return dynamoDbTemplate.save(customer);
    }

    public CustomerEntity update(CustomerEntity customer) {
        return dynamoDbTemplate.update(customer);
    }

}
