package etb.etbDL.output;

import java.util.Collection;
import java.util.Map;

import etb.etbDL.utils.*;
import etb.etbDL.statements.etbDLStatement;

/**
 * Default implementation of {@link QueryOutput} that uses {@code System.out}.
 */
public class DefaultQueryOutput implements QueryOutput {

    @Override
    public void writeResult(etbDLStatement statement, Collection<Map<String, String>> answers) {
		System.out.println(statement.toString());
		if (!answers.isEmpty()) {
			if (answers.iterator().next().isEmpty()) {
				System.out.println("  Yes.");
			} else {
				for (Map<String, String> answer : answers) {
					System.out.println("  " + OutputUtils.bindingsToString(answer));
				}
			}
		} else {
			System.out.println("  No.");
		}
	}

    @Override
    public void writeResult2(Collection<Map<String, String>> answers) {
        //System.out.println(goal.toString());
        if (!answers.isEmpty()) {
            if (answers.iterator().next().isEmpty()) {
                System.out.println("--> Yes.");
            } else {
                for (Map<String, String> answer : answers) {
                    System.out.println("--> answers: " + OutputUtils.bindingsToString(answer));
                }
            }
        } else {
            System.out.println("--> No.");
        }
    }
}
