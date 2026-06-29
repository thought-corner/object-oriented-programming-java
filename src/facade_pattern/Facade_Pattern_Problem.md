# 문제

- 온라인 쇼핑몰의 주문 처리 로직을 구현하라.
- 고객이 주문하면 재고 → 결제 → 배송 → 알림 순서로 4개의 서브시스템이 동작한다.
- 현재는 컨트롤러가 모든 서브시스템을 직접 호출하며 호출 순서·예외·롤백을 떠안고 있어 유지보수가 어렵다.
- 이 모든 서브시스템을 단일 진입점 `OrderFacade.placeOrder(OrderRequest)`로 감싸 클라이언트가 메서드 하나만 호출하면 주문이 완료되도록 구현하라.
- 실패 시에는 이미 수행된 작업을 역순으로 보상해야 한다.

# 주어진 조건(수정 불가)

- 다음 서브시스템들은 이미 구현되어 있다고 가정한다. 내부를 변경하지 말고 호출만 한다.

| 서브시스템                 | 메서드                                                                | 설명               |
|-----------------------|--------------------------------------------------------------------|------------------|
| `InventoryService`    | `boolean isAvailable(String sku, int qty)`                         | 재고 확인            |
|                       | `String reserve(String sku, int qty)`                              | 재고 예약 → 예약ID 반환  |
|                       | `void release(String reservationId)`                               | 예약 취소(보상)        |
| `PaymentService`      | `PaymentResult charge(String customerId, long amount)`             | 결제 승인 → 결제ID 포함  |
|                       | `void refund(String paymentId)`                                    | 환불(보상)           |
| `ShippingService`     | `String schedule(String address, String sku, int qty)`             | 배송 예약 → 운송장번호 반환 |
| `NotificationService` | `void sendOrderConfirmation(String customerId, String trackingNo)` | 주문 확인 알림         |

- 각 메서드는 실패 시 `RuntimeException`을 던진다고 가정한다.

# 입력

- `OrderRequest` 객체 하나가 주어진다.

| 필드         | 타입     | 설명    |
|------------|--------|-------|
| customerId | String | 고객 ID |
| sku        | String | 상품 코드 |
| qty        | int    | 수량    |
| amount     | long   | 결제 금액 |
| address    | String | 배송지   |

# 출력

- 성공 시 `OrderResult` 를 반환한다.(주문번호, 운송장번호, 결제ID 포함)
- 실패 시 `OrderException` 을 던진다.(실패 단계와 원인 메시지 포함)

# 처리 규칙

1) isAvailable 확인 → false 면 즉시 OrderException
2) reserve(재고 예약)
3) charge(결제 승인) → 실패 시: release
4) schedule(배송 예약) → 실패 시: refund, release
5) sendOrderConfirmation → 실패해도 주문은 성공 (best-effort)
6) OrderResult 반환

# 보상(롤백) 규칙

- 결제 실패 시 : 예약한 재고를 `release` 한다.
- 배송 실패 시 : `refund`후 `release`(역순 보상)
- 알림 실패 시 : 무시. 주문은 성공 처리한다.

# 제약사항

1. 클라이언트는 **`OrderFacade` 에만** 의존한다. 서브시스템을 직접 import 금지.
2. 서브시스템 4개는 **생성자 주입(DI)** 으로 받는다. (테스트에서 mock 교체 가능해야 함)
3. 퍼사드는 **조율(orchestration)** 만 담당한다. 도메인 로직(할인 계산 등) 포함 금지.
4. 보상은 정확한 역순으로 수행한다.

# 시그니처 (Java)

```java
public class OrderFacade {
    public OrderFacade(InventoryService inventory,
                       PaymentService payment,
                       ShippingService shipping,
                       NotificationService notification) { ... }

    public OrderResult placeOrder(OrderRequest request) { ... }
}
```