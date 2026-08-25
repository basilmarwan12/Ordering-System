package App.Controllers;

import App.Services.SeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import App.Repositories.OrderItemRepository;
import App.Repositories.OrderRepository;
import App.Repositories.ProductRepository;
import App.Repositories.UserRepository;

@RestController
@RequestMapping("/api/seed")
public class SeederController {

    private static final Logger log = LoggerFactory.getLogger(SeederController.class);

    private final SeederService seederService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public SeederController(
            SeederService seederService,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.seederService = seederService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @PostMapping("/run")
    public ResponseEntity<String> runSeeder() {
        try {
            log.info("Manually triggering data seeder...");
            String result = seederService.seedData();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error running seeder", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearSeedData() {
        try {
            log.info("Clearing seed data...");
            
            orderItemRepository.deleteAll();
            log.info("Deleted all order items");
            
            orderRepository.deleteAll();
            log.info("Deleted all orders");
            
            productRepository.deleteAll();
            log.info("Deleted all products");
            
            userRepository.deleteAll();
            log.info("Deleted all users");
            
            String result = "Cleared all seed data successfully";
            log.info(result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error clearing seed data", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
