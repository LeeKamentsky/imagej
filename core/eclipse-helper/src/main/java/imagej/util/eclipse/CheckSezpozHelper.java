package imagej.util.eclipse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("*")
public class CheckSezpozHelper extends AbstractProcessor {

	@Override
	public void init(ProcessingEnvironment environment) {
		log("Called helper ");
		Map<String, String> options = environment.getOptions();
		for (final String key : options.keySet()) {
			log("\t" + key + " = " + options.get(key));
		}
		super.init(environment);
	}

	@Override
	public boolean process(Set<? extends TypeElement> arg0,
			RoundEnvironment arg1) {
		log("process");
		return false;
	}

	public static void log(String message) {
		System.err.println(message);
		try {
			File file = new File("/tmp/a1.txt");
			FileWriter fileWriter = new FileWriter(file, true);
			PrintWriter writer = new PrintWriter(fileWriter);
			writer.println(message);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
