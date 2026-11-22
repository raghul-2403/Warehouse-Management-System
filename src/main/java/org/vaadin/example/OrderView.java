package org.vaadin.example;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@PermitAll
@Route("orders")
@PageTitle("WMS - Order")
public class OrderView extends VerticalLayout {

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerOrderService customerOrderService;

    private final TextField searchField = new TextField("Search Orders");
    private final ComboBox<OrderStatus> statusFilter = new ComboBox<>("Filter by Status");
    private final ComboBox<Customer> customerBox = new ComboBox<>("Customer");
    private final ComboBox<Product> productBox = new ComboBox<>("Product");
    private final IntegerField quantityField = new IntegerField("Quantity");

    private final Span availableStockText = new Span(
            "Select a product to view available stock."
    );

    private final Grid<CustomerOrder> orderGrid = new Grid<>(
            CustomerOrder.class,
            false
    );

    private final VerticalLayout orderDetails = new VerticalLayout();
    private CustomerOrder selectedOrder;

    public OrderView(
            CustomerService customerService,
            ProductService productService,
            CustomerOrderService customerOrderService) {

        this.customerService = customerService;
        this.productService = productService;
        this.customerOrderService = customerOrderService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");

        add(new H1("Order Management"));

        configureForm();
        configureGrid();
        configureOrderDetails();

        configureFilters();

        HorizontalLayout filters = new HorizontalLayout(
             searchField,
             statusFilter
        );

        filters.setAlignItems(Alignment.END);
        filters.setWidthFull();

        add(filters);

        Button createOrderButton = new Button(
                "Create Order",
                event -> createOrder()
        );

        HorizontalLayout form = new HorizontalLayout(
                customerBox,
                productBox,
                quantityField,
                createOrderButton
        );

        form.setAlignItems(Alignment.END);
        form.setWidthFull();
        form.getStyle().set("flex-wrap", "wrap");

        availableStockText.getStyle()
                .set("font-weight", "600")
                .set("color", "#334155");

        add(form, availableStockText, orderGrid, orderDetails);

        refreshData();
        WmsDesign.decorate(this, "Orders", "Process and track warehouse orders");
    }

    private void configureForm() {
        customerBox.setItemLabelGenerator(customer ->
                customer.getName() + " - " + customer.getEmail()
        );

        productBox.setItemLabelGenerator(product ->
                product.getSku() + " - " + product.getName()
        );

        quantityField.setMin(1);
        quantityField.setValue(1);
        quantityField.setWidth("130px");

        productBox.addValueChangeListener(event -> showAvailableStock());
    }

