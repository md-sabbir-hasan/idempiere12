package org.mycompany.o2c;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.mycompany.o2c.ui.WSalesOrderForm;

public class O2CFormFactory implements IFormFactory {
    @Override
    public ADForm newFormInstance(String formName) {
        // The old class name is still accepted so the AD Form record
        // (Classname org.mycompany.o2c.WSalesOrderForm) does not have to be changed.
        if (formName.equals("org.mycompany.o2c.ui.WSalesOrderForm")
                || formName.equals("org.mycompany.o2c.WSalesOrderForm"))
            return new WSalesOrderForm();
        return null;
    }
}
