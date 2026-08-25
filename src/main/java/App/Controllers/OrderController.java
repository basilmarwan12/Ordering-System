package App.Controllers;

import App.DTOS.OrderCreationRequest;
import App.DTOS.OrderResponse;
import App.DTOS.OrderUpdateRequest;
import App.Models.Order;
import App.Services.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Order order = orderService.getOrderById(
                id,
                callerId(jwt),
                isAdmin(jwt)
        );

        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        List<OrderResponse> orders =
                orderService.getOrdersByUserId(
                                userId,
                                callerId(jwt),
                                isAdmin(jwt)
                        )
                        .stream()
                        .map(OrderResponse::from)
                        .toList();

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<PagedModel<OrderResponse>> listOrdersByUserId(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Page<OrderResponse> page =
                orderService.listOrdersByUserId(
                                userId,
                                callerId(jwt),
                                isAdmin(jwt),
                                pageable
                        )
                        .map(OrderResponse::from);

        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PostMapping("/createOrder")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
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
                .body(OrderResponse.from(order));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request
    ) {

        Order order = orderService.updateOrder(id, request);

        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id
    ) {

        orderService.deleteOrder(id);

        return ResponseEntity
                .noContent()
                .build();
    }


    private static UUID callerId(Jwt jwt) {
        return UUID.fromString(Objects.requireNonNull(jwt.getClaim("userId")));
    }

    /**
     * Mirrors the "role" claim that SecurityConfig maps onto ROLE_ authorities,
     * so the read checks in OrderService use the same source of truth as
     * the @PreAuthorize annotations.
     */
    private static boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaim("role"));
    }
}