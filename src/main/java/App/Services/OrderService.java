package App.Services;

import App.DTOS.OrderCreationRequest;
import App.DTOS.OrderItemRequest;
import App.DTOS.OrderUpdateRequest;
import App.Middlewares.Auth.UserNotFoundException;
import App.Middlewares.Orders.InvalidOrderAmountsException;
import App.Middlewares.Orders.OrderNotFoundException;
import App.Models.Order;
import App.Models.OrderItem;
import App.Models.Product;
import App.Models.User;
import App.Repositories.OrderItemRepository;
import App.Repositories.OrderRepository;
import App.Repositories.ProductRepository;
import App.Repositories.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
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

        // Calculate subtotal from items and products
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() ->
                            new InvalidOrderAmountsException(
                                    "Product with id " + itemRequest.getProductId() + " not found"
                            )
                    );

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            
            subtotal = subtotal.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            
            orderItems.add(orderItem);
        }

        // Validate discount amount against calculated subtotal
        if (request.getDiscountAmount()
                .compareTo(subtotal) > 0) {

            throw new InvalidOrderAmountsException(
                    "Discount amount cannot be greater than subtotal"
            );
        }

        Order order = new Order();

        order.setNumber(generateOrderNumber());

        order.setUser(user);

        order.setSubtotal(subtotal);

        order.setDiscountAmount(
                request.getDiscountAmount()
        );

        BigDecimal totalAmount = subtotal
                .subtract(request.getDiscountAmount());

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

        // Save order first
        Order savedOrder = orderRepository.save(order);

        // Link items to saved order and save items
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);
        
        // Update order with items
        savedOrder.setItems(orderItems);

        return savedOrder;
    }


    @Transactional(readOnly = true)
    public Order getOrderById(
            Long id,
            UUID callerId,
            boolean callerIsAdmin
    ) {

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found")
                );

        if (!callerIsAdmin
                && !order.getUser().getUuid().equals(callerId)) {

            throw new AccessDeniedException("Access denied");
        }

        return order;
    }


    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(
            UUID userId,
            UUID callerId,
            boolean callerIsAdmin
    ) {

        requireOwnerOrAdmin(userId, callerId, callerIsAdmin);

        return orderRepository.findByUserUuid(userId);
    }


    @Transactional(readOnly = true)
    public Page<Order> listOrdersByUserId(
            UUID userId,
            UUID callerId,
            boolean callerIsAdmin,
            Pageable pageable
    ) {

        requireOwnerOrAdmin(userId, callerId, callerIsAdmin);

        return orderRepository.findByUserUuid(userId, pageable);
    }


    /**
     * A customer may only read their own orders; an admin may read anyone's.
     * The ownership check runs before the existence check so that a customer
     * cannot probe for valid user ids.
     */
    private void requireOwnerOrAdmin(
            UUID userId,
            UUID callerId,
            boolean callerIsAdmin
    ) {

        if (!callerIsAdmin && !userId.equals(callerId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found");
        }
    }


    @Transactional
    public Order updateOrder(
            Long id,
            OrderUpdateRequest request
    ) {

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found")
                );

        // Handle items update if provided
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            
            // Calculate new subtotal from items
            BigDecimal newSubtotal = BigDecimal.ZERO;
            List<OrderItem> newOrderItems = new ArrayList<>();

            for (OrderItemRequest itemRequest : request.getItems()) {
                Product product = productRepository
                        .findById(itemRequest.getProductId())
                        .orElseThrow(() ->
                                new InvalidOrderAmountsException(
                                        "Product with id " + itemRequest.getProductId() + " not found"
                                )
                        );

                BigDecimal itemTotal = product.getPrice()
                        .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                
                newSubtotal = newSubtotal.add(itemTotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setUnitPrice(product.getPrice());
                
                newOrderItems.add(orderItem);
            }

            // Delete old items and save new ones
            orderItemRepository.deleteAll(order.getItems());
            orderItemRepository.saveAll(newOrderItems);
            
            order.setItems(newOrderItems);
            order.setSubtotal(newSubtotal);
        }

        if (request.getDiscountAmount() != null) {
            order.setDiscountAmount(request.getDiscountAmount());
        }

        // Validate discount amount against current subtotal
        if (order.getDiscountAmount()
                .compareTo(order.getSubtotal()) > 0) {

            throw new InvalidOrderAmountsException(
                    "Discount amount cannot be greater than subtotal"
            );
        }

        // totalAmount is derived, never client-supplied.
        order.setTotalAmount(
                order.getSubtotal()
                        .subtract(order.getDiscountAmount())
        );

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(request.getPaymentStatus());
        }

        if (request.getShippingStatus() != null) {
            order.setShippingStatus(request.getShippingStatus());
        }

        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        }

        if (request.getShippingAddress() != null) {
            order.setShippingAddress(request.getShippingAddress());
        }

        if (request.getBillingAddress() != null) {
            order.setBillingAddress(request.getBillingAddress());
        }

        return orderRepository.save(order);
    }


    @Transactional
    public void deleteOrder(Long id) {

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found")
                );

        orderRepository.delete(order);
    }


    private String generateOrderNumber() {

        return "ORD-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }
}