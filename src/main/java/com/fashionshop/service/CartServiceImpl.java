package com.fashionshop.service;

import com.fashionshop.entity.CartItem;
import com.fashionshop.entity.Product;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartServiceImpl implements CartService {

    private final HttpSession session;

    public CartServiceImpl(HttpSession session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, CartItem> getCartMap() {
        ConcurrentHashMap<Long, CartItem> cartMap =
                (ConcurrentHashMap<Long, CartItem>) session.getAttribute("cart");

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
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }

        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }

        int stock = product.getStock() != null ? product.getStock() : 0;
        if (stock <= 0) {
            throw new IllegalArgumentException("Sản phẩm đã hết hàng");
        }

        ConcurrentHashMap<Long, CartItem> cartMap = getCartMap();
        CartItem item = cartMap.get(product.getId());

        if (item != null) {
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > stock) {
                throw new IllegalArgumentException("Số lượng vượt quá tồn kho. Hiện chỉ còn " + stock + " sản phẩm.");
            }
            item.setQuantity(newQuantity);
        } else {
            if (quantity > stock) {
                throw new IllegalArgumentException("Số lượng vượt quá tồn kho. Hiện chỉ còn " + stock + " sản phẩm.");
            }

            BigDecimal price = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
            item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setPrice(price);
            cartMap.put(product.getId(), item);
        }

        session.setAttribute("cart", cartMap);
    }

    @Override
    public void updateCartItem(Long productId, Integer quantity) {
        ConcurrentHashMap<Long, CartItem> cartMap = getCartMap();

        if (quantity == null || quantity <= 0) {
            cartMap.remove(productId);
            session.setAttribute("cart", cartMap);
            return;
        }

        CartItem item = cartMap.get(productId);
        if (item != null) {
            Product product = item.getProduct();
            int stock = product.getStock() != null ? product.getStock() : 0;

            if (stock <= 0) {
                cartMap.remove(productId);
                session.setAttribute("cart", cartMap);
                throw new IllegalArgumentException("Sản phẩm đã hết hàng");
            }

            if (quantity > stock) {
                throw new IllegalArgumentException("Số lượng vượt quá tồn kho. Hiện chỉ còn " + stock + " sản phẩm.");
            }

            item.setQuantity(quantity);
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