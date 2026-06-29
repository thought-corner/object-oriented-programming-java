package facade_pattern.service;

/**
 * [재고 서브시스템]  ── 이미 제공된다고 가정하는 클래스. 내부는 손대지 말고 호출만 한다.
 *
 * 퍼사드(OrderFacade)는 이 클래스를 이렇게 사용한다:
 *   1) isAvailable() 로 재고가 있는지 먼저 확인
 *   2) reserve() 로 재고를 잡아두고, 돌려받은 "예약 ID"를 기억해 둔다
 *   3) 만약 뒤 단계(결제/배송)에서 실패하면 release(예약ID) 로 잡아둔 재고를 되돌린다(보상)
 */
public class InventoryService {

    /** 이 더미가 가진 재고 수량 (실제라면 DB 조회). sku별로 다르게 줄 수도 있다. */
    private static final int STOCK_PER_SKU = 5;

    /**
     * 재고가 충분한지 확인한다.
     * @return 요청 수량이 보유 재고 이하이면 true, 넘으면 false
     *
     * [실패 조건] qty 가 보유 재고(STOCK_PER_SKU=5)를 넘으면 false.
     *   → 퍼사드의 "재고 없음 → OrderException" 경로를 테스트할 수 있다.
     *     예) qty=6 으로 호출하면 false 가 나온다.
     */
    public boolean isAvailable(String sku, int qty) {
        boolean ok = qty <= STOCK_PER_SKU;
        System.out.println("[Inventory] check stock: sku=" + sku + ", qty=" + qty + " -> " + ok);
        return ok;
    }

    /**
     * 재고를 예약(선점)하고 "예약 ID"를 돌려준다.
     * 이 ID는 나중에 취소(release)할 때 필요하므로 퍼사드가 보관해야 한다.
     * @return 예약 ID (예: "R-123")
     */
    public String reserve(String sku, int qty) {
        String reservationId = "R-" + System.nanoTime();
        System.out.println("[Inventory] reserved: " + reservationId);
        return reservationId;
    }

    /**
     * 예약했던 재고를 취소한다 = 보상(rollback) 동작.
     * 결제/배송이 실패했을 때 퍼사드가 이걸 호출해서 재고를 원상복구한다.
     */
    public void release(String reservationId) {
        System.out.println("[Inventory] released: " + reservationId);
    }
}