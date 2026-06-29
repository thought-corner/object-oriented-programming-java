package facade_pattern;

import facade_pattern.controller.dto.OrderRequest;
import facade_pattern.facade.OrderFacade;
import facade_pattern.service.InventoryService;
import facade_pattern.service.NotificationService;
import facade_pattern.service.PaymentService;
import facade_pattern.service.ShippingService;
import facade_pattern.service.dto.OrderResult;

public class FacadeMain {

    public static void main(String[] args) {

        // 입력값
        OrderRequest request = new OrderRequest("Test-Customer", "Test-Sku", 1, 10000, "Test-Address");

        // 객체 생성
        InventoryService inventoryService = new InventoryService();
        NotificationService notificationService = new NotificationService();
        PaymentService  paymentService = new PaymentService();
        ShippingService shippingService = new ShippingService();

        // 퍼싸드 패턴
        OrderFacade orderFacade = new OrderFacade(inventoryService, notificationService, paymentService, shippingService);

        OrderResult result = orderFacade.placeOrder(request);
        System.out.println("결과 : " + result);
    }

}
