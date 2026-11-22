
package org.vaadin.example;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            CustomerOrderRepository customerOrderRepository) {

        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Transactional
    public Inventory save(Inventory inventory) {

        validateInventory(inventory);

        Long productId;

        if (inventory.getId() == null) {
            productId = inventory.getProduct().getId();
        } else {
            Inventory existing = getInventory(inventory.getId());
            productId = existing.getProduct().getId();

            if (!productId.equals(inventory.getProduct().getId())) {
                throw new IllegalArgumentException(
                        "Changing the product of an existing stock "
                                + "record is not supported."
                );
            }
        }

        lockProduct(productId);

        if (inventory.getId() == null) {

            inventory.setLocation(inventory.getLocation().trim());

            return inventoryRepository.save(inventory);
        }

        Inventory existing = getInventory(inventory.getId());

        if (!existing.getProduct().getId().equals(productId)) {
            throw new IllegalStateException(
                    "Inventory product changed during update."
            );
        }

        long proposedTotal =
                getTotalPhysicalStock(productId)
                        - existing.getQuantity()
                        + inventory.getQuantity();

        validateReservedStock(productId, proposedTotal);

        existing.setLocation(inventory.getLocation().trim());
        existing.setQuantity(inventory.getQuantity());
        existing.setReorderLevel(inventory.getReorderLevel());

        return inventoryRepository.save(existing);
    }

    @Transactional
    public void deleteById(Long inventoryId) {

        Inventory inventory = getInventory(inventoryId);

        Long productId = inventory.getProduct().getId();

        lockProduct(productId);

        inventory = getInventory(inventoryId);

        long proposedTotal =
                getTotalPhysicalStock(productId)
                        - inventory.getQuantity();

        validateReservedStock(productId, proposedTotal);

        inventoryRepository.delete(inventory);
    }

    @Transactional
    public Inventory addQuantity(Long inventoryId, int amount) {

        validatePositiveAmount(amount);

        Inventory inventory = getInventory(inventoryId);

        lockProduct(inventory.getProduct().getId());

        inventory = getInventory(inventoryId);

        try {
            inventory.setQuantity(
                    Math.addExact(inventory.getQuantity(), amount)
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Quantity exceeds the supported limit."
            );
        }

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory removeQuantity(Long inventoryId, int amount) {

        validatePositiveAmount(amount);

        Inventory inventory = getInventory(inventoryId);

        Long productId = inventory.getProduct().getId();

        lockProduct(productId);

        inventory = getInventory(inventoryId);

        if (inventory.getQuantity() < amount) {
            throw new IllegalArgumentException(
                    "Not enough stock in this location. Available: "
                            + inventory.getQuantity()
            );
        }

        long proposedTotal =
                getTotalPhysicalStock(productId) - amount;

        validateReservedStock(productId, proposedTotal);

        inventory.setQuantity(inventory.getQuantity() - amount);

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory updateReorderLevel(
            Long inventoryId,
            int reorderLevel) {

        if (reorderLevel < 0) {
            throw new IllegalArgumentException(
                    "Reorder level cannot be negative."
            );
        }

        Inventory inventory = getInventory(inventoryId);

        lockProduct(inventory.getProduct().getId());

        inventory = getInventory(inventoryId);
        inventory.setReorderLevel(reorderLevel);

        return inventoryRepository.save(inventory);
    }
    
    
@Transactional
public Inventory updateInventory(
        Long inventoryId,
        Product product,
        String location,
        Integer quantity,
        Integer reorderLevel) {

    Inventory requested = new Inventory(
            product, location, quantity, reorderLevel
    );

    validateInventory(requested);

    Inventory existing = getInventory(inventoryId);
    Long productId = existing.getProduct().getId();

    if (!productId.equals(product.getId())) {
        throw new IllegalArgumentException(
                "Changing the product of an existing stock record is not supported."
        );
    }

    lockProduct(productId);

    existing = getInventory(inventoryId);

    long proposedTotal =
            getTotalPhysicalStock(productId)
                    - existing.getQuantity()
                    + quantity;

    validateReservedStock(productId, proposedTotal);

    existing.setLocation(location.trim());
    existing.setQuantity(quantity);
    existing.setReorderLevel(reorderLevel);

    return inventoryRepository.save(existing);
}


    private Inventory getInventory(Long inventoryId) {

        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory record not found."
                ));
    }

    private void lockProduct(Long productId) {

        productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Product not found."
                ));
    }

    private long getTotalPhysicalStock(Long productId) {

        return inventoryRepository.findByProductId(productId)
                .stream()
                .mapToLong(Inventory::getQuantity)
                .sum();
    }

    private void validateReservedStock(
            Long productId,
            long proposedTotal) {

        Long reserved = customerOrderRepository
                .sumQuantityByProductIdAndStatus(
                        productId,
                        OrderStatus.NEW
                );

        long reservedQuantity = reserved == null ? 0 : reserved;

        if (proposedTotal < reservedQuantity) {
            throw new IllegalStateException(
                    "Cannot reduce inventory. "
                            + reservedQuantity
                            + " item(s) are reserved for NEW orders."
            );
        }
    }

    private void validatePositiveAmount(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Enter a quantity greater than zero."
            );
        }
    }

    private void validateInventory(Inventory inventory) {

        if (inventory == null
                || inventory.getProduct() == null
                || inventory.getProduct().getId() == null) {

            throw new IllegalArgumentException(
                    "Please select a valid product."
            );
        }

        if (inventory.getLocation() == null
                || inventory.getLocation().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Location cannot be empty."
            );
        }

        if (inventory.getQuantity() == null
                || inventory.getQuantity() < 0) {

            throw new IllegalArgumentException(
                    "Quantity cannot be negative."
            );
        }

        if (inventory.getReorderLevel() == null
                || inventory.getReorderLevel() < 0) {

            throw new IllegalArgumentException(
                    "Reorder level cannot be negative."
            );
        }
    }
}
