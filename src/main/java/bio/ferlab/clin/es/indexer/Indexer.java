package bio.ferlab.clin.es.indexer;

import bio.ferlab.clin.es.IndexerHelper;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;

abstract class Indexer {
    public void index(RequestDetails requestDetails) {
        if (requestDetails.getRequestType() == RequestTypeEnum.POST || requestDetails.getRequestType() == RequestTypeEnum.PUT) {
            final IBaseResource processedResource = requestDetails.getResource();
            if (IndexerHelper.isIndexable(processedResource)) {
                this.doIndex(requestDetails, processedResource);
            }
        }
    }

    protected abstract void doIndex(RequestDetails requestDetails, IBaseResource resource);
}
