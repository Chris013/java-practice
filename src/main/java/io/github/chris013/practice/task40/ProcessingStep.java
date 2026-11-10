package io.github.chris013.practice.task40;

/** Legacy. One link of a hand-made chain of responsibility: it does its work, then hands the result on. */
abstract class ProcessingStep {

    private ProcessingStep successor;

    /** Returns the successor, so that links can be chained: {@code a.setSuccessor(b).setSuccessor(c)}. */
    ProcessingStep setSuccessor(ProcessingStep successor) {
        this.successor = successor;
        return successor;
    }

    final String process(String input) {
        String output = doProcess(input);
        return successor == null ? output : successor.process(output);
    }

    abstract String doProcess(String input);
}
