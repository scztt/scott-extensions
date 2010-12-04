+Environment {
	useArgs { arg function ...args;
		// temporarily replaces the currentEnvironment with this,
		// executes function, returns the result of the function
		var result, saveEnvir;

		saveEnvir = currentEnvironment;
		currentEnvironment = this;
		protect {
			result = function.value(*args);
		}{
			currentEnvironment = saveEnvir;
		};
		^result
	}
}