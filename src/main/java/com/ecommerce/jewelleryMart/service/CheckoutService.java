package com.ecommerce.jewelleryMart.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.jewelleryMart.dto.DeliveryDetails;
import com.ecommerce.jewelleryMart.dto.PaymentRequest;
import com.ecommerce.jewelleryMart.model.Cart;
import com.ecommerce.jewelleryMart.model.Order;

@Service
public class CheckoutService {

    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;

    public Map<String, Object> getCartSummary(String userId) {
        Cart cart = cartService.getCartOrThrow(userId);

        double totalAmount = cart.calculateTotal();
        List<Map<String, Object>> items = cartService.getCartItemsWithDetails(cart);

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("items", items);
        summary.put("totalAmount", totalAmount);

        return summary;
    }

    public Map<String, Object> processPayment(PaymentRequest request) {
        validatePaymentRequest(request);

        Cart cart = cartService.getCartOrThrow(request.getUserId());
        double totalAmount = cart.calculateTotal();
        
        double discount = request.getDiscount() != null ? request.getDiscount() : 0.0;
        double finalTotal = Math.max(totalAmount - discount, 0);

        // Delegate to OrderService
        Order savedOrder = orderService.createAndSaveOrder(
                request.getUserId(),
                cart,
                finalTotal,
                request.getDelivery()
        );

        // Retrieve items for the invoice before clearing
        List<Map<String, Object>> invoiceItems = cartService.getCartItemsWithDetails(cart);

        // Use CartService to handle persistence of clearing the cart
        cartService.clearCart(cart);

        return createInvoiceResponse(savedOrder, discount, invoiceItems, request.getDelivery());
    }

    // --- Helper Methods ---

    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getUserId() == null || request.getDelivery() == null) {
            throw new IllegalArgumentException("Missing user or delivery details.");
        }
        DeliveryDetails d = request.getDelivery();
        if (d.getName() == null || d.getContact() == null || d.getAddress() == null || d.getCity() == null) {
            throw new IllegalArgumentException("Incomplete delivery details.");
        }
    }

    private Map<String, Object> createInvoiceResponse(Order order, double discount, List<Map<String, Object>> items, DeliveryDetails delivery) {
        Map<String, Object> invoiceResponse = new HashMap<>();
        invoiceResponse.put("message", "Payment successful, order placed!");
        invoiceResponse.put("orderId", order.getId());
        invoiceResponse.put("userId", order.getUserId());
        invoiceResponse.put("totalAmount", order.getTotalAmount());
        invoiceResponse.put("discount", discount);
        invoiceResponse.put("orderDate", order.getCreatedAt());
        invoiceResponse.put("items", items);
        invoiceResponse.put("delivery", delivery);
        return invoiceResponse;
    }
}