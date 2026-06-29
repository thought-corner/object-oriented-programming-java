package facade_pattern.service;

import facade_pattern.service.dto.PaymentResult;

/**
 * [결제 서브시스템]  ── 이미 제공된다고 가정. 호출만 한다.
 *
 * 퍼사드는:
 *   1) 재고 예약에 성공한 뒤 charge() 로 결제를 승인한다
 *   2) charge() 가 실패(예외)하면 → 잡아둔 재고를 release 한다
 *   3) 결제는 됐는데 그 다음 배송이 실패하면 → refund() 로 환불한다
 */
public class PaymentService {

    /** 이 더미의 1회 결제 한도 (결제 서비스 고유의 사정). 실제라면 카드사/잔액 조회. */
    private static final long CREDIT_LIMIT = 1_000_000L;

    /**
     * 결제를 승인한다.
     * @return 성공 시 결제 ID가 담긴 PaymentResult
     * @throws RuntimeException 결제 실패 시
     *
     * [실패 조건] 결제 금액이 한도(CREDIT_LIMIT=1,000,000)를 넘으면 예외를 던진다.
     *   → 퍼사드의 "결제 실패 → release(예약ID) → OrderException" 경로를 테스트할 수 있다.
     *     예) amount=2_000_000 으로 호출하면 예외가 터진다.
     *
     *   ※ 한도 초과는 '결제 서비스의 책임'이라 여기서 검사한다.
     *     (수량 음수/주소 누락 같은 '주문 검증'은 결제의 일이 아니므로 넣지 않는다)
     */
    public PaymentResult charge(String customerId, long amount) {
        System.out.println("[Payment] charge: customer=" + customerId + ", amount=" + amount);
        if (amount > CREDIT_LIMIT) {
            throw new RuntimeException("결제 한도 초과: amount=" + amount + " > limit=" + CREDIT_LIMIT);
        }
        String paymentId = "P-" + System.nanoTime();
        return new PaymentResult(paymentId);
    }

    /**
     * 결제를 환불한다 = 보상(rollback) 동작.
     * 배송 단계가 실패했을 때 퍼사드가 호출한다.
     */
    public void refund(String paymentId) {
        System.out.println("[Payment] refunded: " + paymentId);
    }
}