package App.DTOS;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderCreationRequest {

    @NotNull(message = "Subtotal is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Subtotal must be greater than 0"
    )
    private BigDecimal subtotal;

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
}