package com.ecommerce.jewelleryMart.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.jewelleryMart.model.Product;
import com.ecommerce.jewelleryMart.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // ===== GET ALL (Search + Filter + Sort) =====
    public List<Product> getAllProducts(
            String search,
            String sort,
            String category,
            String metalType
    ) {
        List<Product> products =
                (search != null && !search.isEmpty())
                        ? productRepository.findByNameContainingIgnoreCase(search)
                        : productRepository.findAll();

        if (category != null && !category.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null &&
                            p.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        if (metalType != null && !metalType.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getMetalType() != null &&
                            p.getMetalType().equalsIgnoreCase(metalType))
                    .collect(Collectors.toList());
        }

        if (sort != null && !sort.isEmpty()) {
            applySorting(products, sort);
        }

        return products;
    }

    // ===== SORTING LOGIC =====
    private void applySorting(List<Product> products, String sort) {
        switch (sort) {
            case "priceLowToHigh":
                products.sort(Comparator.comparingDouble(Product::getPrice));
                break;
            case "priceHighToLow":
                products.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
            case "nameAsc":
                products.sort(Comparator.comparing(
                        Product::getName,
                        String.CASE_INSENSITIVE_ORDER
                ));
                break;
            case "nameDesc":
                products.sort(Comparator.comparing(
                        Product::getName,
                        String.CASE_INSENSITIVE_ORDER
                ).reversed());
                break;
            default:
                break;
        }
    }

    // ===== GET BY ID =====
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    // ===== CREATE =====
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    // ===== UPDATE =====
    public Optional<Product> updateProduct(String id, Product productDetails) {
        Optional<Product> optionalProduct = productRepository.findById(id);

        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();

            product.setName(productDetails.getName());
            product.setPrice(productDetails.getPrice());
            product.setCategory(productDetails.getCategory());
            product.setMetalType(productDetails.getMetalType());
            product.setImage(productDetails.getImage());
            product.setDescription(productDetails.getDescription());
            product.setWeight(productDetails.getWeight());

            productRepository.save(product);
        }

        return optionalProduct;
    }

    // ===== DELETE =====
    public boolean deleteProduct(String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
