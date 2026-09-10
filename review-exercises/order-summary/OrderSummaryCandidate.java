package reviewexercises.ordersummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OrderSummaryCandidate {
  public record Order(
      String customerId,
      String orderId,
      BigDecimal subtotal,
      int discountPercent,
      BigDecimal paidAmount) {}

  public record Summary(
      int orderCount,
      BigDecimal totalDue,
      BigDecimal totalPaid,
      int reconciledOrderCount) {}

  private final List<Order> snapshot;
  private final Map<String, BigDecimal> dueByOrder = new HashMap<>();

  public OrderSummaryCandidate(List<Order> orders) {
    snapshot = List.copyOf(orders);
  }

  public Summary summarize(String customerId) {
    int orderCount = 0;
    int reconciledOrderCount = 0;
    BigDecimal totalDue = new BigDecimal("0.00");
    BigDecimal totalPaid = new BigDecimal("0.00");

    for (Order order : snapshot) {
      if (!order.customerId().equals(customerId)) {
        continue;
      }

      BigDecimal due = dueByOrder.computeIfAbsent(order.orderId(),
          ignored -> discountedTotal(order));
      orderCount++;
      totalDue = totalDue.add(due);
      totalPaid = totalPaid.add(order.paidAmount());
      if (due.equals(order.paidAmount())) {
        reconciledOrderCount++;
      }
    }

    return new Summary(orderCount, totalDue, totalPaid.setScale(2), reconciledOrderCount);
  }

  public List<Order> page(String customerId, int pageIndex, int pageSize) {
    if (pageIndex < 0 || pageSize <= 0) {
      throw new IllegalArgumentException("pageIndex must be nonnegative and pageSize positive");
    }

    List<Order> customerOrders = snapshot.stream()
        .filter(order -> order.customerId().equals(customerId))
        .toList();
    long start = (long) pageIndex * pageSize;
    if (start >= customerOrders.size()) {
      return List.of();
    }

    int end = (int) Math.min(start + pageSize - 1, customerOrders.size());
    return customerOrders.subList((int) start, end);
  }

  private BigDecimal discountedTotal(Order order) {
    BigDecimal discountRate = BigDecimal.valueOf(order.discountPercent() / 100);
    return order.subtotal().subtract(order.subtotal().multiply(discountRate))
        .setScale(2, RoundingMode.HALF_UP);
  }
}
