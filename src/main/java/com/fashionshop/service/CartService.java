package com.fashionshop.service;

import com.fashionshop.entity.CartItem;
import com.fashionshop.entity.Product;
import java.math.BigDecimal;
import java.util.List;

public interface CartService {

    List<CartItem> getCartItems();

    BigDecimal getCartTotal();

    int getCartCount();

    void addToCart(Product product, Integer quantity);

    void updateCartItem(Long productId, Integer quantity);

    void removeFromCart(Long productId);

    void clearCart();
}
