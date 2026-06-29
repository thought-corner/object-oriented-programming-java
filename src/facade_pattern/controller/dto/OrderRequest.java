package facade_pattern.controller.dto;

public record OrderRequest(String customerId, String sku, int qty, long amount, String address) {
}
