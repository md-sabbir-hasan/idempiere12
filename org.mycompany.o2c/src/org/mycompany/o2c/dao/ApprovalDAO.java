package org.mycompany.o2c.dao;

import java.util.Properties;

import org.compiere.model.MOrder;
import org.compiere.model.MPInstance;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.wf.MWorkflow;
import org.mycompany.o2c.model.SubmitResult;
import org.mycompany.o2c.util.DBUtils;
import org.mycompany.o2c.util.ValidationException;

/**
 * Sends a Draft order into iDempiere's own document workflow (the same one the Complete button of the
 * Sales Order window starts). The approval layers are nodes of that workflow, see docs/APPROVAL_SETUP.md.
 */
public class ApprovalDAO {

    /** Name of the first approval node (Zone Manager). Its approver is set per order by ZoneApprovalHandler. */
    public static final String ZM_NODE = "ZM Approval";

    /** Name of the second approval node (special discount). Role "Sales Admin" + Segment Access decide who may approve. */
    public static final String ADMIN_NODE = "Sales Admin Approval";

    /** Name of the Sales Admin role. */
    public static final String ADMIN_ROLE = "Sales Admin";

    /** {AD_Process_ID, AD_Workflow_ID} behind the Document Action button of C_Order; null if not found. */
    private int[] docActionWorkflow() {
        return DBUtils.queryOne(
            "SELECT c.ad_process_id, p.ad_workflow_id FROM ad_column c "
          + "JOIN ad_process p ON p.ad_process_id = c.ad_process_id "
          + "WHERE c.ad_table_id = ? AND c.columnname = 'DocAction' AND p.ad_workflow_id IS NOT NULL",
            rs -> new int[] { rs.getInt(1), rs.getInt(2) }, Integer.valueOf(MOrder.Table_ID));
    }

    private boolean hasNode(int workflowId, String name) {
        return !DBUtils.queryString(
            "SELECT 1 FROM ad_wf_node WHERE ad_workflow_id = ? AND name = ? AND isactive = 'Y' LIMIT 1",
            Integer.valueOf(workflowId), name).isEmpty();
    }

    /** Which approval step an order is waiting at: "ZM Approval - Rahim"; empty when none. */
    public String waitingFor(int orderId) {
        return DBUtils.queryString(
            "SELECT n.name || CASE WHEN u.name IS NULL THEN '' ELSE ' - ' || u.name END "
          + "FROM ad_wf_activity a JOIN ad_wf_node n ON n.ad_wf_node_id = a.ad_wf_node_id "
          + "LEFT JOIN ad_user u ON u.ad_user_id = a.ad_user_id "
          + "WHERE a.ad_table_id = ? AND a.record_id = ? AND a.processed = 'N' "
          + "ORDER BY a.ad_wf_activity_id DESC LIMIT 1",
            Integer.valueOf(MOrder.Table_ID), Integer.valueOf(orderId));
    }

    /** An active user with role Sales Admin and Segment Access to the Segment exists. */
    public boolean hasSalesAdmin(int clientId, int segmentId) {
        return !DBUtils.queryString(
            "SELECT 1 FROM ad_user u "
          + "JOIN ad_user_roles ur ON ur.ad_user_id = u.ad_user_id AND ur.isactive = 'Y' "
          + "JOIN ad_role r ON r.ad_role_id = ur.ad_role_id AND r.isactive = 'Y' "
          + "JOIN ad_user_segmentaccess sa ON sa.ad_user_id = u.ad_user_id AND sa.isactive = 'Y' "
          + "WHERE u.ad_client_id = ? AND u.isactive = 'Y' AND r.name = ? AND sa.c_activity_id = ? LIMIT 1",
            Integer.valueOf(clientId), ADMIN_ROLE, Integer.valueOf(segmentId)).isEmpty();
    }

    /** Last rejection of this Draft order (who, when, the reason the approver typed); empty when none. */
    public String returnedInfo(int orderId) {
        return DBUtils.queryString(
            "SELECT 'Returned by ' || COALESCE(u.name, '?') || ' (' || to_char(a.updated, 'DD-MM-YYYY') || ')' "
          + "|| CASE WHEN COALESCE(a.textmsg, '') = '' THEN '' ELSE ': ' || a.textmsg END "
          + "FROM ad_wf_activity a JOIN ad_wf_node n ON n.ad_wf_node_id = a.ad_wf_node_id "
          + "JOIN c_order o ON o.c_order_id = a.record_id AND a.ad_table_id = ? "
          + "LEFT JOIN ad_user u ON u.ad_user_id = a.updatedby "
          + "WHERE a.record_id = ? AND o.docstatus = 'DR' AND a.processed = 'Y' AND n.name IN (?, ?) "
          + "ORDER BY a.ad_wf_activity_id DESC LIMIT 1",
            Integer.valueOf(MOrder.Table_ID), Integer.valueOf(orderId), ZM_NODE, ADMIN_NODE);
    }

    /**
     * Starts the workflow for a Draft order that is already stored. The order must stop at the
     * approval steps; an order that would complete without them is refused by the check below.
     */
    public SubmitResult submit(int orderId, int userId) {
        Properties ctx = Env.getCtx();
        MOrder order = new MOrder(ctx, orderId, null);
        if (order.get_ID() != orderId)
            throw new ValidationException("The order was not found.");
        if (!MOrder.DOCSTATUS_Drafted.equals(order.getDocStatus()))
            throw new ValidationException("Only a Draft order can be submitted. Status is "
                    + order.getDocStatus() + ".");

        int[] wf = docActionWorkflow();
        if (wf == null || wf[1] <= 0)
            throw new ValidationException("No workflow is linked to the Document Action of the Order.");
        if (!hasNode(wf[1], ZM_NODE))
            throw new ValidationException("The approval workflow is not set up: the node '" + ZM_NODE
                    + "' is missing in the Order workflow. See docs/APPROVAL_SETUP.md. "
                    + "The order was NOT submitted (it stays a Draft).");

        java.math.BigDecimal special = (java.math.BigDecimal) order.get_Value("XX_SpecialDiscount");
        if (special != null && special.signum() > 0 && !hasSalesAdmin(order.getAD_Client_ID(), order.getC_Activity_ID()))
            throw new ValidationException("This order has a special discount, so a Sales Admin must approve it, "
                    + "but no active Sales Admin has Segment Access to this Segment. "
                    + "Add Segment Access to a Sales Admin user, then submit again. The order stays a Draft.");

        order.setDocAction(DocAction.ACTION_Complete);
        order.saveEx();

        ProcessInfo pi = new ProcessInfo("Submit Sales Order", wf[0], MOrder.Table_ID, orderId);
        pi.setAD_User_ID(userId);
        pi.setAD_Client_ID(order.getAD_Client_ID());
        MPInstance instance = new MPInstance(ctx, wf[0], MOrder.Table_ID, orderId, null);
        instance.saveEx();
        pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());
        
        MWorkflow workflow = MWorkflow.get(ctx, wf[1]);
        workflow.startWait(pi);

        MOrder after = new MOrder(ctx, orderId, null);
        if (MOrder.DOCSTATUS_Drafted.equals(after.getDocStatus()))
            throw new ValidationException("The order could not be submitted and is still a Draft. "
                    + "Check the Workflow Process window for the reason.");
        return new SubmitResult(orderId, after.getDocumentNo(), after.getDocStatus(), waitingFor(orderId));
    }
}
