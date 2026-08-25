package App.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private String status;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "shipping_status", nullable = false)
    private String shippingStatus;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "billing_address")
    private String billingAddress;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @JsonIgnore
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Order(
            Long id,
            String number,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            String status,
            String paymentStatus,
            String shippingStatus,
            String paymentMethod,
            String shippingAddress,
            String billingAddress,
            User user
    ) {
        this.id = id;
        this.number = number;
        this.subtotal = subtotal;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.shippingStatus = shippingStatus;
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.user = user;
    }

}