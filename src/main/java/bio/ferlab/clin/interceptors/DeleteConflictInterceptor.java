package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.model.DeleteConflictList;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.storage.TransactionDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

public class DeleteConflictInterceptor {

    @Hook(Pointcut.STORAGE_PRESTORAGE_DELETE_CONFLICTS)
    public void resolveConflicts(DeleteConflictList deleteConflictList, RequestDetails requestDetails, ServletRequestDetails servletRequestDetails, TransactionDetails transactionDetails) {
        deleteConflictList.removeIf(deleteConflict -> deleteConflict.getSourcePath().equals("AuditEvent.entity.what"));
    }


}
