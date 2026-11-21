package org.mycompany.o2c;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.mycompany.o2c.dao.ApprovalDAO;
import org.mycompany.o2c.util.DBUtils;
import org.osgi.service.event.Event;

/**
 * Two rules around the approval steps of a Sales Order (steps "ZM Approval" and "Sales Admin Approval"):
 *
 * 1. WHO MAY APPROVE (before the order is marked approved):
 *    - nobody approves an order he created himself;
 *    - ZM Approval: only the user the activity is assigned to (the Zone Manager);
 *    - Sales Admin Approval: only a user with the role Sales Admin who has Segment Access (User window,
 *      AD_User_SegmentAccess) to the order's Segment.
 *
 * 2. REJECTED = BACK TO THE MAKER: when an approver rejects, the order gets "Not Approved" (standard).
 *    It is put back to Draft so the sales representative can change it and submit again. The reason the
 *    approver typed stays on the workflow activity (shown on the order form, see ApprovalDAO.returnedInfo).
 */
public class ApprovalGuardHandler extends AbstractEventHandler {

    @Override
    protected void initialize() {
        registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MOrder.Table_Name);
        registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MOrder.Table_Name);
    }

    @Override
    protected void doHandleEvent(Event event) {
        PO po = getPO(event);
        if (!(po instanceof MOrder))
            return;
        MOrder order = (MOrder) po;
        if (!order.isSOTrx())
            return;
        String topic = event.getTopic();
        if (IEventTopics.PO_BEFORE_CHANGE.equals(topic))
            checkApprover(order);
        else if (IEventTopics.PO_AFTER_CHANGE.equals(topic))
            returnToMaker(order);
    }

    // ------------------------------------------------------------------ rule 1

    private void checkApprover(MOrder order) {
        if (!order.is_ValueChanged(MOrder.COLUMNNAME_IsApproved) || !order.isApproved())
            return;
        if (!MOrder.DOCSTATUS_InProgress.equals(order.getDocStatus()))
            return;
        // the approval step the order is waiting at now ("" = not one of our steps, e.g. other tenants)
        String step = DBUtils.queryString(
            "SELECT n.name FROM ad_wf_activity a JOIN ad_wf_node n ON n.ad_wf_node_id = a.ad_wf_node_id "
          + "WHERE a.ad_table_id = ? AND a.record_id = ? AND a.processed = 'N' AND n.name IN (?, ?) "
          + "ORDER BY a.ad_wf_activity_id DESC LIMIT 1",
            Integer.valueOf(MOrder.Table_ID), Integer.valueOf(order.getC_Order_ID()),
            ApprovalDAO.ZM_NODE, ApprovalDAO.ADMIN_NODE);
        if (step.isEmpty())
            return;

        int approver = Env.getAD_User_ID(order.getCtx());
        if (approver == order.getCreatedBy())
            throw new AdempiereException("You created order " + order.getDocumentNo()
                    + ", so you cannot approve it.");

        if (ApprovalDAO.ZM_NODE.equals(step)) {
            int assigned = DB.getSQLValue(null,
                "SELECT COALESCE(MAX(a.ad_user_id), 0) FROM ad_wf_activity a "
              + "JOIN ad_wf_node n ON n.ad_wf_node_id = a.ad_wf_node_id "
              + "WHERE a.ad_table_id = ? AND a.record_id = ? AND a.processed = 'N' AND n.name = ?",
                MOrder.Table_ID, order.getC_Order_ID(), ApprovalDAO.ZM_NODE);
            if (assigned > 0 && assigned != approver)
                throw new AdempiereException("Order " + order.getDocumentNo()
                        + " must be approved by its Zone Manager.");
        } else {
            String ok = DBUtils.queryString(
                "SELECT 1 FROM ad_user_roles ur JOIN ad_role r ON r.ad_role_id = ur.ad_role_id "
              + "JOIN ad_user_segmentaccess sa ON sa.ad_user_id = ur.ad_user_id "
              + "WHERE ur.ad_user_id = ? AND ur.isactive = 'Y' AND r.isactive = 'Y' AND r.name = ? "
              + "AND sa.isactive = 'Y' AND sa.c_activity_id = ? LIMIT 1",
                Integer.valueOf(approver), ApprovalDAO.ADMIN_ROLE, Integer.valueOf(order.getC_Activity_ID()));
            if (ok.isEmpty())
                throw new AdempiereException("You can approve only the orders of the Segments you have "
                        + "Segment Access to (role " + ApprovalDAO.ADMIN_ROLE + "). Order "
                        + order.getDocumentNo() + " is of another Segment.");
        }
    }

    // ------------------------------------------------------------------ rule 2

    private void returnToMaker(MOrder order) {
        if (!order.is_ValueChanged(MOrder.COLUMNNAME_DocStatus)
                || !MOrder.DOCSTATUS_NotApproved.equals(order.getDocStatus()))
            return;
        // only orders that were in our approval steps (other tenants keep the standard behaviour)
        int steps = DB.getSQLValue(order.get_TrxName(),
            "SELECT COUNT(*) FROM ad_wf_activity a JOIN ad_wf_node n ON n.ad_wf_node_id = a.ad_wf_node_id "
          + "WHERE a.ad_table_id = ? AND a.record_id = ? AND n.name IN (?, ?)",
            MOrder.Table_ID, order.getC_Order_ID(), ApprovalDAO.ZM_NODE, ApprovalDAO.ADMIN_NODE);
        if (steps <= 0)
            return;
        DB.executeUpdateEx("UPDATE c_order SET docstatus = 'DR', docaction = 'CO', processed = 'N', "
            + "isapproved = 'N', processing = 'N', updated = now() WHERE c_order_id = ?",
            new Object[] { Integer.valueOf(order.getC_Order_ID()) }, order.get_TrxName());
    }
}
