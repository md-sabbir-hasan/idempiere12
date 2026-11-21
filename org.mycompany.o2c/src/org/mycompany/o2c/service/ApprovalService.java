package org.mycompany.o2c.service;

import java.sql.Timestamp;

import org.mycompany.o2c.dao.ApprovalDAO;
import org.mycompany.o2c.dao.ZoneManagerDAO;
import org.mycompany.o2c.model.SubmitResult;
import org.mycompany.o2c.model.ZoneManager;
import org.mycompany.o2c.util.DBUtils;
import org.mycompany.o2c.util.ValidationException;

/**
 * Approval rules of a Sales Order. The layers themselves live in iDempiere's workflow (so they can be
 * changed without code); this class only checks that the first approver (Zone Manager of the order's
 * Zone) exists, and starts the workflow.
 */
public class ApprovalService {
    private final ZoneManagerDAO zones = new ZoneManagerDAO();
    private final ApprovalDAO dao = new ApprovalDAO();

    /**
     * Zone Manager of the Zone above the territory, valid on the order date.
     *
     * @param date ignored: the Zone Manager valid TODAY approves (not the one of the order date)
     * @throws ValidationException when there is no Zone or no Zone Manager: the order must not be submitted
     */
    public ZoneManager requireZoneManager(int territoryId, int docTypeId, int segmentId, Timestamp date) {
        ZoneManager zm = zones.find(DBUtils.clientId(), territoryId, docTypeId, segmentId, null);
        if (zm == null)
            throw new ValidationException("No Zone was found above this Territory in the Sales Region tree, "
                    + "so the approver cannot be decided.");
        if (zm.userId() <= 0)
            throw new ValidationException("Zone " + zm.zoneName() + " has no Zone Manager. "
                    + "Assign one in Officer Assignment (Zone level), then submit again.");
        if (zm.userId() == org.compiere.util.Env.getAD_User_ID(org.compiere.util.Env.getCtx()))
            throw new ValidationException("You are the Zone Manager of " + zm.zoneName() + ", so you cannot "
                    + "approve your own order. Ask your manager to assign another Zone Manager for this "
                    + "Sales Type and Segment, or to place the order.");
        return zm;
    }

    /** Why the order is a Draft again after a rejection: "Returned by Rahim (12-11-2026): reason"; empty if never. */
    public String returnedInfo(int orderId) {
        return dao.returnedInfo(orderId);
    }

    public SubmitResult submit(int orderId, int userId) {
        return dao.submit(orderId, userId);
    }

    public String waitingFor(int orderId) {
        return dao.waitingFor(orderId);
    }
}
