package bio.ferlab.clin.utils;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@ConditionalOnProperty(value = "bio.monitors.enabled", havingValue = "true")
@EnableScheduling
public class Monitors {

  @Around("within(bio.ferlab.clin.es.builder.nanuq..*)" +
    //"|| within(ca.uhn.fhir.jpa.app..*)" +
    "|| within(bio.ferlab.clin.validation..*)" +
    "|| within(bio.ferlab.clin.utils..*)" +
    "|| within(bio.ferlab.clin.auth..*)" +
    "|| within(bio.ferlab.clin.interceptors..*)" +
    "|| within(bio.ferlab.clin.es.ElasticsearchRestClient)" +
    "|| within(bio.ferlab.clin.es.TemplateIndexer)" +
    "|| within(bio.ferlab.clin.es.IndexerHelper)" +
    "|| within(bio.ferlab.clin.es.MigrationManager)" +
    "|| within(bio.ferlab.clin.es.builder.nanuq..*)" +
    "|| within(bio.ferlab.clin.es.extractor..*)" +
    "|| within(bio.ferlab.clin.es.indexer..*)")
  public Object monitors(ProceedingJoinPoint joinPoint) throws Throwable {
    String targetClass = joinPoint.getTarget().getClass().getSimpleName();
    String targetMethod = joinPoint.getSignature().getName();
    var monitor = MonitorFactory.start(String.format("%s.%s()", targetClass, targetMethod));
    try {
      return joinPoint.proceed();
    } finally {
      monitor.stop();
    }
  }

  @Scheduled(fixedDelayString = "${bio.monitors.display-rate}")
  public void scheduleFixedRateTask() {
    System.out.println(monitors());
  }

  public String monitors() {
    StringBuilder builder = new StringBuilder();
    builder.append("Monitors:\n");
    builder.append("~~~~~\n");
    var monitors = Arrays.stream(MonitorFactory.getRootMonitor().getMonitors())
      .filter((m) -> m.getHits() > 0)
      .sorted((m1, m2) -> Double.compare(m2.getTotal(), m1.getTotal()))
      .collect(Collectors.toList());
    int lm = monitors.stream().map(Monitor::getLabel).mapToInt(String::length).max().orElse(10);
    for (Monitor monitor : monitors) {
      builder.append(String.format("%-" + lm + "s -> %8.0f hits; %8.1f avg; %8.1f min; %8.1f max;\n", monitor.getLabel(),
        monitor.getHits(), monitor.getAvg(), monitor.getMin(), monitor.getMax()));
    }
    builder.append("\n");
    return builder.toString();
  }

}
