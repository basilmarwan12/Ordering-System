package App.Services;

import App.DTOS.OrderCreationRequest;
import App.DTOS.OrderItemRequest;
import App.DTOS.OrderUpdateRequest;
import App.Middlewares.Auth.UserNotFoundException;
import App.Middlewares.Orders.InvalidOrderAmountsException;
import App.Middlewares.Orders.InvalidOrderStateException;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private static final Map<String, Set<String>> VALID_STATUS_TRANSITIONS = Map.of(
            "CREATED", Set.of("PAID", "CANCELLED"),
            "PAID", Set.of("SHIPPED"),
            "SHIPPED", Set.of("DELIVERED")
    );

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


    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(
            OrderCreationRequest request,
            String userId
    ) {

        User user = loadUser(userId);
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = getProductOrThrow(itemRequest.getProductId());
            validateItemQuantity(product, itemRequest.getQuantity());

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItems.add(orderItem);

            product.setStock(product.getStock() - itemRequest.getQuantity());
        }

        validateDiscount(request.getDiscountAmount(), subtotal);

        Order order = new Order();
        order.setNumber(generateOrderNumber());
        order.setUser(user);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(request.getDiscountAmount());
        order.setTotalAmount(subtotal.subtract(request.getDiscountAmount()));
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setBillingAddress(request.getBillingAddress());
        order.setStatus("CREATED");
        order.setPaymentStatus("PENDING");
        order.setShippingStatus("NOT_SHIPPED");

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);
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


    @Transactional(rollbackFor = Exception.class)
    public Order updateOrder(
            Long id,
            OrderUpdateRequest request,
            boolean callerIsAdmin
    ) {

        Order order = getOrderOrThrow(id);
        validateStatusUpdate(request, order, callerIsAdmin);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            BigDecimal newSubtotal = calculateSubtotal(request.getItems());
            List<OrderItem> newOrderItems = buildOrderItems(order, request.getItems());

            orderItemRepository.deleteAll(order.getItems());
            orderItemRepository.saveAll(newOrderItems);

            order.setItems(newOrderItems);
            order.setSubtotal(newSubtotal);
        }

        if (request.getDiscountAmount() != null) {
            order.setDiscountAmount(request.getDiscountAmount());
        }

        validateDiscount(order.getDiscountAmount(), order.getSubtotal());
        order.setTotalAmount(order.getSubtotal().subtract(order.getDiscountAmount()));

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        applyOptionalFieldUpdates(order, request);

        return orderRepository.save(order);
    }

    private void validateStatusUpdate(OrderUpdateRequest request, Order order, boolean callerIsAdmin) {
        if (request.getStatus() == null) {
            return;
        }

        validateStatusTransition(order.getStatus(), request.getStatus());

        if (("SHIPPED".equals(request.getStatus()) || "DELIVERED".equals(request.getStatus()))
                && !callerIsAdmin) {
            throw new AccessDeniedException("Only admins can change order status to SHIPPED or DELIVERED");
        }

        if ("SHIPPED".equals(request.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new InvalidOrderStateException("An order cannot be shipped unless it is paid");
        }

        if ("PAID".equals(request.getStatus()) && "CANCELLED".equals(order.getStatus())) {
            throw new InvalidOrderStateException("An order cannot be paid if it is cancelled");
        }
    }

    private void applyOptionalFieldUpdates(Order order, OrderUpdateRequest request) {
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
    }

    private BigDecimal calculateSubtotal(List<OrderItemRequest> itemRequests) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = getProductOrThrow(itemRequest.getProductId());
            validateItemQuantity(product, itemRequest.getQuantity());
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        return subtotal;
    }

    private List<OrderItem> buildOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = getProductOrThrow(itemRequest.getProductId());
            validateItemQuantity(product, itemRequest.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(product.getPrice());
            orderItems.add(item);
        }

        return orderItems;
    }

    private void validateItemQuantity(Product product, int quantity) {
        if (quantity <= 0) {
            throw new InvalidOrderAmountsException(
                    "Order quantity must be greater than zero for product " + product.getId()
            );
        }

        if (quantity > product.getStock()) {
            throw new InvalidOrderAmountsException(
                    "Insufficient stock for product " + product.getName()
            );
        }
    }

    private void validateDiscount(BigDecimal discountAmount, BigDecimal subtotal) {
        if (discountAmount.compareTo(subtotal) > 0) {
            throw new InvalidOrderAmountsException(
                    "Discount amount cannot be greater than subtotal"
            );
        }
    }

    private User loadUser(String userId) {
        UUID uuid = UUID.fromString(userId);
        return userRepository
                .findById(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new InvalidOrderAmountsException(
                        "Product with id " + productId + " not found"
                ));
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository
                .findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    private void validateStatusTransition(String currentStatus, String nextStatus) {
        String normalizedCurrent = currentStatus == null ? "CREATED" : currentStatus.trim();
        String normalizedNext = nextStatus == null ? null : nextStatus.trim();

        if (normalizedNext == null) {
            return;
        }

        if ("DELIVERED".equals(normalizedCurrent) || "CANCELLED".equals(normalizedCurrent)) {
            throw new InvalidOrderStateException(
                    "No transitions are allowed from " + normalizedCurrent + " state"
            );
        }

        Set<String> validTransitions = VALID_STATUS_TRANSITIONS.getOrDefault(normalizedCurrent, Set.of());
        if (!validTransitions.contains(normalizedNext)) {
            throw new InvalidOrderStateException(
                    "Invalid order status transition from " + normalizedCurrent + " to " + normalizedNext
            );
        }
    }


    @Transactional(rollbackFor = Exception.class)
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