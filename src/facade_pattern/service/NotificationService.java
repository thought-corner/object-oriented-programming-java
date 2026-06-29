package facade_pattern.service;

/**
 * [알림 서브시스템]  ── 이미 제공된다고 가정. 호출만 한다.
 *
 * 퍼사드는 모든 단계가 성공한 마지막에 이걸 호출해 주문 확인을 보낸다.
 *
 * ★ 주의: 이 단계는 "best-effort"다.
 *   알림 전송이 실패(예외)하더라도 주문 자체는 성공으로 처리해야 한다.
 *   즉 퍼사드에서 이 호출은 try-catch 로 감싸 예외를 무시(로그만)하면 된다.
 *   (보상/롤백 대상이 아님)
 */
public class NotificationService {

    /**
     * 주문 확인 알림을 보낸다.
     * @throws RuntimeException 연락처가 비어 알림을 못 보낼 때
     *
     * [실패 조건] customerId 가 비어있으면 예외를 던진다.
     *   → 단, 이건 best-effort 단계다. 퍼사드는 이 예외를 잡아서 무시(로그만)하고
     *     주문은 그대로 성공 처리해야 한다. (보상 대상 아님)
     *     예) customerId="" 로 호출하면 예외가 나지만 주문 결과는 성공이어야 한다.
     */
    public void sendOrderConfirmation(String customerId, String trackingNo) {
        if (customerId == null || customerId.isBlank()) {
            throw new RuntimeException("알림 발송 실패: 연락처 없음");
        }
        System.out.println("[Notify] sent to " + customerId + ", tracking=" + trackingNo);
    }
}