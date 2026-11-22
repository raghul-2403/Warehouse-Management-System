package org.vaadin.example;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Route("dashboard")
@PageTitle("WMS | Dashboard")
public class DashboardView extends VerticalLayout {

    public DashboardView(ProductRepository products, InventoryRepository inventoryRepo,
            CustomerRepository customers, CustomerOrderRepository ordersRepo) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        List<CustomerOrder> allOrders = ordersRepo.findAll();
        List<Inventory> stock = inventoryRepo.findAll();
        long stockUnits = stock.stream().filter(i -> i.getQuantity() != null)
                .mapToLong(Inventory::getQuantity).sum();
        long lowStock = stock.stream().filter(i -> i.getQuantity() != null
                && i.getReorderLevel() != null && i.getQuantity() <= i.getReorderLevel()).count();
        long newOrders = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.NEW).count();

        Div metrics = new Div();
        metrics.setWidthFull();
        metrics.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit,minmax(180px,1fr))")
                .set("gap", "15px");
        metrics.add(WmsDesign.metric("Total orders", "" + allOrders.size(), "dark"));
        metrics.add(WmsDesign.metric("Stock units", "" + stockUnits, "orange"));
        metrics.add(WmsDesign.metric("Customers", "" + customers.count(), "plain"));
        metrics.add(WmsDesign.metric("Low-stock records", "" + lowStock, "plain"));
        add(metrics);

        Div analytics = new Div();
        analytics.setWidthFull();
        analytics.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit,minmax(300px,1fr))")
                .set("gap", "16px");

        VerticalLayout monthly = WmsDesign.panel("Monthly orders", "Orders recorded in " + LocalDate.now().getYear());
        int[] months = new int[12];
        String currentYear = String.valueOf(LocalDate.now().getYear());
        for (CustomerOrder order : allOrders) {
            String date = String.valueOf(order.getOrderDate());
            if (date.length() >= 7 && date.startsWith(currentYear + "-")) {
                try {
                    int month = Integer.parseInt(date.substring(5, 7));
                    if (month >= 1 && month <= 12) months[month - 1]++;
                } catch (NumberFormatException ignored) { }
            }
        }
        int max = Math.max(1, Arrays.stream(months).max().orElse(0));
        Div columns = new Div();
        columns.setWidthFull();
        columns.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(12,minmax(0,1fr))")
                .set("align-items", "end").set("gap", "7px")
                .set("height", "225px").set("border-bottom", "1px solid #e6e5e8");
        for (int m = 0; m < 12; m++) {
            Div bar = new Div();
            int height = Math.max(3, (int) (months[m] * 200.0 / max));
            bar.getStyle().set("height", height + "px")
                    .set("background", m == LocalDate.now().getMonthValue() - 1
                            ? WmsDesign.ORANGE : WmsDesign.INK)
                    .set("border-radius", "5px 5px 0 0")
                    .set("width", "100%");
            bar.getElement().setProperty("title", (m + 1) + ": " + months[m] + " orders");
            columns.add(bar);
        }
        Div monthLabels = new Div();
        monthLabels.setWidthFull();
        monthLabels.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(12,minmax(0,1fr))")
                .set("gap", "7px");
        for (int m = 1; m <= 12; m++) {
            Span label = new Span(java.time.Month.of(m).getDisplayName(TextStyle.NARROW, Locale.ENGLISH));
            label.getStyle().set("font-size", "10px").set("text-align", "center")
                    .set("color", WmsDesign.MUTED);
            monthLabels.add(label);
        }
        monthly.add(columns, monthLabels);
        analytics.add(monthly);

        VerticalLayout statuses = WmsDesign.panel("Order pipeline", "Live fulfillment-stage breakdown");
        Map<OrderStatus, Long> counts = allOrders.stream()
                .filter(o -> o.getStatus() != null)
                .collect(Collectors.groupingBy(CustomerOrder::getStatus, Collectors.counting()));
        long total = Math.max(1, allOrders.size());
        for (OrderStatus status : OrderStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            HorizontalLayout line = new HorizontalLayout();
            line.setWidthFull();
            line.setJustifyContentMode(JustifyContentMode.BETWEEN);
            Span name = new Span(status.name());
            Span value = new Span(count + " orders");
            name.getStyle().set("color", WmsDesign.INK).set("font-size", "12px");
            value.getStyle().set("color", WmsDesign.MUTED).set("font-size", "12px");
            line.add(name, value);
            Div track = new Div();
            track.setWidthFull();
            track.getStyle().set("height", "9px").set("background", "#eeeef0")
                    .set("border-radius", "9px").set("overflow", "hidden");
            Div fill = new Div();
            fill.getStyle().set("height", "100%")
                    .set("width", (count * 100.0 / total) + "%")
                    .set("background", status == OrderStatus.NEW ? WmsDesign.ORANGE : WmsDesign.INK)
                    .set("border-radius", "9px");
            track.add(fill);
            statuses.add(line, track);
        }
        Paragraph pending = new Paragraph("New orders awaiting processing: " + newOrders);
        pending.getStyle().set("font-size", "12px").set("color", WmsDesign.ORANGE)
                .set("font-weight", "700");
        statuses.add(pending);
        analytics.add(statuses);
        add(analytics);

        VerticalLayout recent = WmsDesign.panel("Recent orders", "Latest recorded warehouse orders");
        List<CustomerOrder> sorted = allOrders.stream()
                .sorted(Comparator.comparing(o -> String.valueOf(o.getOrderDate()), Comparator.reverseOrder()))
                .limit(8).toList();
        if (sorted.isEmpty()) {
            recent.add(new Paragraph("No orders recorded yet."));
        } else {
            Grid<CustomerOrder> grid = new Grid<>();
            grid.addColumn(CustomerOrder::getId).setHeader("Order ID").setAutoWidth(true);
            grid.addColumn(o -> o.getCustomer() != null ? o.getCustomer().getName() : "—")
                    .setHeader("Customer").setAutoWidth(true);
            grid.addColumn(o -> o.getProduct() != null ? o.getProduct().getName() : "—")
                    .setHeader("Product").setAutoWidth(true);
            grid.addColumn(CustomerOrder::getQuantity).setHeader("Qty").setAutoWidth(true);
            grid.addColumn(o -> String.valueOf(o.getStatus())).setHeader("Status").setAutoWidth(true);
            grid.addColumn(o -> String.valueOf(o.getOrderDate())).setHeader("Date").setAutoWidth(true);
            grid.setItems(sorted);
            grid.setAllRowsVisible(true);
            grid.setWidthFull();
            recent.add(grid);
        }
        add(recent);
        WmsDesign.decorate(this, "Dashboard", "Live operations, fulfillment and stock performance");
    }
}
