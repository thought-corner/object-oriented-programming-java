package facade_pattern.service;

/**
 * [배송 서브시스템]  ── 이미 제공된다고 가정. 호출만 한다.
 *
 * 퍼사드는:
 *   1) 결제 성공 후 schedule() 로 배송을 예약하고 "운송장 번호"를 받는다
 *   2) schedule() 이 실패(예외)하면 → 이미 한 결제를 refund, 재고를 release 한다 (역순 보상)
 *   3) 성공하면 운송장 번호를 OrderResult 에 담는다
 */
public class ShippingService {

    /**
     * 배송을 예약하고 운송장 번호를 돌려준다.
     * @return 운송장 번호 (예: "T-123")
     * @throws RuntimeException 배송 불가 지역일 때
     *
     * [실패 조건] 배송 불가 지역("제주", "도서산간")이면 예외를 던진다.
     *   → 퍼사드의 "배송 실패 → refund(결제ID) → release(예약ID) → OrderException"
     *     (역순 보상) 경로를 테스트할 수 있다.
     *     예) address="제주" 로 호출하면 예외가 터진다.
     *
     *   ※ 배송 가능 권역 여부는 '배송 서비스의 책임'이라 여기서 검사한다.
     */
    public String schedule(String address, String sku, int qty) {
        if (address == null || address.contains("제주") || address.contains("도서산간")) {
            throw new RuntimeException("배송 불가 지역: " + address);
        }
        System.out.println("[Shipping] scheduled to " + address + " (" + sku + " x" + qty + ")");
        return "T-" + System.nanoTime();
    }
}