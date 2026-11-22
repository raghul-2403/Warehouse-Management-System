package org.vaadin.example;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("ADMIN")
@Route("customers")
@PageTitle("WMS - Customer")
public class CustomerView extends VerticalLayout {

    private final CustomerService customerService;

    private final Grid<Customer> customerGrid = new Grid<>(
            Customer.class,
            false
    );

    private final TextField searchField = new TextField("Search");

    private final TextField nameField = new TextField("Customer Name");
    private final TextField emailField = new TextField("Email");
    private final TextField phoneField = new TextField("Phone");
    private final TextArea addressField = new TextArea("Address");

    private final Span selectedCustomerText = new Span(
            "No customer selected. Fill the form to add a new customer."
    );

    private Customer selectedCustomer;

    public CustomerView(CustomerService customerService) {
        this.customerService = customerService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");

        add(new H1("Customer Management"));

        configureFields();
        configureGrid();

        Button addButton = new Button(
                "Add Customer",
                event -> addCustomer()
        );

        Button updateButton = new Button(
                "Update Customer",
                event -> updateCustomer()
        );

        Button deleteButton = new Button(
                "Delete Customer",
                event -> deleteCustomer()
        );

        Button clearButton = new Button(
                "Clear Form",
                event -> clearForm()
        );

        HorizontalLayout form = new HorizontalLayout(
                nameField,
                emailField,
                phoneField,
                addressField,
                addButton,
                updateButton,
                deleteButton,
                clearButton
        );

        form.setAlignItems(Alignment.END);
        form.setWidthFull();
        form.getStyle().set("flex-wrap", "wrap");

        HorizontalLayout searchLayout = new HorizontalLayout(searchField);
        searchLayout.setWidthFull();

        selectedCustomerText.getStyle()
                .set("font-weight", "600")
                .set("color", "#334155");

        add(
                searchLayout,
                selectedCustomerText,
                form,
                customerGrid
        );

        refreshGrid();
        WmsDesign.decorate(this, "Customers", "Manage customer records");
    }

    private void configureFields() {
        searchField.setPlaceholder(
                "Search name, email, phone, or address"
        );

        searchField.setClearButtonVisible(true);
        searchField.setWidth("350px");

        searchField.addValueChangeListener(event -> applySearch());

        nameField.setPlaceholder("Example: Arun Kumar");
        nameField.setRequiredIndicatorVisible(true);

        emailField.setPlaceholder("Example: arun@email.com");
        emailField.setRequiredIndicatorVisible(true);

        phoneField.setPlaceholder("Example: 9876543210");

        addressField.setPlaceholder("Example: Chennai, Tamil Nadu");
        addressField.setWidth("250px");
    }

    private void configureGrid() {
        customerGrid.addColumn(Customer::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        customerGrid.addColumn(Customer::getName)
                .setHeader("Customer Name")
                .setAutoWidth(true);

        customerGrid.addColumn(Customer::getEmail)
                .setHeader("Email")
                .setAutoWidth(true);

        customerGrid.addColumn(Customer::getPhone)
                .setHeader("Phone")
                .setAutoWidth(true);

        customerGrid.addColumn(Customer::getAddress)
                .setHeader("Address")
                .setAutoWidth(true)
                .setFlexGrow(1);

        customerGrid.setWidthFull();

        customerGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedCustomer = event.getValue();

            if (selectedCustomer == null) {
                return;
            }

            loadCustomerIntoForm(selectedCustomer);
        });
    }

    private void loadCustomerIntoForm(Customer customer) {
        nameField.setValue(customer.getName());
        emailField.setValue(customer.getEmail());
        phoneField.setValue(
                customer.getPhone() == null ? "" : customer.getPhone()
        );
        addressField.setValue(
                customer.getAddress() == null ? "" : customer.getAddress()
        );

        selectedCustomerText.setText(
                "Selected customer: "
                        + customer.getName()
                        + " (ID: " + customer.getId() + ")"
        );
    }

    private void addCustomer() {
        if (!isFormValid()) {
            return;
        }

        if (selectedCustomer != null) {
            showError(
                    "A customer is selected. Use Update Customer or Clear Form."
            );
            return;
        }

        try {
            Customer customer = new Customer(
                    nameField.getValue().trim(),
                    emailField.getValue().trim(),
                    phoneField.getValue().trim(),
                    addressField.getValue().trim()
            );

            customerService.save(customer);

            showSuccess("Customer added successfully.");
            clearForm();
            refreshGrid();

        } catch (Exception exception) {
            showError(
                    "Could not add customer. Email must be unique."
            );
        }
    }

    private void updateCustomer() {
        if (selectedCustomer == null) {
            showError(
                    "Select a customer from the table before updating."
            );
            return;
        }

        if (!isFormValid()) {
            return;
        }

        try {
            selectedCustomer.setName(nameField.getValue().trim());
            selectedCustomer.setEmail(emailField.getValue().trim());
            selectedCustomer.setPhone(phoneField.getValue().trim());
            selectedCustomer.setAddress(addressField.getValue().trim());

            customerService.save(selectedCustomer);

            showSuccess("Customer updated successfully.");
            clearForm();
            refreshGrid();

        } catch (Exception exception) {
            showError(
                    "Could not update customer. Email must be unique."
            );
        }
    }

    private void deleteCustomer() {
        if (selectedCustomer == null) {
            showError(
                    "Select a customer from the table before deleting."
            );
            return;
        }

        try {
            customerService.deleteById(selectedCustomer.getId());

            showSuccess("Customer deleted successfully.");
            clearForm();
            refreshGrid();

        } catch (Exception exception) {
            showError(
                    "Cannot delete this customer because it may have orders."
            );
        }
    }

    private boolean isFormValid() {
        String name = nameField.getValue().trim();
        String email = emailField.getValue().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showError("Customer Name and Email are required.");
            return false;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showError("Enter a valid email address.");
            return false;
        }

        return true;
    }

    private void clearForm() {
        selectedCustomer = null;

        nameField.clear();
        emailField.clear();
        phoneField.clear();
        addressField.clear();

        customerGrid.asSingleSelect().clear();

        selectedCustomerText.setText(
                "No customer selected. Fill the form to add a new customer."
        );
    }

    private void refreshGrid() {
        applySearch();
    }

    private void applySearch() {
        String searchText = searchField.getValue() == null
                ? ""
                : searchField.getValue()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        List<Customer> customers = customerService.findAll()
                .stream()
                .filter(customer -> matchesSearch(customer, searchText))
                .sorted(Comparator.comparing(
                        Customer::getName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        customerGrid.setItems(customers);
    }

    private boolean matchesSearch(
            Customer customer,
            String searchText) {

        if (searchText.isBlank()) {
            return true;
        }

        String name = safeLower(customer.getName());
        String email = safeLower(customer.getEmail());
        String phone = safeLower(customer.getPhone());
        String address = safeLower(customer.getAddress());

        return name.contains(searchText)
                || email.contains(searchText)
                || phone.contains(searchText)
                || address.contains(searchText);
    }

    private String safeLower(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT);
    }

    private void showSuccess(String message) {
        Notification.show(
                message,
                3000,
                Notification.Position.BOTTOM_START
        );
    }

    private void showError(String message) {
        Notification.show(
                message,
                4000,
                Notification.Position.MIDDLE
        );
    }
}