package com.ecommerce.jewelleryMart.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.jewelleryMart.model.Cart;
import com.ecommerce.jewelleryMart.model.CartItem;
import com.ecommerce.jewelleryMart.model.Order;
import com.ecommerce.jewelleryMart.model.Product;
import com.ecommerce.jewelleryMart.repository.CartRepository;
import com.ecommerce.jewelleryMart.repository.OrderRepository;
import com.ecommerce.jewelleryMart.repository.ProductRepository;

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "*")
public class CheckoutController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCartSummary(@PathVariable String userId) {
        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);
        if (optionalCart.isEmpty() || optionalCart.get().getItems().isEmpty()) {
            return new ResponseEntity<>("Cart not found or is empty.", HttpStatus.NOT_FOUND);
        }

        Cart cart = optionalCart.get();

        // REFACTOR: Call method on the object instead of using a helper
        double totalAmount = cart.calculateTotal();
        
        List<Map<String, Object>> items = getCartItems(cart);

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("items", items);
        summary.put("totalAmount", totalAmount);
        
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestBody Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Map<String, Object> delivery = (Map<String, Object>) payload.get("delivery");
        double discount = payload.get("discount") != null ? ((Number) payload.get("discount")).doubleValue() : 0.0;

        String validationError = validatePaymentRequest(userId, delivery);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("message", validationError));
        }

        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);
        if (optionalCart.isEmpty() || optionalCart.get().getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cart is empty."));
        }
        Cart cart = optionalCart.get();

        // REFACTOR: Call method on the object
        double totalAmount = cart.calculateTotal();
        double finalTotal = Math.max(totalAmount - discount, 0);

        Order savedOrder = createAndSaveOrder(userId, cart, finalTotal, delivery);
        List<Map<String, Object>> invoiceItems = getCartItems(cart);
        clearCart(cart);

        Map<String, Object> response = createInvoiceResponse(savedOrder, discount, invoiceItems, delivery);
        return ResponseEntity.ok(response);
    }

    // --- HELPER METHODS ---

    private String validatePaymentRequest(String userId, Map<String, Object> delivery) {
        if (userId == null || delivery == null) {
            return "Missing user or delivery details.";
        }
        if (delivery.get("name") == null || delivery.get("contact") == null || 
            delivery.get("address") == null || delivery.get("city") == null) {
            return "Incomplete delivery details.";
        }
        return null;
    }

    // REMOVED: private double calculateTotal(Cart cart) { ... } 
    // The logic is now inside Cart.java

    private List<Map<String, Object>> getCartItems(Cart cart) {
        List<CartItem> items = cart.getItems();
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> productIds = items.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Map<String, Object>> itemsSummary = new ArrayList<>();

        for (CartItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) continue;

            double itemTotal = item.getFinalPrice() * item.getQuantity();

            Map<String, Object> summaryItem = new HashMap<>();
            summaryItem.put("productId", item.getProductId());
            summaryItem.put("productName", product.getName());
            summaryItem.put("quantity", item.getQuantity());
            summaryItem.put("grams", item.getGrams());
            summaryItem.put("price", product.getPrice());
            summaryItem.put("finalPrice", item.getFinalPrice());
            summaryItem.put("itemTotal", itemTotal);
            itemsSummary.add(summaryItem);
        }
        return itemsSummary;
    }

    private Order createAndSaveOrder(String userId, Cart cart, double totalAmount, Map<String, Object> delivery) {
        List<String> orderProductIds = cart.getItems().stream().map(CartItem::getProductId).collect(Collectors.toList());
        List<Integer> orderQuantities = cart.getItems().stream().map(CartItem::getQuantity).collect(Collectors.toList());
        List<Double> orderGrams = cart.getItems().stream().map(CartItem::getGrams).collect(Collectors.toList());
        
        Order order = new Order(userId, orderProductIds, orderQuantities, totalAmount);
        order.setGrams(orderGrams);
        
        order.setDeliveryName((String) delivery.get("name"));
        order.setDeliveryContact((String) delivery.get("contact"));
        order.setDeliveryAddress((String) delivery.get("address"));
        order.setDeliveryCity((String) delivery.get("city"));

        return orderRepository.save(order);
    }

    private void clearCart(Cart cart) {
        cart.clearItems(); 
        cartRepository.save(cart);
    }

    private Map<String, Object> createInvoiceResponse(Order order, double discount, List<Map<String, Object>> items, Map<String, Object> deliveryDetails) {
        Map<String, Object> invoiceResponse = new HashMap<>();
        invoiceResponse.put("message", "Payment successful, order placed!");
        invoiceResponse.put("orderId", order.getId());
        invoiceResponse.put("userId", order.getUserId());
        invoiceResponse.put("totalAmount", order.getTotalAmount());
        invoiceResponse.put("discount", discount);
        invoiceResponse.put("orderDate", order.getCreatedAt());
        invoiceResponse.put("items", items);
        invoiceResponse.put("delivery", deliveryDetails);
        return invoiceResponse;
    }
}