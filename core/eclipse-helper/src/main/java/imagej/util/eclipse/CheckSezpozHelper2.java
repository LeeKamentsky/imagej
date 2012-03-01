package imagej.util.eclipse;

import java.util.Collection;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

public class CheckSezpozHelper2 implements AnnotationProcessorFactory {

	@Override
	public AnnotationProcessor getProcessorFor(
			Set<AnnotationTypeDeclaration> arg0,
			AnnotationProcessorEnvironment arg1) {
		CheckSezpozHelper.log("getProcessorFor");
		return null;
	}

	@Override
	public Collection<String> supportedAnnotationTypes() {
		CheckSezpozHelper.log("supportedAnnotationTypes");
		return null;
	}

	@Override
	public Collection<String> supportedOptions() {
		CheckSezpozHelper.log("supportedOptions");
		return null;
	}

}
