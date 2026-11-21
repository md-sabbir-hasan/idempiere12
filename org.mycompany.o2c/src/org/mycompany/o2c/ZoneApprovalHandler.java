package org.mycompany.o2c;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.PO;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFNode;
import org.mycompany.o2c.dao.ApprovalDAO;
import org.mycompany.o2c.dao.ZoneManagerDAO;
import org.mycompany.o2c.model.ZoneManager;
import org.osgi.service.event.Event;

/**
 * Makes the first approval step dynamic: when the workflow creates the activity of the node
 * "ZM Approval" for a Sales Order, the activity is given to the Zone Manager of the order's Zone
 * (parent of the Territory, assigned in Officer Assignment, valid today), so only that person sees it in
 * Workflow Activities.
 *
 * There is NO fallback to the Zonal Manager role: with a role fallback every Zone Manager could approve
 * the orders of every Zone. When no Zone Manager is found the activity is not created and the
 * submit fails (the order stays as it was); ApprovalService checks this before the submit as well.
 */
public class ZoneApprovalHandler extends AbstractEventHandler {
    private final ZoneManagerDAO zones = new ZoneManagerDAO();

    @Override
    protected void initialize() {
        registerTableEvent(IEventTopics.PO_BEFORE_NEW, MWFActivity.Table_Name);
    }

    @Override
    protected void doHandleEvent(Event event) {
        PO po = getPO(event);
        if (!(po instanceof MWFActivity))
            return;
        MWFActivity activity = (MWFActivity) po;
        if (activity.getAD_Table_ID() != MOrder.Table_ID)
            return;
        MWFNode node = activity.getNode();
        if (node == null || !ApprovalDAO.ZM_NODE.equals(node.getName()))
            return;
        MOrder order = new MOrder(activity.getCtx(), activity.getRecord_ID(), null);
        ZoneManager zm = zones.find(order.getAD_Client_ID(), order.get_ValueAsInt("C_SalesRegion_ID"),
                order.getC_DocTypeTarget_ID(), order.getC_Activity_ID(), null);
        if (zm == null || zm.userId() <= 0)
            throw new AdempiereException("No Zone Manager for order " + order.getDocumentNo()
                    + ": assign one in Officer Assignment (Zone level, same Sales Type and Segment).");
        if (zm.userId() == order.getCreatedBy())
            throw new AdempiereException("The Zone Manager created order " + order.getDocumentNo()
                    + " himself and cannot approve it.");
        activity.setAD_User_ID(zm.userId());
    }
}
