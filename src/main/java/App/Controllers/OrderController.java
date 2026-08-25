package App.Controllers;

import App.DTOS.OrderCreationRequest;
import App.Models.Order;
import App.Services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String getOrders() {
        return "Orders endpoint reached";
    }

    @PostMapping("/createOrder")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody OrderCreationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        String userId = jwt.getClaim("userId");

        Order order =
                orderService.createOrder(
                        request,
                        userId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }
}