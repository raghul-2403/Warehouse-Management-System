
package org.vaadin.example;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByStatus(OrderStatus status);

    @Query("""
        SELECT COALESCE(SUM(o.quantity), 0)
        FROM CustomerOrder o
        WHERE o.product.id = :productId
          AND o.status = :status
        """)
    Long sumQuantityByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status") OrderStatus status
    );
}
