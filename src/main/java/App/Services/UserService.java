package App.Services;

import App.Middlewares.Auth.UserNotFoundException;
import App.Middlewares.Users.SelfDeletionNotAllowedException;
import App.Middlewares.Users.UserHasOrdersException;
import App.Models.User;
import App.Repositories.OrderRepository;
import App.Repositories.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserService(
            UserRepository userRepository,
            OrderRepository orderRepository
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void deleteUser(
            UUID uuid,
            String actingUserId
    ) {

        User user = userRepository
                .findById(uuid)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        if (user.getUuid().toString().equals(actingUserId)) {

            throw new SelfDeletionNotAllowedException(
                    "Admins cannot delete their own account"
            );
        }

        // Orders reference the user with a non-null FK, so the row
        // cannot be removed while any order history exists.
        if (orderRepository.existsByUserUuid(uuid)) {

            throw new UserHasOrdersException(
                    "User cannot be deleted while orders exist"
            );
        }

        userRepository.delete(user);
    }
}
