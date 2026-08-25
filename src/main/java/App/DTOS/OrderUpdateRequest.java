package App.DTOS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Partial update. Every field is optional: a null value leaves the
 * corresponding column untouched. Bean Validation skips null values for
 * these constraints, so they only apply to the fields actually supplied.
 */
@Getter
@Setter
public class OrderUpdateRequest {

    @DecimalMin(
            value = "0.0",
            message = "Discount amount cannot be negative"
    )
    private BigDecimal discountAmount;

    @Pattern(
            regexp = "PENDING|CONFIRMED|CANCELLED|COMPLETED",
            message = "Status must be one of PENDING, CONFIRMED, CANCELLED, COMPLETED"
    )
    private String status;

    @Pattern(
            regexp = "PENDING|PAID|REFUNDED|FAILED",
            message = "Payment status must be one of PENDING, PAID, REFUNDED, FAILED"
    )
    private String paymentStatus;

    @Pattern(
            regexp = "NOT_SHIPPED|SHIPPED|DELIVERED|RETURNED",
            message = "Shipping status must be one of NOT_SHIPPED, SHIPPED, DELIVERED, RETURNED"
    )
    private String shippingStatus;

    @Pattern(
            regexp = ".*\\S.*",
            message = "Payment method cannot be blank"
    )
    private String paymentMethod;

    @Pattern(
            regexp = ".*\\S.*",
            message = "Shipping address cannot be blank"
    )
    private String shippingAddress;

    @Pattern(
            regexp = ".*\\S.*",
            message = "Billing address cannot be blank"
    )
    private String billingAddress;

    @Valid
    private List<OrderItemRequest> items;
}
