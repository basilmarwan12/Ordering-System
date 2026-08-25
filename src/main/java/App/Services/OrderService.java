package App.Services;

import App.DTOS.OrderCreationRequest;
import App.Middlewares.Auth.UserNotFoundException;
import App.Models.Order;
import App.Models.User;
import App.Repositories.OrderRepository;
import App.Repositories.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order createOrder(
            OrderCreationRequest request,
            String userId
    ) {

        UUID uuid = UUID.fromString(userId);

        User user = userRepository
                .findById(uuid)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        if (request.getDiscountAmount()
                .compareTo(request.getSubtotal()) > 0) {

            throw new IllegalArgumentException(
                    "Discount amount cannot be greater than subtotal"
            );
        }

        Order order = new Order();

        order.setNumber(generateOrderNumber());

        order.setUser(user);

        order.setSubtotal(
                request.getSubtotal()
        );

        order.setDiscountAmount(
                request.getDiscountAmount()
        );

        BigDecimal totalAmount =
                request.getSubtotal()
                        .subtract(
                                request.getDiscountAmount()
                        );

        order.setTotalAmount(totalAmount);

        order.setPaymentMethod(
                request.getPaymentMethod()
        );

        order.setShippingAddress(
                request.getShippingAddress()
        );

        order.setBillingAddress(
                request.getBillingAddress()
        );

        // Backend-controlled values
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");
        order.setShippingStatus("NOT_SHIPPED");

        return orderRepository.save(order);
    }


    private String generateOrderNumber() {

        return "ORD-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }
}