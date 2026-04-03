package com.fashionshop.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stock;
    private String image;
    private Boolean active;
    private Long categoryId;
    private String categoryName;
    private List<String> images;
}
