
package org.vaadin.example;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteById(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("Customer not found.");
        }

        customerRepository.deleteById(customerId);
    }
}
