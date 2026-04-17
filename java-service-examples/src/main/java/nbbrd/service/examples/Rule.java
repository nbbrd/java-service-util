package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.util.List;

@ServiceDefinition(quantifier = Quantifier.MULTIPLE, batchType = Rule.Batch.class)
public interface Rule {

    interface Batch {
        List<Rule> getRules();
    }

    // 💡 Generates the batch class
    @ServiceProvider
    enum GuidingPrinciples implements Rule {
        A, B, C;
    }

    static void main(String[] args) {
        // 💡 Get the batch of rules
        RuleLoader.load().forEach(System.out::println);
    }
}
