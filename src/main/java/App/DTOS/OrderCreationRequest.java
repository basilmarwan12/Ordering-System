package App.DTOS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a new order with items.
 * The order subtotal and total are calculated automatically based on products and quantities.
 * Discount amount is optional and defaults to 0.
 */
@Getter
@Setter
public class OrderCreationRequest {

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Discount amount is required")
    @DecimalMin(
            value = "0.0",
            message = "Discount amount cannot be negative"
    )
    private BigDecimal discountAmount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Billing address is required")
    private String billingAddress;

    public OrderCreationRequest() {
        this.discountAmount = BigDecimal.ZERO;
    }
}