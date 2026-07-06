package com.quickeats.restaurantservice.config;

import com.quickeats.restaurantservice.document.MenuItem;
import com.quickeats.restaurantservice.document.Restaurant;
import com.quickeats.restaurantservice.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final RestaurantRepository restaurantRepository;

    public DatabaseSeeder(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @SuppressWarnings("null")
    @Override
    public void run(String... args) throws Exception {
        if (restaurantRepository.count() == 0) {
            Restaurant pizzaPalace = new Restaurant(
                    "507f1f77bcf86cd799439011",
                    "Pizza Palace",
                    "Pizza",
                    "123 Pizza St",
                    new double[] { -73.935242, 40.730610 },
                    Arrays.asList(
                            new MenuItem("item1", "Margherita Pizza", "Classic cheese and tomato pizza",
                                    new BigDecimal("12.99"), true),
                            new MenuItem("item2", "Pepperoni Pizza", "Pizza with spicy pepperoni",
                                    new BigDecimal("15.99"), true),
                            new MenuItem("item3", "Garlic Bread", "Warm garlic bread with butter",
                                    new BigDecimal("5.99"), true)));

            Restaurant burgerBonanza = new Restaurant(
                    "507f1f77bcf86cd799439012",
                    "Burger Bonanza",
                    "Burgers",
                    "456 Burger Ave",
                    new double[] { -73.985130, 40.758896 },
                    Arrays.asList(
                            new MenuItem("item4", "Classic Cheeseburger", "Juicy beef patty with cheese",
                                    new BigDecimal("9.99"), true),
                            new MenuItem("item5", "Bacon Double Cheeseburger", "Double patty with crispy bacon",
                                    new BigDecimal("13.99"), true),
                            new MenuItem("item6", "French Fries", "Golden crispy fries", new BigDecimal("3.49"),
                                    true)));

            Restaurant sushiSupreme = new Restaurant(
                    "507f1f77bcf86cd799439013",
                    "Sushi Supreme",
                    "Japanese",
                    "789 Fish Way",
                    new double[] { -74.0060, 40.7128 },
                    Arrays.asList(
                            new MenuItem("item7", "California Roll", "Crab, avocado and cucumber roll",
                                    new BigDecimal("8.99"), true),
                            new MenuItem("item8", "Salmon Nigiri", "Fresh salmon over pressed rice",
                                    new BigDecimal("11.99"), true),
                            new MenuItem("item9", "Miso Soup", "Traditional seaweed and tofu soup",
                                    new BigDecimal("2.99"), true)));

            restaurantRepository.saveAll(Arrays.asList(pizzaPalace, burgerBonanza, sushiSupreme));
            logger.info("Restaurant Service: Seeded database with 3 restaurants");
        }
    }
}
