package App.Services;

import App.Models.Order;
import App.Models.OrderItem;
import App.Models.Product;
import App.Models.User;
import App.Repositories.OrderItemRepository;
import App.Repositories.OrderRepository;
import App.Repositories.ProductRepository;
import App.Repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeederService {

    private static final Logger log = LoggerFactory.getLogger(SeederService.class);

    private static final String EMAIL_DOMAIN = "@seed.local";
    private static final String NUMBER_PREFIX = "SEED-";

    public static final String PASSWORD = "Password1*";

    private static final String[] STATUSES = {
            "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"
    };

    private static final String[] PAYMENT_STATUSES = {
            "PENDING", "PAID", "REFUNDED", "FAILED"
    };

    private static final String[] SHIPPING_STATUSES = {
            "NOT_SHIPPED", "SHIPPED", "DELIVERED", "RETURNED"
    };

    private static final String[] PAYMENT_METHODS = {
            "CARD", "CASH_ON_DELIVERY", "BANK_TRANSFER", "WALLET"
    };

    private static final int[] ORDERS_PER_CUSTOMER = {
            12, 6, 4, 2, 1, 0, 0, 0
    };

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    public SeederService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String seedData() {
        String message = checkAndSeed();
        return message;
    }

    private String checkAndSeed() {
        if (userRepository.existsByEmail("admin1" + EMAIL_DOMAIN)) {
            String msg = "Seed data already present. To re-seed, first delete existing seed data.";
            log.info(msg);
            return msg;
        }

        String encodedPassword = passwordEncoder.encode(PASSWORD);

        List<User> admins = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            admins.add(
                    buildUser(i, "Admin", "User", "ADMIN", encodedPassword)
            );
        }

        List<User> customers = new ArrayList<>();

        for (int i = 1; i <= ORDERS_PER_CUSTOMER.length; i++) {
            customers.add(
                    buildUser(i, "Customer", "Number" + i, "CUSTOMER", encodedPassword)
            );
        }

        userRepository.saveAll(admins);
        userRepository.saveAll(customers);
        log.info("Saved {} admins and {} customers", admins.size(), customers.size());

        List<Product> products = new ArrayList<>();
        products.add(buildProduct(1, "Laptop", "High-performance laptop for professionals", new BigDecimal("1299.99"), 15));
        products.add(buildProduct(2, "Wireless Mouse", "Ergonomic wireless mouse with precision tracking", new BigDecimal("29.99"), 50));
        products.add(buildProduct(3, "USB-C Cable", "Durable USB-C charging and data cable", new BigDecimal("12.99"), 100));
        products.add(buildProduct(4, "Mechanical Keyboard", "RGB mechanical gaming keyboard", new BigDecimal("149.99"), 25));
        products.add(buildProduct(5, "Monitor 4K", "27-inch 4K ultra HD monitor", new BigDecimal("599.99"), 10));
        products.add(buildProduct(6, "Headphones", "Noise-cancelling wireless headphones", new BigDecimal("199.99"), 30));

        productRepository.saveAll(products);
        log.info("Saved {} products", products.size());

        List<Order> orders = new ArrayList<>();
        int sequence = 0;

        for (int c = 0; c < customers.size(); c++) {
            for (int n = 0; n < ORDERS_PER_CUSTOMER[c]; n++) {
                orders.add(buildOrder(customers.get(c), ++sequence));
            }
        }

        orderRepository.saveAll(orders);
        log.info("Saved {} orders", orders.size());

        List<OrderItem> orderItems = new ArrayList<>();
        int itemSequence = 0;

        for (Order order : orders) {
            int itemCount = 1 + (itemSequence % 3);

            for (int i = 0; i < itemCount; i++) {
                Product product = products.get((itemSequence + i) % products.size());
                int quantity = 1 + ((itemSequence + i) % 5);

                orderItems.add(buildOrderItem(
                        order,
                        product,
                        quantity,
                        product.getPrice()
                ));
                itemSequence++;
            }
        }

        orderItemRepository.saveAll(orderItems);
        log.info("Saved {} order items", orderItems.size());

        String successMsg = String.format(
                "Seeded %d admins, %d customers, %d orders, %d products, and %d order items.",
                admins.size(),
                customers.size(),
                orders.size(),
                products.size(),
                orderItems.size()
        );
        log.info(successMsg);

        log.info(
                "Log in as admin1{} / {} to get an ADMIN token.",
                EMAIL_DOMAIN,
                PASSWORD
        );

        customers.forEach(customer ->
                log.info(
                        "  customer {} -> uuid {}",
                        customer.getEmail(),
                        customer.getUuid()
                )
        );

        return successMsg;
    }

    private User buildUser(
            int index,
            String firstName,
            String lastName,
            String role,
            String encodedPassword
    ) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(
                role.equals("ADMIN")
                        ? "admin" + index + EMAIL_DOMAIN
                        : "customer" + index + EMAIL_DOMAIN
        );
        user.setPhoneNumber(
                role.equals("ADMIN")
                        ? String.format("0900%06d", index)
                        : String.format("0700%06d", index)
        );
        user.setAddress(index + " Test Street, Springfield");
        user.setBirthday(LocalDate.of(1990, 1, 1).plusDays(index * 37L));
        user.setRole(role);
        user.setPassword(encodedPassword);
        return user;
    }

    private Order buildOrder(User user, int sequence) {
        Order order = new Order();
        order.setNumber(String.format("%s%04d", NUMBER_PREFIX, sequence));
        order.setUser(user);
        order.setStatus(STATUSES[sequence % STATUSES.length]);
        order.setPaymentStatus(
                PAYMENT_STATUSES[sequence % PAYMENT_STATUSES.length]
        );
        order.setShippingStatus(
                SHIPPING_STATUSES[sequence % SHIPPING_STATUSES.length]
        );
        order.setPaymentMethod(
                PAYMENT_METHODS[sequence % PAYMENT_METHODS.length]
        );

        BigDecimal subtotal = BigDecimal.valueOf(1000L + (sequence * 1750L), 2);
        BigDecimal discountAmount = BigDecimal.valueOf((sequence % 5) * 100L, 2);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subtotal.subtract(discountAmount));
        order.setShippingAddress(user.getAddress());
        order.setBillingAddress(user.getAddress());

        return order;
    }

    private Product buildProduct(
            int index,
            String name,
            String description,
            BigDecimal price,
            Integer stock
    ) {
        return new Product(name, description, price, stock);
    }

    private OrderItem buildOrderItem(
            Order order,
            Product product,
            Integer quantity,
            BigDecimal unitPrice
    ) {
        return new OrderItem(order, product, quantity, unitPrice);
    }
}