    private void configureGrid() {
        orderGrid.addColumn(CustomerOrder::getId)
                .setHeader("Order ID")
                .setAutoWidth(true);

        orderGrid.addColumn(order -> order.getCustomer().getName())
                .setHeader("Customer")
                .setAutoWidth(true);

        orderGrid.addColumn(order -> order.getProduct().getSku())
                .setHeader("SKU")
                .setAutoWidth(true);

        orderGrid.addColumn(order -> order.getProduct().getName())
                .setHeader("Product")
                .setAutoWidth(true)
                .setFlexGrow(1);

        orderGrid.addColumn(CustomerOrder::getQuantity)
                .setHeader("Quantity")
                .setAutoWidth(true);

        orderGrid.addComponentColumn(this::createStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true);

        orderGrid.addComponentColumn(this::createActionButton)
                .setHeader("Action")
                .setAutoWidth(true);

        orderGrid.addColumn(order -> order.getOrderDate().toString())
                .setHeader("Order Date")
                .setAutoWidth(true);

        orderGrid.setWidthFull();

        orderGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedOrder = event.getValue();
            showOrderDetails(selectedOrder);
        });
    }

    private void configureOrderDetails() {
        orderDetails.setPadding(true);
        orderDetails.setSpacing(false);
        orderDetails.setWidthFull();

        orderDetails.getStyle()
                .set("border", "1px solid #d1d5db")
                .set("border-radius", "8px")
                .set("background", "#f8fafc");

        showOrderDetails(null);
    }

    private void showOrderDetails(CustomerOrder order) {
        orderDetails.removeAll();

        orderDetails.add(new H2("Order Details"));

        if (order == null) {
            Paragraph message = new Paragraph(
                    "Select an order from the table to view its details."
            );

            message.getStyle().set("color", "#64748b");

            orderDetails.add(message);
            return;
        }

        orderDetails.add(
                createDetailRow("Order ID", String.valueOf(order.getId())),
                createDetailRow("Customer", order.getCustomer().getName()),
                createDetailRow("Email", order.getCustomer().getEmail()),
                createDetailRow("Product", order.getProduct().getName()),
                createDetailRow("SKU", order.getProduct().getSku()),
                createDetailRow("Quantity", String.valueOf(order.getQuantity())),
                createDetailRow("Status", order.getStatus().toString()),
                createDetailRow("Order Date", order.getOrderDate().toString())
        );
    }

    private HorizontalLayout createDetailRow(String label, String value) {
        Span labelText = new Span(label + ":");
        Span valueText = new Span(value);

        labelText.getStyle()
                .set("font-weight", "600")
                .set("width", "130px")
                .set("color", "#334155");

        valueText.getStyle().set("color", "#0f172a");

        HorizontalLayout row = new HorizontalLayout(labelText, valueText);
        row.setSpacing(true);
        row.getStyle().set("margin", "3px 0");

        return row;
    }

    private Span createStatusBadge(CustomerOrder order) {
        Span badge = new Span(order.getStatus().toString());

        badge.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "600")
                .set("padding", "4px 8px")
                .set("border-radius", "999px");

        switch (order.getStatus()) {
            case NEW -> badge.getStyle()
                    .set("background", "#dbeafe")
                    .set("color", "#1d4ed8");

            case PICKED -> badge.getStyle()
                    .set("background", "#fef3c7")
                    .set("color", "#b45309");

            case PACKED -> badge.getStyle()
                    .set("background", "#ede9fe")
                    .set("color", "#6d28d9");

            case SHIPPED -> badge.getStyle()
                    .set("background", "#dcfce7")
                    .set("color", "#15803d");

            case CANCELLED -> badge.getStyle()
                    .set("background", "#fee2e2")
                    .set("color", "#b91c1c");

            default -> badge.getStyle()
                    .set("background", "#e2e8f0")
                    .set("color", "#475569");
        }

        return badge;
    }

    private Component createActionButton(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.CANCELLED) {
            Button completedButton = new Button(
                    order.getStatus() == OrderStatus.SHIPPED
                            ? "Completed" : "Cancelled"
            );
            completedButton.setEnabled(false);
            return completedButton;
        }

        String buttonText;
        if (order.getStatus() == OrderStatus.NEW) {
            buttonText = "Mark Picked";
        } else if (order.getStatus() == OrderStatus.PICKED) {
            buttonText = "Mark Packed";
        } else {
            buttonText = "Mark Shipped";
        }

        Button nextStatusButton = new Button(
                buttonText, event -> updateOrderStatus(order)
        );

        if (order.getStatus() != OrderStatus.NEW) {
            return nextStatusButton;
        }

        Button cancelButton = new Button("Cancel Order", event -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Cancel Order #" + order.getId());
            dialog.setText("Cancel this NEW order and release its reserved stock?");
            dialog.setCancelable(true);
            dialog.setConfirmText("Cancel Order");
            dialog.setConfirmButtonTheme("error primary");
            dialog.addConfirmListener(confirmEvent -> cancelOrder(order));
            dialog.open();
        });

        HorizontalLayout actions = new HorizontalLayout(
                nextStatusButton, cancelButton
        );
        actions.setSpacing(true);
        return actions;
    }

    private void cancelOrder(CustomerOrder order) {
        try {
            customerOrderService.cancelOrder(order.getId());
            refreshData();
            showAvailableStock();
            showSuccess("Order cancelled. Reserved stock released.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
            refreshData();
        }
    }

    private void showAvailableStock() {
        Product product = productBox.getValue();

        if (product == null) {
            availableStockText.setText(
                    "Select a product to view available stock."
            );
            return;
        }

        try {
            int availableQuantity = customerOrderService.getAvailableQuantity(
                    product.getId()
            );

            availableStockText.setText(
                    "Available stock for "
                            + product.getName()
                            + ": "
                            + availableQuantity
            );

        } catch (IllegalStateException exception) {
            availableStockText.setText(exception.getMessage());
        }
    }

    private void createOrder() {
        Customer customer = customerBox.getValue();
        Product product = productBox.getValue();
        Integer quantity = quantityField.getValue();

        if (customer == null || product == null
                || quantity == null || quantity < 1) {

            showError(
                    "Select a customer, product, and valid quantity."
            );

            return;
        }

        try {
            customerOrderService.save(
                    new CustomerOrder(customer, product, quantity)
            );

            customerBox.clear();
            productBox.clear();
            quantityField.setValue(1);

            availableStockText.setText(
                    "Select a product to view available stock."
            );

            refreshData();

            showSuccess(
                    "Order created with NEW status. "
                            + "Stock reduces when it is marked PICKED."
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    private void updateOrderStatus(CustomerOrder order) {
        try {
            customerOrderService.moveToNextStatus(order);
            CustomerOrder updatedOrder = customerOrderService.findById(order.getId());
            refreshData();
            showAvailableStock();
            if (updatedOrder.getStatus() == OrderStatus.PICKED) {
                showSuccess("Order marked PICKED. Inventory was reduced.");
            } else {
                showSuccess("Order updated to: " + updatedOrder.getStatus());
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
            refreshData();
        }
    }

    private void refreshData() {
        customerBox.setItems(customerService.findAll());
        productBox.setItems(productService.findAll());
        applyFilters();

        selectedOrder = null;
        orderGrid.asSingleSelect().clear();
        showOrderDetails(null);
    }

    private void showSuccess(String message) {
        Notification.show(
                message,
                3500,
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

    private void configureFilters() {
    searchField.setPlaceholder(
            "Search order ID, customer, SKU, or product"
    );
    searchField.setClearButtonVisible(true);
    searchField.setWidth("350px");

    statusFilter.setItems(OrderStatus.values());
    statusFilter.setPlaceholder("All statuses");
    statusFilter.setClearButtonVisible(true);

    searchField.addValueChangeListener(
            event -> applyFilters()
    );

    statusFilter.addValueChangeListener(
            event -> applyFilters()
    );
}

private void applyFilters() {

    String search = searchField.getValue()
            .trim()
            .toLowerCase(Locale.ROOT);

    OrderStatus selectedStatus = statusFilter.getValue();

    List<CustomerOrder> filteredOrders =
            customerOrderService.findAll()
                    .stream()
                    .filter(order -> {
                        String orderId =
                                String.valueOf(order.getId());

                        String customer =
                                order.getCustomer()
                                        .getName()
                                        .toLowerCase(Locale.ROOT);

                        String product =
                                order.getProduct()
                                        .getName()
                                        .toLowerCase(Locale.ROOT);

                        String sku =
                                order.getProduct()
                                        .getSku()
                                        .toLowerCase(Locale.ROOT);

                        return orderId.contains(search)
                                || customer.contains(search)
                                || product.contains(search)
                                || sku.contains(search);
                    })
                    .filter(order ->
                            selectedStatus == null
                                    || order.getStatus() == selectedStatus
                    )
                    .sorted(Comparator.comparing(
                            CustomerOrder::getId
                    ).reversed())
                    .toList();

    orderGrid.setItems(filteredOrders);
}
}