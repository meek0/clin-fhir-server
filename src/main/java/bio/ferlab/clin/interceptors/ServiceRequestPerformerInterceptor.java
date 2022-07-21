package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import bio.ferlab.clin.utils.ResourceFinder;
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
  
  public static final String ANALYSIS_REQUEST_CODE = "http://fhir.cqgc.ferlab.bio/CodeSystem/analysis-request-code";

  private final ResourceDaoConfiguration configuration;
  private final ResourceFinder resourceFinder;

  public ServiceRequestPerformerInterceptor(ResourceDaoConfiguration configuration, ResourceFinder resourceFinder) {
    this.configuration = configuration;
    this.resourceFinder = resourceFinder;
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
        if (ANALYSIS_REQUEST_CODE.equals(coding.getSystem()) && StringUtils.isNotBlank(coding.getCode())) {
           code = coding.getCode();
        }
      }
    }
    return Optional.ofNullable(code).orElseThrow(() -> new InvalidRequestException("Missing code/coding in service request"));
  }
  
  private String extractEp(RequestDetails requestDetails, ServiceRequest serviceRequest) {
    final String subjectRef = serviceRequest.getSubject().getReference();
    return resourceFinder.findPatientFromRequestOrDAO(requestDetails, subjectRef)
        .orElseThrow(() -> new InvalidRequestException("Can't find subject patient " + subjectRef))
        .getManagingOrganization().getReference();
  }
  
  private OrganizationAffiliation findOrganizationAffiliationByCode(String ep, String code) {
    final RuntimeException notFound = new InvalidRequestException("Can't find organization affiliation attached to code: " + code + " with ep: " + ep);
    final IBundleProvider searchResultBundle = this.configuration.organizationAffiliationDAO
        .search(SearchParameterMap.newSynchronous().add(OrganizationAffiliation.SP_SPECIALTY, new TokenParam(code)));
    if (searchResultBundle.isEmpty()) {
      throw notFound;
    }
    return searchResultBundle.getAllResources().stream()
        .map(aff -> (OrganizationAffiliation)aff)
        .filter(aff -> ep.equals(aff.getOrganization().getReference()))
        .findFirst().orElseThrow(() -> notFound);
  }
  
  private void handleRequestAndResource(RequestDetails requestDetails, IBaseResource resource) {
    if (isValidRequestAndResource(requestDetails, resource)) {
      final ServiceRequest serviceRequest = (ServiceRequest) resource;
      if (serviceRequest.getPerformer().isEmpty()) {
        final String code = extractCode(serviceRequest);
        final String ep = extractEp(requestDetails, serviceRequest);
        final OrganizationAffiliation affiliation = findOrganizationAffiliationByCode(ep, code);
        final String ldm = affiliation.getParticipatingOrganization().getReference();
        serviceRequest.setPerformer(List.of(new Reference(ldm)));
      }
    }
  }
}
