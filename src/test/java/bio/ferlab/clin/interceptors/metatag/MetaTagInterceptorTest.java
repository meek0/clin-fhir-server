package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.auth.RPTPermissionExtractor;
import bio.ferlab.clin.auth.data.Permission;
import bio.ferlab.clin.auth.data.UserPermissions;
import bio.ferlab.clin.interceptors.BioAuthInterceptor;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MetaTagInterceptorTest {

  final MetaTagResourceAccess metaTagResourceAccess = Mockito.mock(MetaTagResourceAccess.class);
  final MetaTagResourceVisitor metaTagResourceVisitor = Mockito.mock(MetaTagResourceVisitor.class);
  final MetaTagInterceptor metaTagInterceptor = new MetaTagInterceptor(metaTagResourceAccess, metaTagResourceVisitor);

  @Test
  void addTagParameter_GET_valid_resource() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);
    when(requestDetails.getResourceName()).thenReturn("ValidResource");
    when(metaTagResourceAccess.isResourceWithTags(any(String.class))).thenReturn(true);
    when(metaTagResourceAccess.getUserTags(any())).thenReturn(List.of("tag1", "tag2"));
    metaTagInterceptor.addTagParameter(requestDetails);
    verify(metaTagResourceAccess).isResourceWithTags(eq("ValidResource"));
    verify(metaTagResourceAccess).getUserTags(eq(requestDetails));
    verify(requestDetails).addParameter(eq("_tag"), eq(new String[]{"tag1", "tag2"}));
  }

  @Test
  void addTagParameter_not_valid_request() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.DELETE);
    metaTagInterceptor.addTagParameter(requestDetails);
    verify(requestDetails, never()).addParameter(any(), any());
  }

  @Test
  void addTagParameter_not_valid_resource() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);
    when(requestDetails.getResourceName()).thenReturn("NotValidResource");
    when(metaTagResourceAccess.isResourceWithTags(any(String.class))).thenReturn(false);
    metaTagInterceptor.addTagParameter(requestDetails);
    verify(metaTagResourceAccess).isResourceWithTags(eq("NotValidResource"));
    verify(requestDetails, never()).addParameter(any(), any());
  }
  
  @Test
  void addTagCode_POST_valid_resource() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final IBaseResource resource = Mockito.mock(IBaseResource.class);
    final IBaseMetaType meta = Mockito.mock(IBaseMetaType.class);
    final IBaseCoding tag = Mockito.mock(IBaseCoding.class);
    when(resource.getMeta()).thenReturn(meta);
    when(meta.addTag()).thenReturn(tag);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
    when(metaTagResourceAccess.isResourceWithTags(any(IBaseResource.class))).thenReturn(true);
    when(metaTagResourceVisitor.extractEpCode(any())).thenReturn("ep1");
    when(metaTagResourceVisitor.extractLdmCode(any())).thenReturn("ldm1");
    metaTagInterceptor.created(requestDetails, resource);
    verify(metaTagResourceVisitor).extractEpCode(eq(resource));
    verify(metaTagResourceVisitor).extractLdmCode(eq(resource));
    verify(tag).setCode(eq("ep1"));
    verify(tag).setCode(eq("ldm1"));
  }

  @Test
  void addTagCode_PUT_valid_resource() {
    final RequestDetails requestDetails = Mockito.mock(RequestDetails.class);
    final IBaseResource resource = Mockito.mock(IBaseResource.class);
    final IBaseMetaType meta = Mockito.mock(IBaseMetaType.class);
    final IBaseCoding tag = Mockito.mock(IBaseCoding.class);
    when(resource.getMeta()).thenReturn(meta);
    when(meta.addTag()).thenReturn(tag);
    when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.PUT);
    when(metaTagResourceAccess.isResourceWithTags(any(IBaseResource.class))).thenReturn(true);
    when(metaTagResourceVisitor.extractEpCode(any())).thenReturn("ep1");
    when(metaTagResourceVisitor.extractLdmCode(any())).thenReturn("ldm1");
    metaTagInterceptor.updated(requestDetails, null, resource);
    verify(metaTagResourceVisitor).extractEpCode(eq(resource));
    verify(metaTagResourceVisitor).extractLdmCode(eq(resource));
    verify(tag).setCode(eq("ep1"));
    verify(tag).setCode(eq("ldm1"));
  }

}