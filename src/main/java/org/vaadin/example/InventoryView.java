package org.vaadin.example;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
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
@Route("inventory")
@PageTitle("WMS - Inventory")
public class InventoryView extends VerticalLayout {

    private final ProductService productService;

    private final InventoryService inventoryService;

    private final Grid<Inventory> inventoryGrid = new Grid<>(Inventory.class, false);

    private final ComboBox<Product> productBox = new ComboBox<>("Product");

    private final TextField locationField = new TextField("Location");

    private final IntegerField quantityField = new IntegerField("Quantity");

    private final IntegerField reorderLevelField = new IntegerField("Reorder Level");

    private final TextField searchField = new TextField("Search");

    private final ComboBox<String> locationFilter = new ComboBox<>("Location");

    private final Checkbox lowStockOnly = new Checkbox("Low stock only");

    private final Span recordCount = new Span();

    private final Span selectedInventoryText = new Span("No inventory record selected.");

    private final IntegerField adjustmentField = new IntegerField("Quantity to add/remove");

    private final IntegerField updateReorderLevelField = new IntegerField("New reorder level");

    private Inventory selectedInventory;

    public InventoryView(
            ProductService productService,
            InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");
        add(new H1("Inventory Management"));
        configureForm();
        configureFilters();
        configureGrid();
        configureUpdateControls();
        Button saveButton = new Button("Add Stock Record", event -> saveInventory());
        HorizontalLayout form = new HorizontalLayout(
                productBox,
                locationField,
                quantityField,
                reorderLevelField,
                saveButton
        );
        form.setAlignItems(Alignment.END);
        form.setWidthFull();
        form.getStyle().set("flex-wrap", "wrap");
        Button refreshButton = new Button("Refresh", event -> refreshData());
        HorizontalLayout filters = new HorizontalLayout(
                searchField,
                locationFilter,
                lowStockOnly,
                refreshButton
        );
        filters.setAlignItems(Alignment.END);
        filters.setWidthFull();
        filters.getStyle().set("flex-wrap", "wrap");
        Button addQuantityButton = new Button(
                "Add Quantity",
                event -> addQuantity()
        );
        Button removeQuantityButton = new Button(
                "Remove Quantity",
                event -> removeQuantity()
        );
        Button updateReorderLevelButton = new Button(
                "Update Reorder Level",
                event -> updateReorderLevel()
        );
        Button updateInventoryButton = new Button(
        "Update Inventory",
        event -> updateInventory()
        );
        Button deleteInventoryButton = new Button(
        "Delete Inventory",
        event -> deleteInventory()
        );
        HorizontalLayout stockActions = new HorizontalLayout(
            adjustmentField,
            addQuantityButton,
            removeQuantityButton,
            updateReorderLevelField,
            updateReorderLevelButton,
            updateInventoryButton,
            deleteInventoryButton
        );
        stockActions.setAlignItems(Alignment.END);
        stockActions.setWidthFull();
        stockActions.getStyle().set("flex-wrap", "wrap");
        VerticalLayout updateSection = new VerticalLayout(
                new Paragraph("Update Selected Inventory"),
                selectedInventoryText,
                stockActions
        );
        updateSection.setPadding(true);
        updateSection.setSpacing(true);
        updateSection.setWidthFull();
        updateSection.getStyle()
                .set("border", "1px solid #d1d5db")
                .set("border-radius", "8px")
                .set("background", "#f8fafc");
        selectedInventoryText.getStyle()
                .set("font-weight", "600")
                .set("color", "#334155");
        add(
                form,
                filters,
                recordCount,
                inventoryGrid,
                updateSection
        );
        refreshData();
        WmsDesign.decorate(this, "Inventory", "Manage stock across storage locations");
    }

    private void configureForm() {
        productBox.setItems(productService.findAll());
        productBox.setItemLabelGenerator(
                product -> product.getSku() + " - " + product.getName()
        );
        locationField.setPlaceholder("Example: A-01-02");
        quantityField.setMin(0);
        quantityField.setValue(0);
        reorderLevelField.setMin(0);
        reorderLevelField.setValue(0);
    }

    private void configureFilters() {
        searchField.setPlaceholder("Product name, SKU, or location");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("280px");
        locationFilter.setPlaceholder("All locations");
        locationFilter.setClearButtonVisible(true);
        locationFilter.setWidth("180px");
        searchField.addValueChangeListener(event -> applyFilters());
        locationFilter.addValueChangeListener(event -> applyFilters());
        lowStockOnly.addValueChangeListener(event -> applyFilters());
    }

