package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.ElasticsearchRestClient;
import bio.ferlab.clin.es.ElasticsearchRestClient.IndexData;
import bio.ferlab.clin.es.data.PatientData;
import bio.ferlab.clin.es.data.UpdateRequestData;
import bio.ferlab.clin.es.data.builder.PatientDataBuilder;
import bio.ferlab.clin.utils.Extensions;
import bio.ferlab.clin.utils.JsonGenerator;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * Intercepts requests CREATE/UPDATE/DELETE requests, and index patient data to ES when needed.
 * Subscription couldn't be used in this scenario, as they do not offer a way to handle deletions and to filter out
 * unnecessary attributes.
 */
@Interceptor
public class IndexerInterceptor {
    private static final String INDEX_PATIENT = HapiProperties.getBioEsIndexPatient();

    private final ElasticsearchRestClient client;
    private final PatientDataBuilder patientDataBuilder;
    private final JsonGenerator jsonGenerator;
    private final IParser parser;

    public IndexerInterceptor(ApplicationContext appContext) {
        this.client = appContext.getBean(ElasticsearchRestClient.class);
        this.patientDataBuilder = appContext.getBean(PatientDataBuilder.class);
        this.jsonGenerator = appContext.getBean(JsonGenerator.class);
        this.parser = appContext.getBean(FhirContext.class).newJsonParser();
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_UPDATED)
    public void resourceUpdated(IBaseResource oldResource, IBaseResource newResource, RequestDetails requestDetails) {
        if (!isEsBundled(requestDetails) && newResource instanceof ServiceRequest) {
            final ServiceRequest serviceRequest = (ServiceRequest) newResource;
            if (serviceRequest.hasSubject()) {
                final BooleanType isSubmitted = (BooleanType) serviceRequest.getExtensionByUrl(Extensions.IS_SUBMITTED).getValue();
                final UpdateRequestData updateData = new UpdateRequestData(serviceRequest.getStatus().toCode(), isSubmitted.getValue());
                final IndexData indexData = new IndexData(serviceRequest.getSubject().getReference().split("/")[1], updateData.stringify(jsonGenerator));
                client.update(INDEX_PATIENT, indexData);
            } else {
                throw new ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException("Service Request without subject");
            }
        }
    }

    @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_DELETED)
    public void resourceDeleted(IBaseResource resource) {
        if (resource instanceof Patient) {
            client.delete(INDEX_PATIENT, resource.getIdElement().getIdPart());
        }
    }

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public boolean response(
            RequestDetails requestDetails,
            ServletRequestDetails servletRequestDetails,
            IBaseResource resource,
            ResponseDetails responseDetails) {

        if (isEsBundled(requestDetails)) {
            final String content = new String(requestDetails.getRequestContentsIfLoaded());
            final Bundle bundle = parser.parseResource(Bundle.class, content);
            final Bundle responseBundle = (Bundle) responseDetails.getResponseResource();
            final List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            for (int i = 0; i < entries.size(); i++) {
                final String id = responseBundle.getEntry().get(i).getResponse().getLocation().split("/")[1];
                entries.get(i).getResource().setId(id);
            }
            indexToEs(bundle);
        }
        return true;
    }

    private boolean isEsBundled(RequestDetails requestDetails) {
        return requestDetails.getParameters().containsKey("id") &&
                (requestDetails.getParameters().get("id")[0].contentEquals(HapiProperties.getBioEsPatientBundleId()) ||
                        requestDetails.getParameters().get("id")[0].contentEquals(HapiProperties.getBioEsRequestBundleId()));
    }


    private void indexToEs(Bundle bundle) {
        final PatientData patientData = this.patientDataBuilder.fromBundle(bundle);
        if (patientData != null) {
            final IndexData data = new IndexData(patientData.getId(), jsonGenerator.toString(patientData));
            client.index(INDEX_PATIENT, data);
        }
    }
}
