package com.ecommerce.jewelleryMart.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.jewelleryMart.model.Cart;
import com.ecommerce.jewelleryMart.model.CartItem;
import com.ecommerce.jewelleryMart.repository.CartRepository;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*") 
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    /**
     * EXTRACTED HELPER METHOD
     * Centralizes the logic for retrieving or creating a cart.
     * Prevents duplication in getCart and addToCart.
     */
    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElse(new Cart(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable String userId) {
        // REFACTOR: Use helper method
        Cart cart = getOrCreateCart(userId);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(defaultValue = "1") double grams,
            @RequestParam double finalPrice) {

        if (quantity <= 0 || grams <= 0 || finalPrice <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // REFACTOR: Use helper method
        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(item.getQuantity() + quantity);
            // In a real app, you might check if grams/price changed before overwriting
            item.setGrams(grams);
            item.setFinalPrice(finalPrice);
        } else {
            cart.getItems().add(new CartItem(productId, quantity, grams, finalPrice));
        }

        cartRepository.save(cart);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/update")
    public ResponseEntity<Cart> updateCart(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam int quantity) {

        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);
        if (optionalCart.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (quantity < 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Cart cart = optionalCart.get();

        if (quantity == 0) {
            cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        } else {
            cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
        }

        cartRepository.save(cart);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Cart> removeItem(
            @RequestParam String userId,
            @RequestParam String productId) {

        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);
        if (optionalCart.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Cart cart = optionalCart.get();
        
        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        
        if (!removed) {
             return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        cartRepository.save(cart);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    // ADMIN CRUD

    @GetMapping
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<String> clearCart(@PathVariable String userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isPresent()) {
            // Note: If you want to use the "clear items" strategy we discussed earlier,
            // you would call cart.clearItems() here instead of delete().
            // For now, I'm keeping your provided delete logic.
            cartRepository.delete(cartOpt.get());
            return ResponseEntity.ok("Cart cleared for user: " + userId);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}