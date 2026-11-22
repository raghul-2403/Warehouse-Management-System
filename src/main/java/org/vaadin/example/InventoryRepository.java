
package org.vaadin.example;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findFirstByProductId(Long productId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByProductIdOrderByIdAsc(Long productId);
}
