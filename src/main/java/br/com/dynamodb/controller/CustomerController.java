package br.com.dynamodb.controller;

import br.com.dynamodb.dto.CustomerDTO;
import br.com.dynamodb.service.CustomerService;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/v1/")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("customer")
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        return ResponseEntity.ok(customerService.saveCustomer(customerDTO));
    }

    @GetMapping("customer")
    public ResponseEntity<List<CustomerDTO>> findCustomerByName(@RequestParam("companyName") String companyName) {
        return ResponseEntity.ok(customerService.findByCompanyName(companyName));
    }

    @GetMapping("customer/query")
    public ResponseEntity<CustomerDTO> findCompanyNameByQuery(@RequestParam("companyName") String companyName) {
        return ResponseEntity.ok(customerService.findCompanyNameByQuery(companyName));
    }

    @GetMapping("customer/all")
    public ResponseEntity<List<CustomerDTO>> findAllCustomers() {
        return ResponseEntity.ok(customerService.findAllCustomers());
    }

    @PatchMapping("customer")
    public ResponseEntity<CustomerDTO> updateCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        return ResponseEntity.ok(customerService.updateCustomer(customerDTO));
    }

    @PatchMapping(value = "customer/{companyDocumentNumber}")
    public ResponseEntity<CustomerDTO> disableCustomer(@PathVariable(value = "companyDocumentNumber") String companyDocumentNumber) {
        return ResponseEntity.ok(customerService.disableCustomer(companyDocumentNumber));
    }

}