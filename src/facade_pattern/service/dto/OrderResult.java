package facade_pattern.service.dto;

public record OrderResult(String orderNo, String trackingNo, String paymentId) {

    @Override
    public String toString() {
        return "OrderResult{" +
                "orderNo='" + orderNo + '\'' +
                ", trackingNo='" + trackingNo + '\'' +
                ", paymentId='" + paymentId + '\'' +
                '}';
    }
}
