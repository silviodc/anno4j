package myProject.main;
import java.util.HashSet;

import com.github.anno4j.Anno4j;
import com.github.anno4j.model.Annotation;
import com.github.anno4j.model.impl.targets.SpecificResource;

public class Main {

	public static void main(String[] args) throws Exception {

		Anno4j anno4j = new Anno4j();
		Annotation annotation = anno4j.createObject(Annotation.class);

		SpecificResource specificResource = anno4j.createObject(SpecificResource.class);

		annotation.addTarget(specificResource);

		Annotation result = anno4j.findByID(Annotation.class, annotation.getResourceAsString());
		System.out.println(((SpecificResource) result.getTargets().iterator().next()).getStyleClasses().size());

		specificResource.addStyleClass("red");

		result = anno4j.findByID(Annotation.class, annotation.getResourceAsString());
		System.out.println( ((SpecificResource) result.getTargets().iterator().next()).getStyleClasses().size());

		HashSet<String> styleClasses = new HashSet<>();
		styleClasses.add("green");
		styleClasses.add("blue");
		specificResource.setStyleClasses(styleClasses);

		result = anno4j.findByID(Annotation.class, annotation.getResourceAsString());
		System.out.println(((SpecificResource) result.getTargets().iterator().next()).getStyleClasses().size());
	}

}
