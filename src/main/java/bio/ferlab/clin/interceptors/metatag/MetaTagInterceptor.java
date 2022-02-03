package bio.ferlab.clin.interceptors.metatag;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
@Interceptor
public class MetaTagInterceptor {
  
  private final MetaTagResourceAccess metaTagResourceAccess;
  private final MetaTagResourceVisitor metaTagResourceVisitor;
  
  public MetaTagInterceptor(MetaTagResourceAccess metaTagResourceAccess, MetaTagResourceVisitor metaTagResourceVisitor) {
    this.metaTagResourceAccess = metaTagResourceAccess;
    this.metaTagResourceVisitor = metaTagResourceVisitor;
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
  public void created(RequestDetails requestDetails, IBaseResource resource) {
    this.handleRequestAndResource(requestDetails, resource);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    this.handleRequestAndResource(requestDetails, newResource);
  }

  // this hook (optional) will increase performance by adding _tag in the query before SQL calls
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public void addTagParameter(RequestDetails requestDetails) {
    if (RequestTypeEnum.GET.equals(requestDetails.getRequestType()) 
        && metaTagResourceAccess.isResourceWithTags(requestDetails.getResourceName())) {
      final String[] userTags = metaTagResourceAccess.getUserTags(requestDetails).toArray(String[]::new);
      requestDetails.addParameter("_tag", userTags);
    }
  }

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource resource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && metaTagResourceAccess.isResourceWithTags(resource);
  }

  private void handleRequestAndResource(RequestDetails requestDetails, IBaseResource resource) {
    if(isValidRequestAndResource(requestDetails, resource)) {
      // clear all tags, easy solution to manage tags update
      Optional.ofNullable(resource.getMeta().getTag()).ifPresent(List::clear);
      this.addTagCode(resource, metaTagResourceVisitor.extractEpCode(resource));
      this.addTagCode(resource, metaTagResourceVisitor.extractLdmCode(resource));
    }
  }
  
  private void addTagCode(IBaseResource resource, String code) {
    if (StringUtils.isNotBlank(code)) {
      resource.getMeta().addTag().setCode(code);
    }
  }
  
}
