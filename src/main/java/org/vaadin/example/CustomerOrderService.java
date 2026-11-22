
package org.vaadin.example;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public CustomerOrderService(
            CustomerOrderRepository customerOrderRepository,
            InventoryRepository inventoryRepository,
            ProductRepository productRepository) {

        this.customerOrderRepository = customerOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public List<CustomerOrder> findAll() {
        return customerOrderRepository.findAll();
    }

    @Transactional
    public CustomerOrder save(CustomerOrder order) {

        if (order == null
                || order.getCustomer() == null
                || order.getProduct() == null
                || order.getProduct().getId() == null
                || order.getQuantity() == null
                || order.getQuantity() <= 0) {

            throw new IllegalArgumentException(
                    "Enter valid order details."
            );
        }

        if (order.getId() != null
                || order.getStatus() != OrderStatus.NEW) {
            throw new IllegalArgumentException(
                    "Only new orders can be created."
            );
        }

        Long productId = order.getProduct().getId();

        lockProduct(productId);

        int available = calculateAvailableQuantity(productId);

        if (order.getQuantity() > available) {
            throw new IllegalStateException(
                    "Not enough stock available. Available: "
                            + available
            );
        }

        return customerOrderRepository.save(order);
    }

    @Transactional
    public void moveToNextStatus(CustomerOrder order) {

        if (order == null || order.getId() == null
                || order.getProduct() == null) {
            throw new IllegalArgumentException(
                    "Invalid order."
            );
        }

        Long productId = order.getProduct().getId();

        lockProduct(productId);

        // Retrieve the latest status from the database.
        CustomerOrder currentOrder = customerOrderRepository
                .findById(order.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found."
                ));

        if (!currentOrder.getProduct().getId().equals(productId)) {
            throw new IllegalStateException(
                    "Order product has changed."
            );
        }

        if (currentOrder.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
            "Cancelled orders cannot be processed."
    );
} 

        if (currentOrder.getStatus() == OrderStatus.NEW) {

            reduceStockAcrossLocations(currentOrder);
            currentOrder.setStatus(OrderStatus.PICKED);

        } else if (currentOrder.getStatus() == OrderStatus.PICKED) {

            currentOrder.setStatus(OrderStatus.PACKED);

        } else if (currentOrder.getStatus() == OrderStatus.PACKED) {

            currentOrder.setStatus(OrderStatus.SHIPPED);

        } else {

            throw new IllegalStateException(
                    "This order has already been shipped."
            );
        }

        customerOrderRepository.save(currentOrder);
    }

    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long productId) {

        return calculateAvailableQuantity(productId);
    }

    private int calculateAvailableQuantity(Long productId) {

        List<Inventory> inventoryRecords =
                inventoryRepository.findByProductId(productId);

        if (inventoryRecords.isEmpty()) {
            throw new IllegalStateException(
                    "No inventory record found for this product."
            );
        }

        long totalStock = inventoryRecords.stream()
                .mapToLong(Inventory::getQuantity)
                .sum();

        long reservedStock = customerOrderRepository
                .sumQuantityByProductIdAndStatus(
                        productId,
                        OrderStatus.NEW
                );

        long available = totalStock - reservedStock;

        if (available < 0) {
            throw new IllegalStateException(
                    "Reserved stock exceeds physical inventory."
            );
        }

        return Math.toIntExact(available);
    }

    private void reduceStockAcrossLocations(CustomerOrder order) {

        List<Inventory> records = inventoryRepository
                .findByProductIdOrderByIdAsc(
                        order.getProduct().getId()
                );

        long totalStock = records.stream()
                .mapToLong(Inventory::getQuantity)
                .sum();

        if (totalStock < order.getQuantity()) {
            throw new IllegalStateException(
                    "Insufficient physical stock to pick this order."
            );
        }

        int remaining = order.getQuantity();

        for (Inventory inventory : records) {

            if (remaining == 0) {
                break;
            }

            int available = inventory.getQuantity();

            int toDeduct = Math.min(available, remaining);

            inventory.setQuantity(available - toDeduct);

            inventoryRepository.save(inventory);

            remaining -= toDeduct;
        }

        if (remaining != 0) {
            throw new IllegalStateException(
                    "Could not fully allocate order stock."
            );
        }
    }

    private void lockProduct(Long productId) {

        productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Product not found."
                ));
    }

    
@Transactional
public void cancelOrder(Long orderId) {

    CustomerOrder initialOrder = customerOrderRepository
            .findById(orderId)
            .orElseThrow(() -> new IllegalStateException(
                    "Order not found."
            ));

    Long productId = initialOrder.getProduct().getId();

    // Use the same product lock as order creation.
    lockProduct(productId);

    // Read the latest order state after acquiring the lock.
    CustomerOrder currentOrder = customerOrderRepository
            .findById(orderId)
            .orElseThrow(() -> new IllegalStateException(
                    "Order not found."
            ));

    if (currentOrder.getStatus() != OrderStatus.NEW) {
        throw new IllegalStateException(
                "Only NEW orders can be cancelled."
        );
    }

    currentOrder.setStatus(OrderStatus.CANCELLED);

    customerOrderRepository.save(currentOrder);
}

public CustomerOrder findById(Long orderId) {

    return customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException(
                    "Order not found."
            ));
}

}
