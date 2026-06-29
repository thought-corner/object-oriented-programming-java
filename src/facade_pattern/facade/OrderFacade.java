package facade_pattern.facade;

import facade_pattern.controller.dto.OrderRequest;
import facade_pattern.exception.OrderException;
import facade_pattern.service.InventoryService;
import facade_pattern.service.NotificationService;
import facade_pattern.service.PaymentService;
import facade_pattern.service.ShippingService;
import facade_pattern.service.dto.OrderResult;
import facade_pattern.service.dto.PaymentResult;

/**
 * 주문 처리의 단일 진입점 역할을 하는 퍼사드(Facade).
 *
 * <p>재고·결제·배송·알림 등 서로 독립적인 하위 서비스의 호출 순서와, 실패 시
 * 보상(rollback) 처리를 이 클래스가 전담한다. 클라이언트는 개별 서비스를 알 필요 없이
 * {@link #placeOrder} 하나만 호출하면 주문 처리 전 과정을 위임할 수 있다.
 *
 * <p><b>책임 경계:</b> 이 클래스는 하위 서비스의 <em>조율(orchestration)</em>만 담당한다.
 * 할인 계산·재고 정책 같은 도메인 규칙은 각 하위 서비스가 소유하며 퍼사드에 두지 않는다.
 *
 * <p><b>보상 전략:</b> 외부 시스템 호출이라 단일 DB 트랜잭션으로 묶을 수 없으므로
 * Saga 패턴의 보상 트랜잭션을 사용한다. 어느 단계가 실패하면 그때까지 성공한 작업을
 * <em>역순으로</em> 되돌린 뒤 {@link OrderException}으로 실패 원인을 전파한다.
 *
 * <p><b>스레드 안전성:</b> 가변 상태를 갖지 않으며, 주입된 하위 서비스가 스레드 안전하다는
 * 전제하에 본 클래스도 스레드 안전하다.
 */
public class OrderFacade {

    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;

    /**
     * 협력 객체인 하위 서비스들을 생성자 주입으로 받는다.
     *
     * <p>외부에서 의존성을 주입받으므로 특정 구현에 결합되지 않으며, 테스트 시
     * 목(mock)으로 교체해 보상 로직 등을 독립적으로 검증할 수 있다.
     */
    public OrderFacade(InventoryService inventoryService, NotificationService notificationService, PaymentService paymentService, ShippingService shippingService) {
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.shippingService = shippingService;
    }

    /**
     * 단일 주문을 처리한다.
     *
     * <p>처리 순서는 재고 확인 → 재고 예약 → 결제 → 배송 예약 → 주문 확인 알림이다.
     * 결제 또는 배송이 실패하면 직전까지 성공한 단계를 역순으로 보상한 뒤 예외를 던진다.
     * 알림 발송은 best-effort로 처리하여, 실패하더라도 주문 자체는 성공으로 간주한다.
     *
     * @param request 주문 요청 정보(고객 식별자·상품 코드·수량·결제 금액·배송지)
     * @return 생성된 주문번호·운송장번호·결제ID를 담은 {@link OrderResult}
     * @throws OrderException 재고 부족, 결제 실패, 배송 예약 실패 등으로 주문이 확정되지 못한 경우
     */
    public OrderResult placeOrder(OrderRequest request) {

        // 1) 재고 확인: 재고가 없으면 후속 작업을 시작하지 않고 즉시 실패시킨다.
        if (!inventoryService.isAvailable(request.sku(), request.qty())) {
            throw new OrderException("재고 부족으로 인한 주문 실패!");
        }

        // 2) 재고 예약: 반환된 예약 ID는 이후 단계가 실패할 경우 보상(해제)에 사용된다.
        String reservedId = inventoryService.reserve(request.sku(), request.qty());

        // 3) 결제: 실패 시 예약한 재고를 해제한 뒤 예외를 전파한다.
        PaymentResult charge = processPayment(request.customerId(), request.amount(), reservedId);

        // 4) 배송 예약: 실패 시 결제를 환불하고 재고를 해제(역순 보상)한 뒤 예외를 전파한다.
        String scheduled = processShipping(request.sku(), request.qty(), request.address(), charge, reservedId);

        // 5) 주문 확인 알림: best-effort. 실패해도 주문 확정에는 영향을 주지 않는다.
        sendNotification(request.customerId(), scheduled);

        // 6) 주문번호를 채번하여 처리 결과를 반환한다.
        String orderNo = "O-" + java.util.UUID.randomUUID();
        return new OrderResult(orderNo, scheduled, charge.paymentId());
    }

    /**
     * 주문 확인 알림을 발송한다(best-effort).
     *
     * <p>알림은 주문 확정의 필수 조건이 아니므로 실패하더라도 예외를 전파하지 않고
     * 로그만 남긴 뒤 흐름을 계속한다. 즉 이 단계는 보상 대상이 아니다.
     *
     * @param customerId 알림 수신 고객 식별자
     * @param trackingNo 안내에 포함할 운송장 번호
     */
    private void sendNotification(String customerId, String trackingNo) {
        try {
            notificationService.sendOrderConfirmation(customerId, trackingNo);
        } catch (RuntimeException e) {
            // 알림 실패는 주문 성공 여부와 무관하므로 삼키고 기록만 한다.
            System.out.println("알림 발송 실패!");
        }
    }

    /**
     * 배송을 예약하고 운송장 번호를 반환한다.
     *
     * <p>예약에 실패하면 직전까지 성공한 작업을 역순으로 보상한다.
     * 즉 결제를 환불({@code refund})하고 재고 예약을 해제({@code release})한 뒤
     * {@link OrderException}으로 원인을 전파한다.
     *
     * @param sku        주문 상품 코드
     * @param qty        주문 수량
     * @param address    배송지 주소
     * @param charge     보상 환불에 사용할, 앞선 결제 결과
     * @param reservedId 보상 해제에 사용할 재고 예약 ID
     * @return 발급된 운송장 번호
     * @throws OrderException 배송 예약에 실패한 경우(보상 수행 후 전파)
     */
    private String processShipping(String sku, int qty, String address, PaymentResult charge, String reservedId) {
        try {
            return shippingService.schedule(address, sku, qty);
        } catch (RuntimeException e) {
            paymentService.refund(charge.paymentId());
            inventoryService.release(reservedId);
            throw new OrderException("배송 실패 : " + e.getMessage(), e);
        }
    }

    /**
     * 결제를 승인하고 그 결과를 반환한다.
     *
     * <p>결제에 실패하면 직전에 예약한 재고를 해제({@code release})하여 보상한 뒤
     * {@link OrderException}으로 원인을 전파한다.
     *
     * @param customerId 결제 고객 식별자
     * @param amount     결제 금액
     * @param reservedId 보상 해제에 사용할 재고 예약 ID
     * @return 결제 ID 등을 담은 {@link PaymentResult}
     * @throws OrderException 결제 승인에 실패한 경우(보상 수행 후 전파)
     */
    private PaymentResult processPayment(String customerId, long amount, String reservedId) {
        try {
            return paymentService.charge(customerId, amount);
        } catch (RuntimeException e) {
            inventoryService.release(reservedId);
            throw new OrderException("결제 실패 : " + e.getMessage(), e);
        }
    }
}