    private void configureGrid() {
        inventoryGrid.addColumn(Inventory::getId)
                .setHeader("ID")
                .setAutoWidth(true);
        inventoryGrid.addColumn(item -> item.getProduct().getSku())
                .setHeader("SKU")
                .setAutoWidth(true);
        inventoryGrid.addColumn(item -> item.getProduct().getName())
                .setHeader("Product")
                .setAutoWidth(true)
                .setFlexGrow(1);
        inventoryGrid.addColumn(Inventory::getLocation)
                .setHeader("Location")
                .setAutoWidth(true);
        inventoryGrid.addColumn(Inventory::getQuantity)
                .setHeader("Available")
                .setAutoWidth(true);
        inventoryGrid.addColumn(Inventory::getReorderLevel)
                .setHeader("Reorder Level")
                .setAutoWidth(true);
        inventoryGrid.addComponentColumn(this::createStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true);
        inventoryGrid.setPartNameGenerator(item ->
                isLowStock(item) ? "low-stock-row" : ""
        );
        inventoryGrid.getStyle()
                .set("--lumo-primary-color", "#2563eb");
        inventoryGrid.setWidthFull();
        inventoryGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedInventory = event.getValue();
            if (selectedInventory == null) {
                selectedInventoryText.setText(
                        "No inventory record selected."
                );
                adjustmentField.clear();
                updateReorderLevelField.clear();
                return;
            }
            selectedInventoryText.setText(
                    "Selected: "
                            + selectedInventory.getProduct().getName()
                            + " | SKU: "
                            + selectedInventory.getProduct().getSku()
                            + " | Location: "
                            + selectedInventory.getLocation()
                            + " | Available: "
                            + selectedInventory.getQuantity()
                            + " | Reorder level: "
                            + selectedInventory.getReorderLevel()
            );
            productBox.setValue(selectedInventory.getProduct());
            locationField.setValue(selectedInventory.getLocation());
            quantityField.setValue(selectedInventory.getQuantity());
            reorderLevelField.setValue(selectedInventory.getReorderLevel());
            updateReorderLevelField.setValue(
               selectedInventory.getReorderLevel()
        );
        });
    }

    private void configureUpdateControls() {
        adjustmentField.setMin(1);
        adjustmentField.setPlaceholder("Example: 5");
        adjustmentField.setWidth("190px");
        updateReorderLevelField.setMin(0);
        updateReorderLevelField.setPlaceholder("Example: 10");
        updateReorderLevelField.setWidth("180px");
    }

    private Span createStatusBadge(Inventory inventory) {
        boolean lowStock = isLowStock(inventory);
        Span badge = new Span(
                lowStock ? "LOW STOCK" : "IN STOCK"
        );
        badge.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "600")
                .set("padding", "4px 8px")
                .set("border-radius", "999px");
        if (lowStock) {
            badge.getStyle()
                    .set("background", "#ffedd5")
                    .set("color", "#c2410c");
        } else {
            badge.getStyle()
                    .set("background", "#dcfce7")
                    .set("color", "#15803d");
        }
        return badge;
    }

    private boolean isLowStock(Inventory inventory) {
        return inventory.getQuantity() <= inventory.getReorderLevel();
    }

    private void saveInventory() {
        if (selectedInventory != null) {
            showError("An inventory record is selected. Use Update Inventory or clear selection first.");
            return;
        }
        Product product = productBox.getValue();
        String location = locationField.getValue().trim();
        Integer quantity = quantityField.getValue();
        Integer reorderLevel = reorderLevelField.getValue();
        if (product == null || location.isEmpty()
                || quantity == null || reorderLevel == null
                || quantity < 0 || reorderLevel < 0) {
            showError("Enter a product, location, and nonnegative quantity and reorder level.");
            return;
        }
        try {
    inventoryService.save(
            new Inventory(product, location, quantity, reorderLevel)
    );

    clearInventoryForm();
    refreshData();

    showSuccess("Inventory record added successfully.");

} catch (IllegalArgumentException | IllegalStateException exception) {
    showError(exception.getMessage());

} catch (Exception exception) {
    showError("Could not add inventory record.");
}
    }
    private void addQuantity() {
        if (!hasSelectedInventory()) {
            return;
        }
        Integer amount = adjustmentField.getValue();
        if (amount == null || amount <= 0) {
            showError("Enter a quantity greater than zero.");
            return;
        }
        try {
            inventoryService.addQuantity(
                    selectedInventory.getId(),
                    amount
            );
            showSuccess(amount + " item(s) added successfully.");
            clearStockActionFields();
            refreshData();
        }  catch (IllegalArgumentException | IllegalStateException exception) {
    showError(exception.getMessage());
}
    }

    private void removeQuantity() {
        if (!hasSelectedInventory()) {
            return;
        }
        Integer amount = adjustmentField.getValue();
        if (amount == null || amount <= 0) {
            showError("Enter a quantity greater than zero.");
            return;
        }
        try {
            inventoryService.removeQuantity(
                    selectedInventory.getId(),
                    amount
            );
            showSuccess(amount + " item(s) removed successfully.");
            clearStockActionFields();
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException exception) {
                showError(exception.getMessage());
        }
    }

    private void updateReorderLevel() {
        if (!hasSelectedInventory()) {
            return;
        }
        Integer newReorderLevel = updateReorderLevelField.getValue();
        if (newReorderLevel == null || newReorderLevel < 0) {
            showError("Enter a valid reorder level of zero or greater.");
            return;
        }
        try {
            inventoryService.updateReorderLevel(
                    selectedInventory.getId(),
                    newReorderLevel
            );
            showSuccess("Reorder level updated successfully.");
            refreshData();
        } catch (IllegalArgumentException | IllegalStateException exception) {
                showError(exception.getMessage());
        }
    }

    private void updateInventory() {
        if (!hasSelectedInventory()) {
            return;
        }
        Product product = productBox.getValue();
        String location = locationField.getValue().trim();
        Integer quantity = quantityField.getValue();
        Integer reorderLevel = reorderLevelField.getValue();
        if (product == null || location.isEmpty()
                || quantity == null || reorderLevel == null
                || quantity < 0 || reorderLevel < 0) {
            showError("Enter a product, location, and nonnegative quantity and reorder level.");
            return;
        }
        try {
            
inventoryService.updateInventory(
        selectedInventory.getId(),
        product,
        location,
        quantity,
        reorderLevel
);
            clearInventoryForm();
            refreshData();
            showSuccess("Inventory record updated successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
                showError(exception.getMessage());
        } catch (Exception exception) {
            showError("Could not update inventory record.");
        }
    }

    private void deleteInventory() {
    if (!hasSelectedInventory()) {
        return;
    }
    try {
        inventoryService.deleteById(selectedInventory.getId());
        showSuccess("Inventory record deleted successfully.");
        clearInventoryForm();
        refreshData();
    
    } catch (IllegalArgumentException | IllegalStateException exception) {
    showError(exception.getMessage());
    } catch (Exception exception) {
    showError("Could not delete inventory record.");
    }

    }

    private boolean hasSelectedInventory() {
        if (selectedInventory != null) {
            return true;
        }
        showError(
                "Select an inventory record from the table first."
        );
        return false;
    }

    private void clearStockActionFields() {
        adjustmentField.clear();
    }

    private void clearInventoryForm() {
    productBox.clear();
    locationField.clear();
    quantityField.setValue(0);
    reorderLevelField.setValue(0);
    adjustmentField.clear();
    updateReorderLevelField.clear();
    selectedInventory = null;
    inventoryGrid.asSingleSelect().clear();
    selectedInventoryText.setText(
            "No inventory record selected."
    );
}

    private void refreshData() {
        List<Product> products = productService.findAll();
        productBox.setItems(products);
        Set<String> locations = inventoryService.findAll()
                .stream()
                .map(Inventory::getLocation)
                .filter(location -> location != null && !location.isBlank())
                .collect(Collectors.toSet());
        List<String> sortedLocations = locations.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        locationFilter.setItems(sortedLocations);
        selectedInventory = null;
        inventoryGrid.asSingleSelect().clear();
        selectedInventoryText.setText(
                "No inventory record selected."
        );
        adjustmentField.clear();
        updateReorderLevelField.clear();
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getValue() == null
                ? ""
                : searchField.getValue()
                        .trim()
                        .toLowerCase(Locale.ROOT);
        String selectedLocation = locationFilter.getValue();
        boolean showLowStockOnly = lowStockOnly.getValue();
        List<Inventory> filteredInventory = inventoryService.findAll()
                .stream()
                .filter(item -> matchesSearch(item, searchText))
                .filter(item -> matchesLocation(item, selectedLocation))
                .filter(item -> !showLowStockOnly || isLowStock(item))
                .sorted(Comparator.comparing(
                        item -> item.getProduct().getName(),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
        inventoryGrid.setItems(filteredInventory);
        recordCount.setText(
                "Showing " + filteredInventory.size()
                        + " inventory record(s)"
        );
        recordCount.getStyle()
                .set("color", "#475569")
                .set("font-size", "0.9rem");
    }

    private boolean matchesSearch(Inventory item, String searchText) {
        if (searchText.isBlank()) {
            return true;
        }
        String productName = item.getProduct().getName()
                .toLowerCase(Locale.ROOT);
        String sku = item.getProduct().getSku()
                .toLowerCase(Locale.ROOT);
        String location = item.getLocation() == null
                ? ""
                : item.getLocation().toLowerCase(Locale.ROOT);
        return productName.contains(searchText)
                || sku.contains(searchText)
                || location.contains(searchText);
    }

    private boolean matchesLocation(
            Inventory item,
            String selectedLocation) {
        if (selectedLocation == null || selectedLocation.isBlank()) {
            return true;
        }
        return selectedLocation.equals(item.getLocation());
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
