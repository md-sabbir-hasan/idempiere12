package org.mycompany.o2c.service;

import java.util.Collections;
import java.util.List;

import org.mycompany.o2c.dao.CategoryDAO;
import org.mycompany.o2c.model.Category;
import org.mycompany.o2c.util.ValidationUtils;

public class CategoryService {
    private final CategoryDAO dao = new CategoryDAO();

    /** Categories of the segment. Empty when no segment is selected. */
    public List<Category> getSegmentCategories(int segmentId) {
        if (!ValidationUtils.isId(segmentId))
            return Collections.emptyList();
        return dao.findBySegment(segmentId);
    }
}
