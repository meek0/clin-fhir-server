package bio.ferlab.clin.context;

import lombok.Data;

import java.util.Locale;

@Data
public class ServiceContext {
    private String userId;
    private Locale locale;

    public static ServiceContext get() {
        ServiceContext sc = ThreadLocalServiceContext.getInstance().get();

        if(sc == null){
            sc = new ServiceContext();
            ThreadLocalServiceContext.getInstance().set(sc);
        }

        return sc;
    }
}
