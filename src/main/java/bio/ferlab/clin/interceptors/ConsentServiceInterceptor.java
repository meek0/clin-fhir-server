package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.audit.AuditEventsBuilder;
import bio.ferlab.clin.audit.AuditTrail;
import bio.ferlab.clin.user.RequesterData;
import bio.ferlab.clin.utils.Constants;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class ConsentServiceInterceptor implements IConsentService {
    private final static Logger log = LoggerFactory.getLogger(ConsentServiceInterceptor.class);
    public static final String AUDIT_EVENT_RESOURCE_TYPE = "AuditEvent";
    private final AuditTrail auditTrail;

    public ConsentServiceInterceptor(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Override
    public ConsentOutcome startOperation(RequestDetails requestDetails, IConsentContextServices contextServices) {
        return ConsentOutcome.PROCEED;
    }

    @Override
    public ConsentOutcome canSeeResource(RequestDetails requestDetails, IBaseResource theResource, IConsentContextServices contextServices) {
        return ConsentOutcome.PROCEED;
    }

    @Override
    public ConsentOutcome willSeeResource(RequestDetails requestDetails, IBaseResource theResource, IConsentContextServices contextServices) {
        return ConsentOutcome.AUTHORIZED;
    }

    @Override
    public void completeOperationSuccess(RequestDetails requestDetails, IConsentContextServices contextServices) {
        final boolean result = this.logOperation(requestDetails, true);
        if (result) {
            log.info(String.format("Successful operation [%s] logged.", requestDetails.getRestOperationType()));
        } else {
            log.info(String.format("Successful operation [%s] not logged.", requestDetails.getRestOperationType()));
        }
    }

    @Override
    public void completeOperationFailure(RequestDetails requestDetails, BaseServerResponseException theException, IConsentContextServices contextServices) {
        this.logOperation(requestDetails, false);
        log.info(String.format("Failed operation [%s] logged.", requestDetails.getRestOperationType()));
    }

    private boolean logOperation(RequestDetails requestDetails, boolean successful) {
        final IBaseResource resource = requestDetails.getResource();
        final RequesterData requesterData = (RequesterData) requestDetails.getAttribute(Constants.REQUESTER_DATA_KEY);
        final AuditEventsBuilder builder = new AuditEventsBuilder(requesterData);
        final List<AuditEvent> events = new ArrayList<>();

        if (requestDetails.getRequestType() == RequestTypeEnum.GET) {
            final String resourceName = requestDetails.getRequestPath();
            if (StringUtils.isNotBlank(resourceName) && !AUDIT_EVENT_RESOURCE_TYPE.equals(resourceName)) {
                events.addAll(builder.addReadAction(resourceName).build());
            }
        } else {
            if (resource instanceof Bundle) {
                events.addAll(builder.addBundle((Bundle) resource).build());
            } else {
                final AuditEvent.AuditEventAction action = getActionFromRestOperationType(requestDetails.getRestOperationType());
                if (resource instanceof Resource) {
                    events.addAll(builder.addResource((Resource) resource, action).build());
                } else if (requestDetails.getId() != null){
                    events.addAll(builder.addByIdAndActionType(requestDetails.getId(), action).build());
                }
            }
        }

        if (!events.isEmpty()) {
            auditTrail.auditEvents(events, successful);
            return true;
        }
        return false;
    }

    private AuditEvent.AuditEventAction getActionFromRestOperationType(RestOperationTypeEnum type) {
        switch (type) {
            case ADD_TAGS:
            case PATCH:
            case META_DELETE:
            case META:
            case META_ADD:
            case UPDATE:
            case TRANSACTION:
            case EXTENDED_OPERATION_INSTANCE:
            case EXTENDED_OPERATION_SERVER:
            case DELETE_TAGS:
                return AuditEvent.AuditEventAction.U;
            case GET_TAGS:
            case METADATA:
            case VREAD:
            case VALIDATE:
            case SEARCH_TYPE:
            case SEARCH_SYSTEM:
            case READ:
            case HISTORY_TYPE:
            case HISTORY_SYSTEM:
            case HISTORY_INSTANCE:
            case GRAPHQL_REQUEST:
            case GET_PAGE:
            case EXTENDED_OPERATION_TYPE:
                return AuditEvent.AuditEventAction.R;
            case CREATE:
                return AuditEvent.AuditEventAction.C;
            case DELETE:
                return AuditEvent.AuditEventAction.D;
        }
        throw new IllegalStateException("Invalid rest operation type");
    }
}
