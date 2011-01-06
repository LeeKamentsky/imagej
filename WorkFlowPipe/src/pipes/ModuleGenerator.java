package pipes;

import java.util.HashMap;

import modules.FetchPage;
import modules.Output;
import pipesapi.Module;
import pipesentity.Type;

public class ModuleGenerator {
		

	/**
	 * Returns a list of the internal modules
	 * 
	 * @return
	 */
	public static HashMap<Service, Module> getInternalModules() {

		// Create a module hashmap
		HashMap< Service, Module > moduleServiceHashMap = new HashMap<Service,Module>();

		//add the fetch page module
		moduleServiceHashMap.put( new Service( new Type("fetchpage") ), FetchPage.getFetchPage() );

		//add the output module
		moduleServiceHashMap.put( new Service( new Type("output") ), Output.getOutput() );

		return moduleServiceHashMap;
	}
	
	//TODO: replace with lookup api
	public static Module getModule( String type )
	{
		if ( type.equalsIgnoreCase("fetchpage") )
			return FetchPage.getFetchPage();
		
		return Output.getOutput();
	}

}
