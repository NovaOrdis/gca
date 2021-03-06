package com.novaordis.gc.model.event.cms;

import com.novaordis.gc.model.CollectionType;
import com.novaordis.gc.model.Timestamp;

/**
 * @author <a href="mailto:ovidiu@novaordis.com">Ovidiu Feodorov</a>
 *
 * Copyright 2013 Nova Ordis LLC
 */
public class CMSConcurrentPreclean extends CMSEvent
{
    // Constants ---------------------------------------------------------------------------------------------------------------------------

    // Static ------------------------------------------------------------------------------------------------------------------------------

    // Attributes --------------------------------------------------------------------------------------------------------------------------

    // Constructors ------------------------------------------------------------------------------------------------------------------------

    public CMSConcurrentPreclean(Timestamp ts)
    {
        super(ts, 0L);
    }

    // GCEvent implementation --------------------------------------------------------------------------------------------------------------

    @Override
    public CollectionType getCollectionType()
    {
        return CollectionType.CMS_CONCURRENT_PRECLEAN;
    }

    // Public ------------------------------------------------------------------------------------------------------------------------------

    // Package protected -------------------------------------------------------------------------------------------------------------------

    // Protected ---------------------------------------------------------------------------------------------------------------------------

    // Private -----------------------------------------------------------------------------------------------------------------------------

    // Inner classes -----------------------------------------------------------------------------------------------------------------------
}



