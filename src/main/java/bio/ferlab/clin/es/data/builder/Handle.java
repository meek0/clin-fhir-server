package bio.ferlab.clin.es.data.builder;

import java.util.function.Consumer;

class Handle<T> {
    final Class<T> tClass;
    final Consumer<T> callback;

    public Handle(Class<T> tClass, Consumer<T> callback) {
        this.tClass = tClass;
        this.callback = callback;
    }
}
