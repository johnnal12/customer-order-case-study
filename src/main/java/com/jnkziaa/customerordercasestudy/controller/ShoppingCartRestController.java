package com.jnkziaa.customerordercasestudy.controller;


import com.jnkziaa.customerordercasestudy.dto.*;
import com.jnkziaa.customerordercasestudy.entity.CartItemsInfo;
import com.jnkziaa.customerordercasestudy.entity.CustomerInfo;
import com.jnkziaa.customerordercasestudy.entity.OrderInfo;
import com.jnkziaa.customerordercasestudy.entity.ProductInfo;
import com.jnkziaa.customerordercasestudy.repository.ProductInfoRepository;
import com.jnkziaa.customerordercasestudy.service.CustomerPurchaseService;
import com.jnkziaa.customerordercasestudy.service.CustomerService;
import com.jnkziaa.customerordercasestudy.service.ProductService;
import com.jnkziaa.customerordercasestudy.service.ShowCartItemsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ShoppingCartRestController {

    @Autowired
    private CustomerPurchaseService customerPurchaseService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private ShowCartItemsService showCartItemsService;
    @Autowired
    private ProductInfoRepository productInfoRepository;

    public ShoppingCartRestController(CustomerPurchaseService customerPurchaseService,
                                      ProductService productService,
                                      CustomerService customerService,
                                      ShowCartItemsService showCartItemsService) {
        this.customerPurchaseService = customerPurchaseService;
        this.productService = productService;
        this.customerService = customerService;
        this.showCartItemsService = showCartItemsService;
    }

    @PostMapping("/addProduct")
    public void addProduct(@RequestBody ProductAdditionRequest request){
        productService.saveProductInfo(request);
    }

    @GetMapping("/getAllProducts")
    public ResponseEntity<List<ProductInfo>> getAllProducts(){
        List<ProductInfo> productInfoList = productService.getAllProducts();

        return ResponseEntity.ok(productInfoList);
    }

    @PostMapping("/addToCart")
    public void showProductsInCart(@RequestBody AddToCartRequest request){
        showCartItemsService.saveCartItemsInfo(request);
    }

    @GetMapping("/getAllItemsInCart")
    public ResponseEntity<List<CartItemsInfo>> getAllItemsInCart(){

        List<CartItemsInfo> cartItemsInfoList = showCartItemsService.cartItemsInfoList();

        double totalCartAmount = 0.0;
        double singleCartAmount = 0.0;
        int availableQuantity = 0;

        for(CartItemsInfo items : cartItemsInfoList) {
            String productName = items.getProductName();


            Optional<ProductInfo> productInfoIDs = Optional.ofNullable(productInfoRepository.findByProductName(productName));

            if(productInfoIDs.isPresent()){

                ProductInfo productInfo2 = productInfoIDs.get();
                //System.out.println("PRODUCT1 ARE :" + productInfo1);

                System.out.println("AVAILABLE QUANTITY: " + productInfo2.getProductAvailableQuantity() + " CART QUANTITY : " + items.getProductQuantityAmount());
                if(productInfo2.getProductAvailableQuantity() < items.getProductQuantityAmount()){

                    singleCartAmount = productInfo2.getProductPrice() * productInfo2.getProductAvailableQuantity();

                    items.setProductQuantityAmount(productInfo2.getProductAvailableQuantity());
                }else{

                    singleCartAmount = items.getProductQuantityAmount() * productInfo2.getProductPrice();

                    availableQuantity = productInfo2.getProductAvailableQuantity() - items.getProductQuantityAmount();
                }

                System.out.println("CURRENT TOTAL ARE " + singleCartAmount + " " + totalCartAmount);
                totalCartAmount = totalCartAmount + singleCartAmount;
                productInfo2.setProductAvailableQuantity(availableQuantity);
                availableQuantity = 0;
                items.setProductID(productInfo2.getPID());
                items.setProductName(productInfo2.getProductName());
                items.setTotalCost(singleCartAmount);
            }
        }

        return ResponseEntity.ok(cartItemsInfoList);
    }

    @DeleteMapping("/clearItemsInCart")
    public void clearItemsInCart(){
        List<CartItemsInfo> cartItemsInfoList = showCartItemsService.cartItemsInfoList();
        cartItemsInfoList.clear();
    }


    @PostMapping("/placeOrder")
    public ResponseEntity<ResponseOrderDTO> placeOrder(@RequestBody OrderDTO orderDTO){


        ResponseOrderDTO responseOrderDTO = new ResponseOrderDTO();
        orderDTO.setCartItems(showCartItemsService.cartItemsInfoList());
        double amount = customerPurchaseService.getShoppingCartTotal(orderDTO.getCartItems());

        CustomerInfo customerInfo = new CustomerInfo(orderDTO.getCustomerUsername(), orderDTO.getCustomerEmail());
        System.out.println("THIS CUSTOMER HAS THIS MUCH MONEY " + customerInfo.getCurrentBalance());
        customerInfo.setCurrentBalance(customerInfo.getCurrentBalance() - amount);
        Long customerIdInDataBase = customerService.isCustomerPresent(customerInfo);

        if(customerIdInDataBase != null){
            customerInfo.setCID(customerIdInDataBase);
        }
        else{
            customerInfo = customerService.saveCustomer(customerInfo);
        }

        System.out.println("TOTAL AMOUNT IS :" + amount);
        OrderInfo orderInfo = new OrderInfo(orderDTO.getOrderDescription(), customerInfo, amount, orderDTO.getCartItems());
        orderInfo = customerPurchaseService.saveOrder(orderInfo);

        responseOrderDTO.setAmount(amount);
        responseOrderDTO.setDate(String.valueOf(new Date()));
        responseOrderDTO.setInvoiceNumber(String.valueOf(UUID.randomUUID()));
        responseOrderDTO.setOID(orderInfo.getOID());
        responseOrderDTO.setOrderDescription(orderDTO.getOrderDescription());

        showCartItemsService.cartItemsInfoList().clear();
        return ResponseEntity.ok(responseOrderDTO);

    }

    @GetMapping("/getOrder/{orderId}")
    public ResponseEntity<OrderInfo> getOrderInfoDetails(@PathVariable Long orderId){
        OrderInfo orderInfo = customerPurchaseService.getOrderDetails(orderId);
        return ResponseEntity.ok(orderInfo);
    }
    /*
    @PostMapping("/customerPurchase")
    public CustomerPurchaseAcknowledgement customerPurchase(@RequestBody CustomerPurchaseRequest request) {
        return customerPurchaseService.customerPurchase(request);
    }

    @PostMapping("/addProduct")
    public void addProduct(@RequestBody ProductAdditionRequest request){
        productService.saveProductInfo(request);
    }

    @GetMapping("/productsAvailable")
    public void showProducts(){

    }*/

}
