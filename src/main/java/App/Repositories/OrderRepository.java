package App.Repositories;

import App.Models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    boolean existsByUserUuid(UUID uuid);

    List<Order> findByUserUuid(UUID uuid);

    Page<Order> findByUserUuid(UUID uuid, Pageable pageable);
}
