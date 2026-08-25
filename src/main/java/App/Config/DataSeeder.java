package App.Config;

import App.Models.Order;
import App.Models.User;
import App.Models.Product;
import App.Models.OrderItem;
import App.Repositories.OrderRepository;
import App.Repositories.UserRepository;
import App.Repositories.ProductRepository;
import App.Repositories.OrderItemRepository;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Development seed data. Guarded by the "seed" profile so it never runs
 * during a normal boot:
 *
 *     ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
 *
 * Everything it writes is tagged — users get an "@seed.local" email and
 * orders get a "SEED-" number prefix — so the data is easy to identify and
 * remove without touching real rows.
 */
@Component
@Profile("seed")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataSeeder.class);

    private static final String EMAIL_DOMAIN = "@seed.local";
    private static final String NUMBER_PREFIX = "SEED-";

    /** Shared plaintext password for every seeded account. */
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

    /**
     * Orders per customer. The leading 12 gives multi-page responses at the
     * default page size, and the trailing zeros leave customers that can
     * actually be removed through the admin delete-user endpoint.
     */
    private static final int[] ORDERS_PER_CUSTOMER = {
            12, 6, 4, 2, 1, 0, 0, 0
    };

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
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

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {

        if (userRepository.existsByEmail("admin1" + EMAIL_DOMAIN)) {

            log.info(
                    "Seed data already present, skipping. "
                            + "Remove it first to re-seed."
            );
            return;
        }

        // Encoded once and shared: every seeded account uses the same
        // password, and bcrypt is deliberately slow.
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

        // Seed products
        List<Product> products = new ArrayList<>();
        products.add(buildProduct(1, "Laptop", "High-performance laptop for professionals", new BigDecimal("1299.99"), 15));
        products.add(buildProduct(2, "Wireless Mouse", "Ergonomic wireless mouse with precision tracking", new BigDecimal("29.99"), 50));
        products.add(buildProduct(3, "USB-C Cable", "Durable USB-C charging and data cable", new BigDecimal("12.99"), 100));
        products.add(buildProduct(4, "Mechanical Keyboard", "RGB mechanical gaming keyboard", new BigDecimal("149.99"), 25));
        products.add(buildProduct(5, "Monitor 4K", "27-inch 4K ultra HD monitor", new BigDecimal("599.99"), 10));
        products.add(buildProduct(6, "Headphones", "Noise-cancelling wireless headphones", new BigDecimal("199.99"), 30));

        productRepository.saveAll(products);

        List<Order> orders = new ArrayList<>();
        int sequence = 0;

        for (int c = 0; c < customers.size(); c++) {

            for (int n = 0; n < ORDERS_PER_CUSTOMER[c]; n++) {
                orders.add(buildOrder(customers.get(c), ++sequence));
            }
        }

        orderRepository.saveAll(orders);

        // Seed order items
        List<OrderItem> orderItems = new ArrayList<>();
        int itemSequence = 0;

        for (Order order : orders) {
            // Each order gets 1-3 random items
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

        log.info(
                "Seeded {} admins, {} customers, {} orders, {} products, and {} order items.",
                admins.size(),
                customers.size(),
                orders.size(),
                products.size(),
                orderItems.size()
        );

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

        // Kept distinct across both groups: phone_number is unique.
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

        // Cycled by sequence so every status value appears in the data set.
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

        BigDecimal subtotal =
                BigDecimal.valueOf(1000L + (sequence * 1750L), 2);

        BigDecimal discountAmount =
                BigDecimal.valueOf((sequence % 5) * 100L, 2);

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
