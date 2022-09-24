package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Interceptor
public class SameRequestInterceptor {

  private final Map<RequestDetails, List<IBaseResource>> requests = new ConcurrentHashMap<>();

  @Hook(Pointcut.STORAGE_PRESHOW_RESOURCES)
  public void concatResources(IPreResourceShowDetails preResourceShowDetails, RequestDetails requestDetails) {
    List<IBaseResource> resources = new ArrayList<>();
    preResourceShowDetails.forEach(resources::add);
    requests.computeIfAbsent(requestDetails, rd -> new ArrayList<>());
    requests.get(requestDetails).addAll(resources);
  }
  
  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
  public void clear(RequestDetails requestDetails) {
    remove(requestDetails);
  }
  
  public List<IBaseResource> get(RequestDetails requestDetails) {
    return requests.get(requestDetails);
  }
  
  public void remove(RequestDetails requestDetails) {
    requests.remove(requestDetails);
  }
}
