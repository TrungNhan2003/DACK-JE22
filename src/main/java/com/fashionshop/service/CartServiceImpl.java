package com.fashionshop.service;

import com.fashionshop.entity.CartItem;
import com.fashionshop.entity.Product;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final HttpSession session;

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, CartItem> getCartMap() {
        ConcurrentHashMap<Long, CartItem> cartMap = (ConcurrentHashMap<Long, CartItem>) session.getAttribute("cart");
        if (cartMap == null) {
            cartMap = new ConcurrentHashMap<>();
            session.setAttribute("cart", cartMap);
        }
        return cartMap;
    }

    @Override
    public List<CartItem> getCartItems() {
        return new ArrayList<>(getCartMap().values());
    }

    @Override
    public BigDecimal getCartTotal() {
        return getCartMap().values().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int getCartCount() {
        return getCartMap().values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Override
    public void addToCart(Product product, Integer quantity) {
        ConcurrentHashMap<Long, CartItem> cartMap = getCartMap();
        CartItem item = cartMap.get(product.getId());

        if (item != null) {
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            BigDecimal price = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
            item = CartItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .price(price)
                    .build();
            cartMap.put(product.getId(), item);
        }
        session.setAttribute("cart", cartMap);
    }

    @Override
    public void updateCartItem(Long productId, Integer quantity) {
        ConcurrentHashMap<Long, CartItem> cartMap = getCartMap();
        if (quantity <= 0) {
            cartMap.remove(productId);
        } else {
            CartItem item = cartMap.get(productId);
            if (item != null) {
                item.setQuantity(quantity);
            }
        }
        session.setAttribute("cart", cartMap);
    }

    @Override
    public void removeFromCart(Long productId) {
        ConcurrentHashMap<Long, CartItem> cartMap = getCartMap();
        cartMap.remove(productId);
        session.setAttribute("cart", cartMap);
    }

    @Override
    public void clearCart() {
        getCartMap().clear();
        session.setAttribute("cart", new ConcurrentHashMap<>());
    }
}
