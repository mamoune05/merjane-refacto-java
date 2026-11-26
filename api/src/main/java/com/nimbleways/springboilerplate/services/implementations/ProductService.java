package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class ProductService {
    private static final String NORMAL = "NORMAL";
    private static final String SEASONAL = "SEASONAL";
    private static final String EXPIRABLE = "EXPIRABLE";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationService notificationService;

    public void process(Product product) {
        switch (product.getType()) {
            case NORMAL:
                processNormalProduct(product);
                break;
            case SEASONAL:
                handleSeasonalProduct(product);
                break;
            case EXPIRABLE:
                handleExpiredProduct(product);
                break;
            default:
                break;
        }
    }

    public void notifyDelay(int leadTime, Product product) {
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    public void handleSeasonalProduct(Product product) {
        processSeasonalProduct(product, LocalDate.now());
    }

    public void handleExpiredProduct(Product product) {
        processExpirableProduct(product, LocalDate.now());
    }

    private void processNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            decrementAvailable(product);
        } else if (product.getLeadTime() > 0) {
            notifyDelay(product.getLeadTime(), product);
        }
    }

    private void processSeasonalProduct(Product product, LocalDate today) {
        if (isInSeason(product, today) && product.getAvailable() > 0) {
            decrementAvailable(product);
            return;
        }

        if (today.isBefore(product.getSeasonStartDate())) {
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);
            return;
        }

        if (today.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            productRepository.save(product);
            return;
        }

        notifyDelay(product.getLeadTime(), product);
    }

    private void processExpirableProduct(Product product, LocalDate today) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(today)) {
            decrementAvailable(product);
            return;
        }

        notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
        product.setAvailable(0);
        productRepository.save(product);
    }

    private boolean isInSeason(Product product, LocalDate today) {
        return !today.isBefore(product.getSeasonStartDate()) && !today.isAfter(product.getSeasonEndDate());
    }

    private void decrementAvailable(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
