package bio.ferlab.clin.es.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Actions {
  private final List<ActionInf> actions = new ArrayList<>();

  public interface ActionInf {

  }

  public static class ActionAdd implements ActionInf {
    public final Action add;
    public ActionAdd(String index, String alias) {
      add = new Action(index, alias);
    }
  }

  public static class ActionRemove implements ActionInf {
    public final Action remove;
    public ActionRemove(String index, String alias) {
      remove = new Action(index, alias);
    }
  }

  @AllArgsConstructor
  public static class Action {
    public String index;
    public String alias;
  }
}

