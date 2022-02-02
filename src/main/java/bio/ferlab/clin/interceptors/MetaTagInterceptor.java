package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.utils.Helpers;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Interceptor
public class MetaTagInterceptor {

  private static final String TOKEN_ATTR_FHIR_ORG_ID = "fhir_organization_id";
  private static final String ALL_TAGS = "*";
  private static final List<String> RESOURCES_WITH_TAGS = List.of("ServiceRequest");

  private final BioProperties bioProperties;
  
  public MetaTagInterceptor(BioProperties bioProperties) {
    this.bioProperties = bioProperties;
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
  public void created(RequestDetails requestDetails, IBaseResource resource) {
    this.handleRequestAndResource(requestDetails, resource);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    this.handleRequestAndResource(requestDetails, newResource);
  }

  /*@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void read(RequestDetails requestDetails) {
    if (RequestTypeEnum.GET.equals(requestDetails.getRequestType())) {
      requestDetails.addParameter("_tag", new String[]{"CHUSJ"});
    }
  }*/
  
  public boolean canSeeResource(RequestDetails requestDetails, IBaseResource resource) {
    if (RequestTypeEnum.GET.equals(requestDetails.getRequestType())
        && bioProperties.isTaggingEnabled()
        && isResourceWithTags(resource)) {
      final List<String> tokenTags = getTags(requestDetails);
      final List<String> resourceTags = resource.getMeta().getTag().stream().map(IBaseCoding::getCode).collect(Collectors.toList());
      return tokenTags.contains(ALL_TAGS) || resourceTags.stream().anyMatch(tokenTags::contains);
    }
    return true;
  }

  private List<String> getTags(RequestDetails requestDetails) {
    final var bearer = requestDetails.getHeader(HttpHeaders.AUTHORIZATION);
    final var rpt = Helpers.extractAccessTokenFromBearer(bearer);
    final var jwt = JWT.decode(rpt);

    final List<String> tags = new ArrayList<>();

    final var isSystem = Optional.ofNullable(jwt.getClaim("azp")).map(Claim::asString)
        .orElse("").equals(bioProperties.getAuthSystemId());

    if (isSystem) {
      tags.add(ALL_TAGS);
    } else {
      tags.add(Optional.ofNullable(jwt.getClaim(TOKEN_ATTR_FHIR_ORG_ID)).map(Claim::asString)
          .orElseThrow(() -> new RptIntrospectionException("missing " + TOKEN_ATTR_FHIR_ORG_ID)));
    }

    return tags;
  }
  
  private boolean isResourceWithTags(IBaseResource resource) {
    return RESOURCES_WITH_TAGS.contains(resource.getClass().getSimpleName());
  }

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource resource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && isResourceWithTags(resource);
  }

  private void handleRequestAndResource(RequestDetails requestDetails, IBaseResource resource) {
    if(isValidRequestAndResource(requestDetails, resource)) {
      // clear all tags
      Optional.ofNullable(resource.getMeta().getTag()).ifPresent(List::clear);
      // add EP code as tag
      resource.getMeta().addTag().setCode(extractEpCode(resource));
      // add LDM code as tag (optional)
      final String ldmCode = extractLdmCode(resource);
      if (ldmCode != null) {
        resource.getMeta().addTag().setCode(ldmCode);
      }
    }
  }
  
  private String extractEpCode(IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractEpCode((ServiceRequest) resource);
    }
    throw new NotImplementedOperationException(String.format("Missing extract EP implementation for meta of %s", resource.getClass().getSimpleName()));
  }

  private String extractLdmCode(IBaseResource resource) {
    if(resource instanceof ServiceRequest) {
      return extractLdmCode((ServiceRequest) resource);
    }
    throw new NotImplementedOperationException(String.format("Missing extract LDM implementation for meta of %s", resource.getClass().getSimpleName()));
  }
  
  private String extractEpCode(ServiceRequest resource) {
      return Optional.ofNullable(resource.getIdentifier()).orElse(Collections.emptyList()).stream()
          .filter(i -> i.getType().getCoding().stream().anyMatch(c -> "MR".equals(c.getCode())))
          .findFirst().map(p -> p.getAssigner().getReference().split("/")[1])
          .orElseThrow(() -> new InvalidRequestException("Identifier of type MR with assigner organization is required"));
  }

  private String extractLdmCode(ServiceRequest resource) {
    return Optional.ofNullable(resource.getPerformer()).orElse(Collections.emptyList()).stream()
        .filter(p -> StringUtils.isNotBlank(p.getReference()))
        .map(p -> p.getReference().split("/")[1])
        .findFirst().orElseThrow(() -> new InvalidRequestException("Performer reference is required"));
        
  }
  
}
