package org.mycompany.o2c.model;

/** Result of submitting an order: its status and, while approval is pending, who it waits for. */
public record SubmitResult(int orderId, String documentNo, String docStatus, String waitingFor) {
}
