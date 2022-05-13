package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@Interceptor
public class ServiceRequestPerformerInterceptor {
  
  public static final String SERVICE_REQUEST_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/service-request-code";
  public static final String ANALYSIS_REQUEST_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/analysis-request-code";

  private final ResourceDaoConfiguration configuration;

  public ServiceRequestPerformerInterceptor(ResourceDaoConfiguration configuration) {
    this.configuration = configuration;
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
  public void created(RequestDetails requestDetails, IBaseResource resource) {
    this.handleRequestAndResource(requestDetails, resource);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    this.handleRequestAndResource(requestDetails, newResource);
  }
  
  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource resource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && resource instanceof ServiceRequest;
  }
  
  private String extractCode(ServiceRequest serviceRequest) {
    String code = null;
    if (serviceRequest != null && serviceRequest.hasCode() && serviceRequest.getCode().hasCoding()) {
      for (Coding coding: serviceRequest.getCode().getCoding()) {
        if (List.of(ANALYSIS_REQUEST_CODE, SERVICE_REQUEST_CODE).contains(coding.getSystem()) && StringUtils.isNotBlank(coding.getCode())) {
           code = coding.getCode();
        }
      }
    }
    return Optional.ofNullable(code).orElseThrow(() -> new InvalidRequestException("Missing code/coding in service request"));
  }
  
  private OrganizationAffiliation findOrganizationAffiliationByCode(String code) {
    final IBundleProvider searchResultBundle = this.configuration.organizationAffiliationDAO
        .search(SearchParameterMap.newSynchronous().add(OrganizationAffiliation.SP_SPECIALTY, new TokenParam(code)));
    if (searchResultBundle.isEmpty()) {
      throw new InvalidRequestException("Can't find organization affiliation attached to code " + code);
    }
    return (OrganizationAffiliation) searchResultBundle.getAllResources().get(0);
  }
  
  private void handleRequestAndResource(RequestDetails requestDetails, IBaseResource resource) {
    if (isValidRequestAndResource(requestDetails, resource)) {
      final ServiceRequest serviceRequest = (ServiceRequest) resource;
      final String code = extractCode(serviceRequest);
      final OrganizationAffiliation affiliation = findOrganizationAffiliationByCode(code);
      final String ldm = affiliation.getParticipatingOrganization().getReference();
      serviceRequest.setPerformer(List.of(new Reference(ldm)));
    }
  }
}
