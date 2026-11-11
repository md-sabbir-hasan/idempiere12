package org.mycompany.o2c;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;

public class O2CFormFactory implements IFormFactory {
    @Override
    public ADForm newFormInstance(String formName) {
        if (formName.equals("org.mycompany.o2c.WSalesOrderForm"))
            return new WSalesOrderForm();
        return null;   // onno factory ke chance dao
    }
}