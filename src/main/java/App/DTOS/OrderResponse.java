package App.DTOS;

import App.Models.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String number;
    private BigDecimal subtotal;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String status;
    private String paymentStatus;
    private String shippingStatus;
    private String paymentMethod;
    private String shippingAddress;
    private String billingAddress;

    // The owner is exposed as an identifier only: serializing the User
    // relation would leak the owner's email, phone, address and birthday.
    private UUID userId;

    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {

        List<OrderItemResponse> items = order.getItems() != null
                ? order.getItems().stream()
                    .map(item -> new OrderItemResponse(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    ))
                    .collect(Collectors.toList())
                : List.of();

        return new OrderResponse(
                order.getId(),
                order.getNumber(),
                order.getSubtotal(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getShippingStatus(),
                order.getPaymentMethod(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getUser() != null
                        ? order.getUser().getUuid()
                        : null,
                items
        );
    }
}
