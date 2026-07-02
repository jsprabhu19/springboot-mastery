package com.quickeats.restaurantservice.repository;

import com.quickeats.restaurantservice.document.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB Repository interface for operations on Restaurant documents.
 */
@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
    List<Restaurant> findByCuisineTypeIgnoreCase(String cuisineType);
}
