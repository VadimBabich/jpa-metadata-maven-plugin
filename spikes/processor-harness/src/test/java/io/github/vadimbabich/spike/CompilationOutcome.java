package io.github.vadimbabich.spike;

import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;

/**
 * What one compilation round produced, in a form no particular harness owns.
 *
 * <p>Generated sources are carried as bytes keyed by path relative to the source root, so the same
 * parity assertion works against a {@code -s} directory, a {@code JavaFileObject} or anything else
 * a harness hands back.
 */
record CompilationOutcome(
    boolean succeeded,
    List<CompilationOutcome.Message> diagnostics,
    Map<String, byte[]> generatedSources) {

  record Message(Diagnostic.Kind kind, String text) {
  }

  List<String> errors() {
    return textOf(Diagnostic.Kind.ERROR);
  }

  List<String> warnings() {
    return textOf(Diagnostic.Kind.WARNING);
  }

  private List<String> textOf(Diagnostic.Kind kind) {
    return diagnostics.stream()
        .filter(message -> message.kind() == kind)
        .map(CompilationOutcome.Message::text)
        .toList();
  }
}
