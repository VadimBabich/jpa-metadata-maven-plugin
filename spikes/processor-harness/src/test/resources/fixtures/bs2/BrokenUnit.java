package bs2;

// BS-2: a semantic error in a compilation unit that carries no @Table annotation. The probe asks
// whether the processor still runs over the valid entity compiled alongside this file.
public class BrokenUnit {

  int broken() {
    return "not an int";
  }
}
