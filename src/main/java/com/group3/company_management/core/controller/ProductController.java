package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.ProductRequest;
import com.group3.company_management.core.dto.ProductResponse;
import com.group3.company_management.core.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private static final List<String> PRODUCT_CATEGORIES = List.of(
            "Camera Body",
            "Lens",
            "Video Camera",
            "Action Camera",
            "Combo Kit"
    );

    private final ProductService productService;

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<ProductResponse> productPage = productService.getProductsPage(page, 10);

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", PRODUCT_CATEGORIES);
        model.addAttribute("countProduct", productPage.getTotalElements());
        return "products/list";
    }

    @GetMapping("/find")
    public String findProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<ProductResponse> productPage = productService.searchPage(keyword, category, status, page, 10);

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("status", status);
        model.addAttribute("categories", PRODUCT_CATEGORIES);
        model.addAttribute("isSearch", true);
        model.addAttribute("countProduct", productPage.getTotalElements());
        return "products/list";
    }

    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) Long id, Model model) {
        ProductRequest productForm = id == null ? new ProductRequest() : toRequest(productService.getProductById(id));
        if (id == null) {
            productForm.setActive(true);
        }
        model.addAttribute("productForm", productForm);
        model.addAttribute("isEdit", id != null);
        model.addAttribute("categories", PRODUCT_CATEGORIES);
        return "products/add-form";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("productForm") ProductRequest request, Model model) {
        try {
            productService.createProduct(request);
            return "redirect:/products";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("categories", PRODUCT_CATEGORIES);
            return "products/add-form";
        }
    }

    @PostMapping("/update")
    public String updateProduct(@ModelAttribute("productForm") ProductRequest request, Model model) {
        try {
            productService.updateProduct(request);
            return "redirect:/products";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("categories", PRODUCT_CATEGORIES);
            return "products/add-form";
        }
    }

    @PostMapping("/update-status")
    public String updateStatus(@ModelAttribute ProductRequest request) {
        productService.updateProductStatus(request);
        return "redirect:/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/products";
    }

    private ProductRequest toRequest(ProductResponse response) {
        ProductRequest request = new ProductRequest();
        request.setId(response.getId());
        request.setProductCode(response.getProductCode());
        request.setName(response.getName());
        request.setCategory(response.getCategory());
        request.setDescription(response.getDescription());
        request.setImageUrl(response.getImageUrl());
        request.setUnitPrice(response.getUnitPrice());
        request.setActive(response.getActive());
        return request;
    }
}
