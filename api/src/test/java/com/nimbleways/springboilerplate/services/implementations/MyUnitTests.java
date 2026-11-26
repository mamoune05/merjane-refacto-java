package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks 
    private ProductService productService;

    @Test
    public void test() {
        // GIVEN
        Product product =new Product(null, 15, 0, "NORMAL", "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.process(product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendDelayNotification(product.getLeadTime(), product.getName());
    }

    @Test
    public void normalInStock() {
        Product product = new Product(null, 15, 2, "NORMAL", "RJ45 Cable", null, null, null);

        productService.process(product);

        assertEquals(1, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    public void seasonalInStock() {
        Product product = new Product(null, 5, 2, "SEASONAL", "Melon", null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));

        productService.process(product);

        assertEquals(1, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    public void seasonalDelay() {
        Product product = new Product(null, 5, 0, "SEASONAL", "Melon", null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));

        productService.process(product);

        Mockito.verify(notificationService, Mockito.times(1))
                .sendDelayNotification(product.getLeadTime(), product.getName());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
    }

    @Test
    public void seasonalUnavailable() {
        Product product = new Product(null, 5, 0, "SEASONAL", "Melon", null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(4));

        productService.process(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService, Mockito.times(1)).sendOutOfStockNotification(product.getName());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
    }

    @Test
    public void expirableFresh() {
        Product product = new Product(null, 15, 2, "EXPIRABLE", "Yogurt",
                LocalDate.now().plusDays(1), null, null);

        productService.process(product);

        assertEquals(1, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    public void expirableExpired() {
        Product product = new Product(null, 15, 2, "EXPIRABLE", "Milk",
                LocalDate.now().minusDays(1), null, null);

        productService.process(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService, Mockito.times(1))
                .sendExpirationNotification(product.getName(), product.getExpiryDate());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
    }
}
