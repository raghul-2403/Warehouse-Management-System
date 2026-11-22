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
@Route("products")
@PageTitle("WMS - Product")
public class ProductView extends VerticalLayout {

    private final ProductService productService;

    private final Grid<Product> productGrid = new Grid<>(
            Product.class,
            false
    );

    private final TextField searchField = new TextField("Search");

    private final TextField skuField = new TextField("SKU");
    private final TextField nameField = new TextField("Product Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final TextField unitField = new TextField("Unit");

    private final Span selectedProductText = new Span(
            "No product selected. Fill the form to add a new product."
    );

    private Product selectedProduct;

    public ProductView(ProductService productService) {
        this.productService = productService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");

        add(new H1("Product Management"));

        configureFields();
        configureGrid();

        Button addButton = new Button(
                "Add Product",
                event -> addProduct()
        );

        Button updateButton = new Button(
                "Update Product",
                event -> updateProduct()
        );

        Button deleteButton = new Button(
                "Delete Product",
                event -> deleteProduct()
        );

        Button clearButton = new Button(
                "Clear Form",
                event -> clearForm()
        );

        HorizontalLayout searchLayout = new HorizontalLayout(searchField);
        searchLayout.setWidthFull();

        HorizontalLayout form = new HorizontalLayout(
                skuField,
                nameField,
                descriptionField,
                unitField,
                addButton,
                updateButton,
                deleteButton,
                clearButton
        );

        form.setAlignItems(Alignment.END);
        form.setWidthFull();
        form.getStyle().set("flex-wrap", "wrap");

        selectedProductText.getStyle()
                .set("font-weight", "600")
                .set("color", "#334155");

        add(
                searchLayout,
                selectedProductText,
                form,
                productGrid
        );

        refreshGrid();
        WmsDesign.decorate(this, "Products", "Manage product catalog and SKUs");
    }

    private void configureFields() {
        searchField.setPlaceholder(
                "Search SKU, name, description, or unit"
        );

        searchField.setClearButtonVisible(true);
        searchField.setWidth("350px");

        searchField.addValueChangeListener(event -> applySearch());

        skuField.setPlaceholder("Example: SC-001");
        skuField.setRequiredIndicatorVisible(true);

        nameField.setPlaceholder("Example: Barcode Scanner");
        nameField.setRequiredIndicatorVisible(true);

        descriptionField.setPlaceholder(
                "Example: Handheld barcode scanner for warehouse use"
        );
        descriptionField.setWidth("250px");

        unitField.setPlaceholder("Example: pcs, box, kg");
        unitField.setRequiredIndicatorVisible(true);
    }

    private void configureGrid() {
        productGrid.addColumn(Product::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        productGrid.addColumn(Product::getSku)
                .setHeader("SKU")
                .setAutoWidth(true);

        productGrid.addColumn(Product::getName)
                .setHeader("Product Name")
                .setAutoWidth(true);

        productGrid.addColumn(Product::getDescription)
                .setHeader("Description")
                .setAutoWidth(true)
                .setFlexGrow(1);

        productGrid.addColumn(Product::getUnit)
                .setHeader("Unit")
                .setAutoWidth(true);

        productGrid.setWidthFull();

        productGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedProduct = event.getValue();

            if (selectedProduct == null) {
                return;
            }

            loadProductIntoForm(selectedProduct);
        });
    }

    private void loadProductIntoForm(Product product) {
        skuField.setValue(product.getSku());
        nameField.setValue(product.getName());
        descriptionField.setValue(
                product.getDescription() == null
                        ? ""
                        : product.getDescription()
        );
        unitField.setValue(product.getUnit());

        selectedProductText.setText(
                "Selected product: "
                        + product.getName()
                        + " (ID: " + product.getId() + ")"
        );
    }

    private void addProduct() {
        if (!isFormValid()) {
            return;
        }

        if (selectedProduct != null) {
            showError(
                    "A product is selected. Use Update Product or Clear Form."
            );
            return;
        }

        try {
            Product product = new Product(
                    skuField.getValue().trim(),
                    nameField.getValue().trim(),
                    descriptionField.getValue().trim(),
                    unitField.getValue().trim()
            );

            productService.save(product);

            showSuccess("Product added successfully.");
            clearForm();
            refreshGrid();

        } catch (Exception exception) {
            showError("Could not add product. SKU must be unique.");
        }
    }

    private void updateProduct() {
        if (selectedProduct == null) {
            showError("Select a product from the table before updating.");
            return;
        }

        if (!isFormValid()) {
            return;
        }

        try {
            selectedProduct.setSku(skuField.getValue().trim());
            selectedProduct.setName(nameField.getValue().trim());
            selectedProduct.setDescription(
                    descriptionField.getValue().trim()
            );
            selectedProduct.setUnit(unitField.getValue().trim());

            productService.save(selectedProduct);

            showSuccess("Product updated successfully.");
            clearForm();
            refreshGrid();

        } catch (Exception exception) {
            showError("Could not update product. SKU must be unique.");
        }
    }

    private void deleteProduct() {
        if (selectedProduct == null) {
            showError("Select a product from the table before deleting.");
            return;
        }

        try {
            productService.deleteById(selectedProduct.getId());

            showSuccess("Product deleted successfully.");
            clearForm();
            refreshGrid();

        } catch (Exception exception) {
            showError(
                    "Cannot delete this product because it may have inventory or orders."
            );
        }
    }

    private boolean isFormValid() {
        String sku = skuField.getValue().trim();
        String name = nameField.getValue().trim();
        String unit = unitField.getValue().trim();

        if (sku.isEmpty() || name.isEmpty() || unit.isEmpty()) {
            showError("SKU, Product Name, and Unit are required.");
            return false;
        }

        return true;
    }

    private void clearForm() {
        selectedProduct = null;

        skuField.clear();
        nameField.clear();
        descriptionField.clear();
        unitField.clear();

        productGrid.asSingleSelect().clear();

        selectedProductText.setText(
                "No product selected. Fill the form to add a new product."
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

        List<Product> products = productService.findAll()
                .stream()
                .filter(product -> matchesSearch(product, searchText))
                .sorted(Comparator.comparing(
                        Product::getName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        productGrid.setItems(products);
    }

    private boolean matchesSearch(
            Product product,
            String searchText) {

        if (searchText.isBlank()) {
            return true;
        }

        String sku = safeLower(product.getSku());
        String name = safeLower(product.getName());
        String description = safeLower(product.getDescription());
        String unit = safeLower(product.getUnit());

        return sku.contains(searchText)
                || name.contains(searchText)
                || description.contains(searchText)
                || unit.contains(searchText);
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